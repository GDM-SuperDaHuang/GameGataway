package com.slg.module.rpc.outside;

import com.slg.module.rpc.interMsg.ForwardClient;
import com.slg.module.rpc.outside.outsideMsg.MsgDecode;
import com.slg.module.util.ConfigReader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

public class GatewayServer {
    private final EventLoopGroup bossGroup;
    private final static EventLoopGroup workerGroup = new NioEventLoopGroup(6);
    private final PbMessageHandler pbMessageHandler;
    private int port;
    private ChannelFuture serverChannelFuture;

    LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
//    private final IdleStateHandler idleStateHandler = new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS);
//    private final IdleStateEventHandler idleStateEventHandler = new IdleStateEventHandler();


    //初始化,获取配置值
    public GatewayServer() {
        try {
            ConfigReader config = new ConfigReader("application.properties");
            port = config.getIntProperty("netty.server.port");
            int protoIdMin = config.getIntProperty("server.proto-id-min");
            int protoIdMax = config.getIntProperty("server.proto-id-max");
            pbMessageHandler = new PbMessageHandler(protoIdMax);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //读取配置
        if (Epoll.isAvailable() && System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            bossGroup = new EpollEventLoopGroup(1);
        } else {
            bossGroup = new NioEventLoopGroup(1);
        }
    }


    public ChannelFuture start(int port) {

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                // 指定Channel
                .channel(NioServerSocketChannel.class)
//                    .handler(new LoggingHandler())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                //非延迟，直接发送
                .childOption(ChannelOption.TCP_NODELAY, true)
                //使用指定的端口设置套接字地址
                .localAddress(new InetSocketAddress(port))
                //服务端可连接队列数,对应TCP/IP协议listen函数中backlog参数,缓存连接
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        //日志
                        p.addLast("log", loggingHandler);
//                        p.addLast(QPSHandler.INSTANCE);

                        p.addLast(new MsgDecode());
                        p.addLast(pbMessageHandler);
//                            System.out.println("客户端连接成功");
                    }
                });

        this.serverChannelFuture = b.bind();
        return serverChannelFuture.addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("===== 网关服务器启动成功，端口: " + port + " =====");
            } else {
                System.err.println("!!!!! 网关服务器启动失败 !!!!!");
                future.cause().printStackTrace();
                System.exit(1); // 启动失败直接退出
            }
        });
//            //zk注册
////            toRegisterZK(port);
//            System.out.println("=====服务器启动成功gatewayServer=====");
//            serverChannelFuture.channel().closeFuture().sync();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            System.out.println("----------------------------服务器关闭--------------------------------------------");
//            workerGroup.shutdownGracefully();
//            bossGroup.shutdownGracefully();
//        }
    }

    /**
     * 优雅关闭服务器
     */
    public void shutdown() {
        if (serverChannelFuture != null) {
            serverChannelFuture.channel().close().syncUninterruptibly();
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        ForwardClient.getInstance().shutdown();
    }

    public int getPort() {
        return port;
    }
}