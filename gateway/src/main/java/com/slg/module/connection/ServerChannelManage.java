package com.slg.module.connection;

import com.slg.module.config.ServerConfig;
import com.slg.module.rpc.interMsg.ForwardClient;
import com.slg.module.rpc.interMsg.QPSMonitor;
import com.slg.module.util.SystemTimeCache;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 目标服务器内部消息管理
 */
public class ServerChannelManage {
    private static ServerChannelManage instance;

    // 私有构造函数
    private ServerChannelManage() {
    }

    // 双重检查锁定获取实例
    public static ServerChannelManage getInstance() {
        if (instance == null) {
            synchronized (ClientChannelManage.class) {
                if (instance == null) {
                    instance = new ServerChannelManage();
                }
            }
        }
        return instance;
    }


    private static final ConcurrentHashMap<Integer, Object> SERVER_LOCKS = new ConcurrentHashMap<>(1);

    ForwardClient forwardClient = ForwardClient.getInstance();

    //目标服务器连接管理 connectionId = serverId * BASE + next
    private static final Map<Integer, Channel> serverChannelMap = new ConcurrentHashMap<>();//<connectionId,channel>
    //建立的连接数统计
    private static final Map<Integer, Integer> serverMaxMap = new ConcurrentHashMap<>();//<serverId,max>


    //内部服务器,
    private final Map<Long, Map<Integer, Integer>> userIdForwardChannelIdMap = new ConcurrentHashMap<>();//userId-<groupId-connectionId>


    private final static int BASE = 1000;

    // QPS 记录
    private final QPSMonitor monitor = QPSMonitor.getInstance();


    // 根据protocolId获取链接
    public Integer getChannelIdByUser(Long userId, int protocolId) {
        ServerConfigManager alreadyInstance = ServerConfigManager.getAlreadyInstance();
        if (alreadyInstance == null) {
            return null;
        }
        //
        Integer groupId = alreadyInstance.getGroupId(protocolId);
        if (groupId == null) {
            return null;
        }
        Map<Integer, Integer> integerChannelIdMap = userIdForwardChannelIdMap.get(userId);
        if (integerChannelIdMap == null) {
            return null;
        }
        Integer channelId = integerChannelIdMap.get(groupId);
        if (channelId == null) {
            return null;
        }
        return channelId;
    }

    //保存用户链接
    private void saveUserChannel(Long userId, int groupId, int connectionId) {
        HashMap<Integer, Integer> groupConnectionMap = new HashMap<>();
        groupConnectionMap.put(groupId, connectionId);
        userIdForwardChannelIdMap.put(userId, groupConnectionMap);
    }


    //todo 负债均衡

    /**
     * 可以新建立连接，todo 监控流量需要接入
     *
     * @param server
     * @return null--需要添加连接
     */
    public Channel getChanel(ServerConfig server) {
        int serverId = server.getServerId();
        Integer sum = serverMaxMap.getOrDefault(serverId, 0);
        long qps = monitor.getQPS(serverId);
        if (sum >= forwardClient.connectionMax) {
            int d = (int) (SystemTimeCache.currentTimeMillis() % sum);//sum==4,--->0,1,2,3
            int connectionId = serverId * BASE + d;
            //QPS 记录
            monitor.record(serverId);
            return serverChannelMap.get(connectionId);
        }

        if (sum >= forwardClient.connectionMin) {//可能尝试建立,todo
            if (qps > 20000) {
                Object lock = SERVER_LOCKS.computeIfAbsent(server.getServerId(), k -> new Object());
                synchronized (lock) {
                    return forwardClient.connection(server);
                }
            }
            int d = (int) (SystemTimeCache.currentTimeMillis() % sum);//sum==4,--->0,1,2,3
            int connectionId = serverId * BASE + d;
            //QPS 记录
            monitor.record(serverId);
            return serverChannelMap.get(connectionId);
        }

        // 初始化最小数目连接
        Channel connection = null;
        for (int i = 0; i < forwardClient.connectionMin; i++) {
            // 获取当前 serverId 对应的专用锁对象
            Object lock = SERVER_LOCKS.computeIfAbsent(server.getServerId(), k -> new Object());
            synchronized (lock) {
                connection = forwardClient.connection(server);
            }
        }
        return connection;
    }


    //尝试分配新链接
    private Integer allocationChanel2(ServerConfig server) {
        // 初始化最小数目连接
        Integer connectionId = null;
        // 获取当前 serverId 对应的专用锁对象
        Object lock = SERVER_LOCKS.computeIfAbsent(server.getServerId(), k -> new Object());
        synchronized (lock) {
            connectionId = forwardClient.connection2(server);
        }
        return connectionId;
    }

//    private Integer allocationChanel(ServerConfig server) {
//        // 初始化最小数目连接
//        return forwardClient.connection2(server);
//    }


