package com.slg.module.rpc.interMsg;

import com.slg.module.connection.ServerChannelManage;
import com.slg.module.connection.ServerConfig;
import com.slg.module.util.ConfigReader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;


public class  ForwardClient {
    private final EventLoopGroup forwardingGroup;
    private final Bootstrap bootstrap = new Bootstrap();
    private final TargetServerHandler targetServerHandler = new TargetServerHandler();
    private final int connectionMin;
    private final int connectionMax;


    // 静态内部类持有单例
    private static class Holder {
        static final ForwardClient INSTANCE = new ForwardClient();
    }

    // 全局访问点
    public static ForwardClient getInstance() {
        return ForwardClient.Holder.INSTANCE;
    }


    private ForwardClient() {
        try {
            ConfigReader config = new ConfigReader("application.properties");
            connectionMin = config.getIntProperty("netty.client.connectionMin");
            connectionMax = config.getIntProperty("netty.client.connectionMax");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //读取配置
        if (Epoll.isAvailable() && System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            forwardingGroup = new EpollEventLoopGroup(1);
        } else {
            forwardingGroup = new NioEventLoopGroup(1);
        }
        bootstrap.group(forwardingGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP, true)  // 每个Channel绑定固定线程
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
     * 建立连接
     */
    public Channel connection(ServerConfig serverConfig) {
        ServerChannelManage instance = ServerChannelManage.getInstance();
        int connectionId = instance.nextChannelId(serverConfig.getServerId());
//        //同步阻塞
//        try {
//            ChannelFuture sync = bootstrap.connect(serverConfig.getHost(), serverConfig.getPort()).sync();
//            if (sync.isSuccess()) {
//                System.out.println("服务器标识符:+" + serverConfig.getServerId() + "+  成功建立连接 [" + "生成serverId:" + connectionId + "] -> " + serverConfig.getHost() + ":" + serverConfig.getPort());
//                return sync.channel();
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        ChannelFuture channelFuture = bootstrap.connect(serverConfig.getHost(), serverConfig.getPort()).addListener(f -> {
            if (f.isSuccess()) {
                Channel channel = ((ChannelFuture) f).channel();
                instance.addChannel(serverConfig.getServerId(), connectionId, channel);
                System.out.println("服务器标识符:+" + serverConfig.getServerId() + "+  成功建立连接 [" + "生成serverId:" + connectionId + "] -> " + serverConfig.getHost() + ":" + serverConfig.getPort());
            } else {
                System.err.println("连接目标服务器失败: " + serverConfig.getHost() + ":" + serverConfig.getPort() + ", 原因: " + f.cause());
            }
        });
        if (channelFuture.isSuccess()) {
            return channelFuture.channel();
        } else {
            return null;
        }
    }

    //断开连接
    public void disconnect(int serverId) {
        ArrayList<Channel> channels = ServerChannelManage.getInstance().removeServerChanel(serverId);
        for (Channel channel : channels) {
            channel.close().addListener(future -> {
                if (future.isSuccess()) {
                    System.out.println("连接已断开: " + channel);
                } else {
                    System.err.println("断开连接失败: " + channel + ", 原因: " + future.cause());
                }
            });
        }
    }

    public void shutdown() {
        System.out.println("----------------------------关闭所有连接--------------------------------------------");
        forwardingGroup.shutdownGracefully().syncUninterruptibly(); // 阻塞直到关闭完成
    }
}