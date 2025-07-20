package com.slg.module.rpc.outside;

import com.slg.module.connection.ServerConfigManager;
import com.slg.module.message.Constants;
import com.slg.module.rpc.interMsg.ForwardClient;
import com.slg.module.rpc.outside.outsideMsg.MsgDecode;
import com.slg.module.util.ConfigReader;
import com.slg.module.util.NacosClientUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class GatewayServer {
    private final EventLoopGroup bossGroup;
    private final static EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final PbMessageHandler pbMessageHandler;
    private int port;
    private ChannelFuture serverChannelFuture;

    //    LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
    ConfigReader config = new ConfigReader("application.properties");
    NacosClientUtil client = NacosClientUtil.getInstance(
            config.getProperty("nacos.serverAddr"),  // Nacos服务器地址
            config.getProperty("nacos.namespace")  // 命名空间ID（如为空字符串则使用默认命名空间）
    );

    //初始化,获取配置值
    public GatewayServer() {
        try {
            port = config.getIntProperty("netty.server.port");
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
//                        p.addLast("log", loggingHandler);
                        p.addLast(QPSHandler.INSTANCE);

                        p.addLast(new MsgDecode());
                        p.addLast(pbMessageHandler);
//                            System.out.println("客户端连接成功");
                    }
                });

        this.serverChannelFuture = b.bind();
        return serverChannelFuture.addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("===== 网关服务器启动成功，端口: " + port + " =====");
                // 注册服务实例
                Map<String, String> metadata = new HashMap<>();
                String group = config.getProperty("nacos.service.group");
                if (group == null) {
                    group = "DEFAULT_GROUP";
                }
                String host = config.getProperty("netty.server.host");
                String serverName = config.getProperty("nacos.service.name");
                String pbMin = config.getProperty("server.proto-id-min");
                String pbMax = config.getProperty("server.proto-id-max");
                String serverId = config.getProperty("netty.server.serverId");//实例id
                String groupId = config.getProperty("netty.server.group-id");//组
                metadata.put(Constants.ProtoMinId, pbMin);
                metadata.put(Constants.ProtoMaxId, pbMax);
                metadata.put(Constants.GroupId, groupId);
                client.registerInstance(serverId, serverName, group, host, port, 1.0, metadata);

                ServerConfigManager.getInstance(serverName, group, null, serverId);
            } else {
                System.err.println("!!!!! 网关服务器启动失败 !!!!!");
                future.cause().printStackTrace();
                System.exit(1); // 启动失败直接退出
            }
        });
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