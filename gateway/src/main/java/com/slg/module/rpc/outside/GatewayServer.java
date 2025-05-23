package com.slg.module.rpc.outside;

import com.slg.module.rpc.outside.outsideMsg.IdleStateEventHandler;
import com.slg.module.rpc.outside.outsideMsg.MsgDecode;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Component
public class GatewayServer implements CommandLineRunner {
    //todo
    @Value("${netty.server.port:8888}")
    private int port;
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup(30);
    @Autowired
    private PbMessageHandler pbMessageHandler;
    LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
    private final IdleStateHandler idleStateHandler = new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS);
    private final IdleStateEventHandler idleStateEventHandler = new IdleStateEventHandler();


    public void start(int port) throws Exception {

        try {
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
//                            p.addLast("log",loggingHandler);
                            p.addLast(new MsgDecode());
                            p.addLast(pbMessageHandler);
//                            System.out.println("客户端连接成功");
                        }
                    });

            ChannelFuture f = b.bind().sync();
            //zk注册
//            toRegisterZK(port);
            System.out.println("=====服务器启动成功gatewayServer=====");
            f.channel().closeFuture().sync();
        } finally {
            System.out.println("----------------------------服务器关闭--------------------------------------------");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();

        }
    }

    @Async
    @Override
    public void run(String... args) throws Exception {
        start(port);
    }
}