package com.slg.module.rpc.interMsg;

import com.slg.module.config.GatewayRoutingManager;
import com.slg.module.connection.ClientChannelManage;
import com.slg.module.connection.ServerChannelManage;
import com.slg.module.connection.ServerConfig;
import com.slg.module.message.ByteBufferServerMessage;
import com.slg.module.message.MsgUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;

/**
 * 目标服务器--网关
 */
@ChannelHandler.Sharable
public class TargetServerHandler extends SimpleChannelInboundHandler<ByteBufferServerMessage> {

    ClientChannelManage clientChannelManage = ClientChannelManage.getInstance();

    /**
     * 接收目标服务器数据
     *
     * @param ctx 目标服务器-网关
     * @param msg 消息
     * @throws Exception .
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBufferServerMessage msg) throws Exception {
        long userId = msg.getUserId();
        ByteBuf body = msg.getBody();
        //转发回给客户端
        Channel clientChannel = clientChannelManage.getChannelByUserId(userId);
        if (clientChannel != null) {
            ByteBuf out = MsgUtil.buildClientMsg(ctx, msg.getCid(), msg.getErrorCode(), msg.getProtocolId(), msg.getZip(), msg.getEncrypted(), msg.getLength(), body);
            clientChannel.writeAndFlush(out).addListener(future -> {

                if (!future.isSuccess()) {//客户端连接丢失
                    System.err.println("Write and flush failed: " + future.cause());
                }
            });
        } else {
            //todo 没有连接，警告处理，记录失败
            msg.recycle();
        }
    }

    /**
     * 网关-服务器错误处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof InvocationTargetException) {
            //目标方法错误
        } else if (cause instanceof SocketException
                || cause instanceof DecoderException) {
            destroyConnection(ctx);
            //客户端关闭连接/连接错误
            // 关闭连接
            ctx.close();
        }
    }

    /**
     * 关闭内部服务器连接
     */
    private void destroyConnection(ChannelHandlerContext ctx) {
        //断开无效连接
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        int port = ((InetSocketAddress) socketAddress).getPort();
        String ip = ((InetSocketAddress) socketAddress).getHostString();
        //todo
        Map<Byte, ServerConfig> serverMap = GatewayRoutingManager.getInstance().getServerMap();
        for (Map.Entry<Byte, ServerConfig> entry : serverMap.entrySet()) {
            ServerConfig config = entry.getValue();
            if (config.getHost().equals(ip) && config.getPort() == port) {
                int serverId = config.getServerId();
                ServerChannelManage.getInstance().removeServerChanel(serverId);
            }
        }
        System.out.println("内部服务器关闭连接：" + socketAddress);
    }
}