    //分配链接
    public Integer allocationChannelToUserId(int protocolId, Long userId) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getAlreadyInstance();
        if (serverConfigManager == null) {
            System.out.printf("ServerConfigManager is null");
            return null;
        }
        List<ServerConfig> channelKey = serverConfigManager.getChannelKey(protocolId);
        if (channelKey == null) {
            System.out.printf(" List<ServerConfig> is null");
            return null;
        }
        //随机分配一个服务器
        ServerConfig serverConfig = channelKey.get(new Random().nextInt(channelKey.size()));
        Integer sum = serverMaxMap.getOrDefault(serverConfig.getServerId(), 0);
        if (sum < forwardClient.connectionMin) {
            Integer connectionId = null;
            // 获取当前 serverId 对应的专用锁对象
            Object lock2 = SERVER_LOCKS.computeIfAbsent(serverConfig.getServerId(), k -> new Object());
            synchronized (lock2) {
                Integer orDefault = serverMaxMap.getOrDefault(serverConfig.getServerId(), 0);
                if (orDefault >= forwardClient.connectionMin){
                    return serverConfig.getServerId() * BASE + new Random().nextInt(orDefault);
                }
                //新链接
                connectionId = forwardClient.connection2(serverConfig);
                if (connectionId==null) return null;
                saveUserChannel(userId, serverConfig.getGroupId(), connectionId);
            }
            return connectionId;
        }
        // 扩容链接 todo

        return serverConfig.getServerId() * BASE + new Random().nextInt(sum);
    }


    /**
     * 服务器标识符：serverId
     *
     * @param serverId 服务器标识符， 移除所有的连接
     */
    public ArrayList<Channel> removeServerChanel(int serverId) {
        ArrayList<Channel> channels = new ArrayList<>();
        for (Map.Entry<Integer, Channel> entry : serverChannelMap.entrySet()) {
            Integer connectionId = entry.getKey();
            if (connectionId >= serverId * BASE && connectionId <= serverId * BASE + BASE - 1) {
                Channel remove = serverChannelMap.remove(connectionId);
                channels.add(remove);
            }
        }

        //清除用户链接
        for (Map.Entry<Long, Map<Integer, Integer>> userEntry : userIdForwardChannelIdMap.entrySet()) {
//            Long userId = userEntry.getKey();
            Map<Integer, Integer> map = userEntry.getValue();
            for (Map.Entry<Integer, Integer> integerIntegerEntry : map.entrySet()) {
                Integer groupId = integerIntegerEntry.getKey();
                Integer userConnectionId = integerIntegerEntry.getValue();
                if (userConnectionId >= serverId * BASE && userConnectionId <= serverId * BASE + BASE - 1) {
                    map.remove(groupId);
                }
            }
        }

        serverMaxMap.remove(serverId);
        return channels;
    }


    // 移除一条连接
    public void removeOneChanel(int serverId) {
        ArrayList<Channel> channels = new ArrayList<>();
        int max = 0;
        for (Map.Entry<Integer, Channel> entry : serverChannelMap.entrySet()) {
            Integer connectionId = entry.getKey();
            if (connectionId >= serverId * BASE && connectionId <= serverId * BASE + BASE - 1) {
                if (max < connectionId) {
                    max = connectionId;
                }
            }
        }
        serverChannelMap.remove(max);
    }


    //获取服务器Id
    public Integer findServerByChanel(int port, String ip) {
        int serverId = 0;
        for (Map.Entry<Integer, Channel> entry : serverChannelMap.entrySet()) {
            SocketAddress socketAddress = entry.getValue().remoteAddress();
            int onePort = ((InetSocketAddress) socketAddress).getPort();
            String oneIp = ((InetSocketAddress) socketAddress).getHostString();
            if (oneIp.equals(ip) && onePort == port) {
                serverId = entry.getKey();
            }

        }
        serverId = serverId / BASE;
        return serverId;
    }

    public void addChannel(Integer serverId, int connectionId, Channel channel) {
        serverChannelMap.put(connectionId, channel);
        updateServer(serverId);
    }

    //记录serverId的连接数目
    private void updateServer(Integer serverId) {
        int orDefault = serverMaxMap.getOrDefault(serverId, 0);
        orDefault++;
        serverMaxMap.put(serverId, orDefault);
    }


    //0开始,获取下一个链接==BASE * serverId + 1
    public int nextChannelId(int serverId) {
        int max = 0;
        for (Map.Entry<Integer, Channel> entry : serverChannelMap.entrySet()) {
            Integer connectionId = entry.getKey();
            if (connectionId >= serverId * BASE && connectionId <= serverId * BASE + BASE - 1) {
                max = connectionId;
            }
        }
        if (max == 0) {//初次建立
            return BASE * serverId;
        }
        return max + 1;
    }

    public Channel getChanel(Integer connectionId) {
        return serverChannelMap.getOrDefault(connectionId, null);
    }

    public Map<Long, Map<Integer, Integer>> getUserIdForwardChannelIdMap() {
        return userIdForwardChannelIdMap;
    }


}
