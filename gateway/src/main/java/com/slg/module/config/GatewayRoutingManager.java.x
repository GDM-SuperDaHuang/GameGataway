package com.slg.module.config;

import com.slg.module.connection.ClientChannelManage;
import com.slg.module.connection.ServerConfig;
import com.slg.module.connection.ServerConfigManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class GatewayRoutingManager {
    private static final String CONFIG_FILE = "gateway-routing.properties";
    private static final String SERVER_PREFIX = "gateway.routing.servers[";
    private static final String SERVER_SUFFIX = "]";
    private static List<ServerConfig> servers;
    private static GatewayRoutingManager instance;

    ClientChannelManage channelManage = ClientChannelManage.getInstance();
    // 辅助数据
    private static byte[] ProtoGroupArr;//protoId-groupId

    static int groupNumMap = 0;//group的种类

    public static Map<Byte, ServerConfig> ServerMap = new HashMap<>();//ServerId-Server


    // 私有构造方法
    private GatewayRoutingManager() {
    }

    // 双重检查锁定获取实例
    public static GatewayRoutingManager getInstance() {
        if (instance == null) {
            synchronized (ClientChannelManage.class) {
                if (instance == null) {
                    instance = loadFromConfig();
                }
            }
        }
        return instance;
    }




    private static GatewayRoutingManager loadFromConfig() {
        Properties props = new Properties();
        try (InputStream input = GatewayRoutingManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new RuntimeException("Unable to find " + CONFIG_FILE);
            }
            props.load(input);
            servers = parseProperties(props);
            init();
            return new GatewayRoutingManager();
        } catch (IOException ex) {
            throw new RuntimeException("Error loading configuration", ex);
        }
    }

    private static List<ServerConfig> parseProperties(Properties props) {
        List<ServerConfig> serverList = new ArrayList<>();
        int index = 0;

        while (true) {
            String prefix = SERVER_PREFIX + index + SERVER_SUFFIX + ".";
            // 检查是否还有更多服务器配置
            if (props.getProperty(prefix + "server-id") == null) {
                break;
            }
            ServerConfig config = new ServerConfig();
            config.setServerId(Byte.parseByte(props.getProperty(prefix + "server-id")));
            config.setGroupId(Byte.parseByte(props.getProperty(prefix + "group-id")));
            config.setProtoIdMin(Integer.parseInt(props.getProperty(prefix + "proto-id-min")));
            config.setProtoIdMax(Integer.parseInt(props.getProperty(prefix + "proto-id-max")));
            config.setHost(props.getProperty(prefix + "host"));
            config.setPort(Integer.parseInt(props.getProperty(prefix + "port")));
            serverList.add(config);
            index++;
        }
        return serverList;
    }

    public Map<Byte, ServerConfig> getServerMap() {
        return ServerMap;
    }

    //统计
    // Key: 服务器IP或标识, Value: 连接数计数器
    private static final Map<Byte, LongAdder> SERVER_CONNECTION_COUNTS = new ConcurrentHashMap<>();//serverId - 数量

    /**
     * 客户端连接时调用（线程安全）
     *
     * @param serverId 服务器唯一标识（如IP）
     */
    public static void increment(Byte serverId) {
        SERVER_CONNECTION_COUNTS.computeIfAbsent(serverId, k -> new LongAdder()).increment();
    }


    public static void init() {
        int max = 0;
        ArrayList<Byte> groupList = new ArrayList<>();
        for (ServerConfig server : servers) {
            int protoIdMax = server.getProtoIdMax();
            if (protoIdMax > max) {
                max = protoIdMax;
            }
            byte groupId = server.getGroupId();
            if (!groupList.contains(groupId)) {
                groupNumMap++;
                groupList.add(groupId);
            }
            ServerMap.put(server.getServerId(), server);
        }
        ProtoGroupArr = new byte[max + 1];
        for (ServerConfig server : servers) {
            byte groupId = server.getGroupId();
            int protoIdMin = server.getProtoIdMin();
            int protoIdMax = server.getProtoIdMax();
            for (int protoId = protoIdMin; protoId <= protoIdMax; protoId++) {
                ProtoGroupArr[protoId] = groupId;
            }
        }
    }

    /**
     * 客户端断开时调用（线程安全）
     *
     * @param serverId 服务器唯一标识
     */
    public static void decrement(Byte serverId) {
        LongAdder counter = SERVER_CONNECTION_COUNTS.get(serverId);
        if (counter != null) {
            counter.decrement();
            // 可选：连接数为0时移除记录（避免内存泄漏）
            if (counter.sum() == 0) {
                SERVER_CONNECTION_COUNTS.remove(serverId, counter);
            }
        }
    }

    /**
     * 获取当前连接数
     *
     * @param serverId 服务器唯一标识
     * @return 连接数（若服务器不存在则返回0）
     */
    public static long getCount(String serverId) {
        LongAdder counter = SERVER_CONNECTION_COUNTS.get(serverId);
        return counter == null ? 0 : counter.sum();
    }

    /**
     * 打印所有服务器的连接数（调试用）
     */
    public static void printAllStats() {
        SERVER_CONNECTION_COUNTS.forEach((serverId, counter) ->
                System.out.printf("Server: %s, Connections: %d\n", serverId, counter.sum())
        );
    }

    public void setServers(List<ServerConfig> servers) {
        this.servers = servers;
    }


    /**
     * @param protocolId
     * @param flag       1连接旧服务器
     * @return
     */
    public ServerConfig getServer(int protocolId, Long userId, int flag) {
        byte groupId = ProtoGroupArr[protocolId];
        Map<Byte, Byte> gsMap = channelManage.getUserGroupServerMap().computeIfAbsent(userId, k -> new HashMap<>(groupNumMap));
        Byte serverId = gsMap.get(groupId);
        if (serverId == null) {//初始化
            //选一个 groupId 内的第一个server,可选均衡
            for (ServerConfig server : servers) {
                if (server.getGroupId() == groupId) {
                    gsMap.put(groupId, server.getServerId());
                    return server;
                }
            }
        }
        return ServerMap.get(serverId);
    }

    //todo 目前放回首个
    public ServerConfig getChannelKey(int protocolId, Long userId, int flag) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        List<ServerConfig> channelKey = serverConfigManager.getChannelKey(protocolId);
        if (channelKey == null) {
            return null;
        }

        com.slg.module.config.ServerConfig serverConfig = channelKey.get(0);
        return serverConfig;
    }
}
