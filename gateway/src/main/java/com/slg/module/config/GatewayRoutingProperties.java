//package com.slg.module.config;
//
//import com.slg.module.connection.ClientChannelManage;
//import com.slg.module.connection.ServerConfig;
//import jakarta.annotation.PostConstruct;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.stereotype.Component;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.LongAdder;
//
//@Component
//@ConfigurationProperties(prefix = "gateway.routing")  // 从配置读取
//public class GatewayRoutingProperties {
//    private List<ServerConfig> servers;
//    byte[] ProtoGroupArr;//protoId-groupId
//    static int groupNumMap = 0;//group的种类
//
//    public static Map<Byte, ServerConfig> ServerMap = new HashMap<>();//ServerId-Server
//
//    public Map<Byte, ServerConfig> getServerMap() {
//        return ServerMap;
//    }
//
//
//    //统计
//    // Key: 服务器IP或标识, Value: 连接数计数器
//    private static final Map<Byte, LongAdder> SERVER_CONNECTION_COUNTS = new ConcurrentHashMap<>();//serverId - 数量
//
//    /**
//     * 客户端连接时调用（线程安全）
//     *
//     * @param serverId 服务器唯一标识（如IP）
//     */
//    public static void increment(Byte serverId) {
//        SERVER_CONNECTION_COUNTS.computeIfAbsent(serverId, k -> new LongAdder()).increment();
//    }
//
//
//    @PostConstruct
//    public void init() {
//        int max = 0;
//        ArrayList<Byte> groupList = new ArrayList<>();
//        for (ServerConfig server : servers) {
//            int protoIdMax = server.getProtoIdMax();
//            if (protoIdMax > max) {
//                max = protoIdMax;
//            }
//            byte groupId = server.getGroupId();
//            if (!groupList.contains(groupId)) {
//                groupNumMap++;
//                groupList.add(groupId);
//            }
//            ServerMap.put(server.getServerId(), server);
//        }
//        ProtoGroupArr = new byte[max + 1];
//        for (ServerConfig server : servers) {
//            byte groupId = server.getGroupId();
//            int protoIdMin = server.getProtoIdMin();
//            int protoIdMax = server.getProtoIdMax();
//            for (int protoId = protoIdMin; protoId <= protoIdMax; protoId++) {
//                ProtoGroupArr[protoId] = groupId;
//            }
//        }
//    }
//
//    /**
//     * 客户端断开时调用（线程安全）
//     *
//     * @param serverId 服务器唯一标识
//     */
//    public static void decrement(Byte serverId) {
//        LongAdder counter = SERVER_CONNECTION_COUNTS.get(serverId);
//        if (counter != null) {
//            counter.decrement();
//            // 可选：连接数为0时移除记录（避免内存泄漏）
//            if (counter.sum() == 0) {
//                SERVER_CONNECTION_COUNTS.remove(serverId, counter);
//            }
//        }
//    }
//
//    /**
//     * 获取当前连接数
//     *
//     * @param serverId 服务器唯一标识
//     * @return 连接数（若服务器不存在则返回0）
//     */
//    public static long getCount(String serverId) {
//        LongAdder counter = SERVER_CONNECTION_COUNTS.get(serverId);
//        return counter == null ? 0 : counter.sum();
//    }
//
//    /**
//     * 打印所有服务器的连接数（调试用）
//     */
//    public static void printAllStats() {
//        SERVER_CONNECTION_COUNTS.forEach((serverId, counter) ->
//                System.out.printf("Server: %s, Connections: %d\n", serverId, counter.sum())
//        );
//    }
//
//    public void setServers(List<ServerConfig> servers) {
//        this.servers = servers;
//    }
//
//
//    /**
//     * @param protocolId
//     * @param flag       1连接旧服务器
//     * @return
//     */
//    public ServerConfig getServer(int protocolId, Long userId, int flag) {
//        byte groupId = ProtoGroupArr[protocolId];
//        Map<Byte, Byte> gsMap = ClientChannelManage.getInstance().getUserGroupServerMap().computeIfAbsent(userId, k -> new HashMap<>(groupNumMap));
//        Byte serverId = gsMap.get(groupId);
//        if (serverId == null) {//初始化
//            //选一个 groupId 内的第一个server,可选均衡
//            for (ServerConfig server : servers) {
//                if (server.getGroupId() == groupId) {
//                    gsMap.put(groupId, server.getServerId());
//                    return server;
//                }
//            }
//        }
//        return ServerMap.get(serverId);
//    }
//}
