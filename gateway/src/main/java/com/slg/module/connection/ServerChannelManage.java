package com.slg.module.connection;

import com.slg.module.util.SystemTimeCache;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 目标服务器内部消息管理
 */
public class ServerChannelManage {
    private static ServerChannelManage instance;
    //目标服务器连接管理 connectionId = serverId * BASE + next
    private static final Map<Integer, Channel> serverChannelMap = new HashMap<>();//<connectionId,channel>
    private static final Map<Integer, Integer> serverMaxMap = new HashMap<>();//<serverId,max>

    private final static int BASE = 1000;

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


    //todo 负债均衡

    /**
     * 可以新建立连接
     *
     * @param serverId
     * @return null--需要添加连接
     */
    public Channel getChanel(int serverId) {
        Integer sum = serverMaxMap.getOrDefault(serverId, null);
        if (sum == null) {
            return null;
        }
        int d = (int) (SystemTimeCache.currentTimeMillis() % sum);
        int connectionId = serverId * BASE + d;
        return serverChannelMap.get(connectionId);
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
        return channels;
    }

    public void addChannel(int serverId, int connectionId, Channel channel) {
        serverChannelMap.put(connectionId, channel);
        updateServer(serverId);
    }

    //记录serverId的连接数目
    public void updateServer(int serverId) {
        int orDefault = serverMaxMap.getOrDefault(serverId, 0);
        orDefault++;
        serverMaxMap.put(serverId, orDefault);
    }


    //1开始
    public int nextChannelId(int serverId) {
        int max = 0;
        for (Map.Entry<Integer, Channel> entry : serverChannelMap.entrySet()) {
            Integer connectionId = entry.getKey();
            if (connectionId >= serverId * BASE && connectionId <= serverId * BASE + BASE - 1) {
                max = connectionId;
            }
        }
        if (max == 0) {
            return BASE * serverId;
        }
        return max + 1;
    }

}
