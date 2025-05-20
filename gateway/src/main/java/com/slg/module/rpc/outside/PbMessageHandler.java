package com.slg.module.rpc.outside;

import com.google.protobuf.GeneratedMessage;
import com.slg.module.config.GatewayRoutingProperties;
import com.slg.module.connection.ClientChannelManage;
import com.slg.module.connection.ServerChannelManage;
import com.slg.module.connection.ServerConfig;
import com.slg.module.message.*;
import com.slg.module.register.HandleBeanDefinitionRegistryPostProcessor;
import com.slg.module.rpc.interMsg.IdleStateEventHandler;
import com.slg.module.rpc.interMsg.MsgServerInternalDecode;
import com.slg.module.rpc.interMsg.TargetServerHandler;
import com.slg.module.util.BeanTool;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;

import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

@Component
@ChannelHandler.Sharable
public class PbMessageHandler extends SimpleChannelInboundHandler<ByteBufferMessage> {
    @Autowired
    private HandleBeanDefinitionRegistryPostProcessor postProcessor;

    //目标服务器--网关管理
    @Autowired
    private ServerChannelManage serverChannelManage;
    //客户端--网关管理
    @Autowired
    private ClientChannelManage clientchannelManage;

    @Autowired
    private TargetServerHandler targetServerHandler;

    private final EventLoopGroup forwardingGroup = new NioEventLoopGroup(4);
    private final Bootstrap bootstrap;
    private final IdleStateHandler idleStateHandler = new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS);
    private final IdleStateEventHandler idleStateEventHandler = new IdleStateEventHandler();

    @Autowired
    private GatewayRoutingProperties routingProperties;  // 注入配置
    @Value("${server.proto-id-max}")
    private int gateProtoIdMax;

    @Autowired
    private SendMsg sendMsg;

    /**
     * 网关转发
     */
    public PbMessageHandler() {
        bootstrap = new Bootstrap();
        bootstrap.group(forwardingGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new MsgServerInternalDecode());
                        p.addLast(targetServerHandler);
                    }
                });
    }


    /**
     * @param ctx 客户端-网关连接
     * @param msg 信息
     * @throws Exception 异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBufferMessage msg) throws Exception {
        int protocolId = msg.getProtocolId();
        byte[] body = msg.getBody();
        Channel channel = ctx.channel();
        Method parse = postProcessor.getParseFromMethod(protocolId);
        if (parse == null) {
            ByteBuf outClient = sendMsg.buildClientMsg(msg.getCid(), ErrorCodeConstants.SERIALIZATION_METHOD_LACK, protocolId, Constants.NoZip, Constants.NoEncrypted, null);
            ctx.writeAndFlush(outClient);
            return;
        }
        long userId = clientchannelManage.getUserId(ctx.channel());
        if (protocolId < gateProtoIdMax) {//本地
            Object msgObject = parse.invoke(null, body);
            MsgResponse message = route(ctx, msgObject, protocolId, userId);
            //写回
            GeneratedMessage.Builder<?> responseBody = message.getBody();
            byte[] bodyByteArr = responseBody.buildPartial().toByteArray();
            //加密判断 todo


            //压缩判断 todo


            //todo
            ByteBuf out = sendMsg.buildClientMsg(msg.getCid(), message.getErrorCode(), protocolId, Constants.NoZip, Constants.NoEncrypted, bodyByteArr);
            ChannelFuture channelFuture = ctx.writeAndFlush(out);
            channelFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    System.err.println("Write and flush failed: " + future.cause());
                }
            });
        } else {//转发
            ServerConfig serverConfig = routingProperties.getServerByProtoId(protocolId);
            if (serverConfig == null) {
                // 发送失败,直接返回，告诉客户端
                failedNotificationClient(ctx,msg.getCid(), ErrorCodeConstants.ESTABLISH_CONNECTION_FAILED, msg.getProtocolId());
                return;
            }
            // 转发到目标服务器
            forwardToTargetServer(ctx, msg, userId, serverConfig);
        }
    }

    /**
     * 转发到目标服务器
     *
     * @param clientChannel 客户端-网关
     * @param msg           消息
     */
    private void forwardToTargetServer(ChannelHandlerContext clientChannel, ByteBufferMessage msg, long userId, ServerConfig serverConfig) {
        Channel channel = serverChannelManage.getChanelByIp(serverConfig.getServerId());
        if (channel == null) {
            try {
                ChannelFuture future = bootstrap.connect(serverConfig.getHost(), serverConfig.getPort()).sync();
                if (future.isSuccess()) {
                    channel = future.channel();
                    serverChannelManage.put(serverConfig.getServerId(), channel, serverConfig);
                }
            } catch (Exception e) {
                // 发送失败,直接返回，告诉客户端
                failedNotificationClient(clientChannel,msg.getCid(), ErrorCodeConstants.ESTABLISH_CONNECTION_FAILED, msg.getProtocolId());
                return;
            }
        }

        //进行转发到目标服务器
        if (channel != null && channel.isActive()) {
            ByteBuf out = sendMsg.buildServerMsg(userId, msg.getCid(), msg.getErrorCode(), msg.getProtocolId(), 0, 1, msg.getBody());
            ChannelFuture channelFuture = channel.writeAndFlush(out);
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // 消息转发成功的处理
                    //System.out.println("Successfully forwarded message");
                } else {
                    // 消息转发失败的处理
                    // log.error("Failed to forward message to {}", targetServerAddress, future.cause());
                    serverChannelManage.removeChanelByIp(serverConfig.getServerId());
                    //直接告诉客户端，返回错误码
                    failedNotificationClient(clientChannel,msg.getCid(), ErrorCodeConstants.GATE_FORWARDING_FAILED, msg.getProtocolId());
                }
            });
        } else {
            failedNotificationClient(clientChannel,msg.getCid(), ErrorCodeConstants.GATE_FORWARDING_FAILED, msg.getProtocolId());
        }
    }


    //客户端端处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof InvocationTargetException) {
            //目标方法错误
        } else if (cause instanceof SocketException
                || cause instanceof DecoderException) {
            //客户端关闭连接/连接错误
            // 关闭连接
            //断开无效连接
            destroyConnection(ctx);
            ctx.close();
        }
    }


    /**
     * 路由分发
     */
    public MsgResponse route(ChannelHandlerContext ctx, Object message, int protocolId, long userId) throws Exception {
        Class<?> handleClazz = postProcessor.getHandleMap(protocolId);
        if (handleClazz == null) {
            return null;
        }
        Method method = postProcessor.getMethodMap(protocolId);
        if (method == null) {
            return null;
        }
        method.setAccessible(true);
        Object bean = BeanTool.getBean(handleClazz);
        if (bean == null) {
            return null;
        }
        Object invoke = method.invoke(bean, ctx, message, userId);
        if (invoke instanceof MsgResponse) {
            return (MsgResponse) invoke;
        } else {
            return null;
        }
    }


    public void destroyConnection(ChannelHandlerContext ctx) {
        //断开无效连接
        clientchannelManage.remove(ctx.channel());
    }

    // 失败通知客户端
    public void failedNotificationClient(ChannelHandlerContext ctx,int cid, int errorCode, int protocolId) {
        //日志记录失败日志 todo

        // 发送失败,直接返回，告诉客户端
        ByteBuf out = sendMsg.buildClientMsg(cid, errorCode, protocolId, Constants.NoZip, Constants.NoEncrypted, null);
        ChannelFuture channelFuture = ctx.writeAndFlush(out);
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {//通知客户端失败 日志 todo
                System.err.println("Write and flush failed: " + future.cause());
            }
        });
    }




}
