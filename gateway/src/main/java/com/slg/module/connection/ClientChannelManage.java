package com.slg.module.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端连接管理
 */
public class ClientChannelManage {
    private static ClientChannelManage instance;

    // 私有构造函数
    private ClientChannelManage() {
    }

    // 双重检查锁定获取实例
    public static ClientChannelManage getInstance() {
        if (instance == null) {
            synchronized (ClientChannelManage.class) {
                if (instance == null) {
                    instance = new ClientChannelManage();
                }
            }
        }
        return instance;
    }


    //客户端连接管理
    private final Map<ChannelId, Long> channelUserIdMap = new ConcurrentHashMap<>();//channel-userId
    private final Map<Long, Channel> userIdChannelMap = new ConcurrentHashMap<>();//userId-channel

    //心跳
    private final Map<Long, Long> timeHeartMap = new ConcurrentHashMap<>();//userId-time

    //todo 删除密钥
    //加密密钥
    private final Map<ChannelId, DHKeyInfo> ipCipherMap = new ConcurrentHashMap<>();//ipInfo-key共享密钥
    private final Map<Long, DHKeyInfo> userIdCipherMap = new ConcurrentHashMap<>();//userID-key共享密钥

    //todo 删除服务器
    //用户服务器负载均衡，用户习惯
    Map<Long, Map<Integer, Integer>> userGroupServerMap = new ConcurrentHashMap<>();//userId--groupId--ServerId

    public Map<Long, Map<Integer, Integer>> getUserGroupServerMap() {
        return userGroupServerMap;
    }

    public void putUserGroupServerMap(Long userId, Integer groupId, Integer serverId) {
        Map<Integer, Integer> orDefault = userGroupServerMap.getOrDefault(userId, new HashMap<>());
        orDefault.put(groupId, serverId);
        userGroupServerMap.put(userId, orDefault);
    }

    public void put(Channel channel, Long userId) {
        channelUserIdMap.put(channel.id(), userId);
        userIdChannelMap.put(userId, channel);
    }

    //todo
    public void updateHearTime(Long userId, Long time) {
        timeHeartMap.put(userId, time);
    }

    public Long getHearTime(Long userId) {
        return timeHeartMap.getOrDefault(userId, 0L);
    }

    public Long getUserId(ChannelId channelId) {
        return channelUserIdMap.getOrDefault(channelId, null);
    }

    public void putCipher(Long userId, DHKeyInfo k) {
        userIdCipherMap.put(userId, k);
    }

    public DHKeyInfo getCipher(Long userId) {
        return userIdCipherMap.getOrDefault(userId, null);
    }

    public void putCipher(ChannelId channelId, DHKeyInfo k) {
        ipCipherMap.put(channelId, k);
    }

    public DHKeyInfo getCipher(ChannelId channelId) {
        return ipCipherMap.get(channelId);
    }

    public Channel getChannelByUserId(Long userId) {
        return userIdChannelMap.getOrDefault(userId, null);
    }


    // 断开连接时
    public void remove(Channel channel) {
        Long userId = channelUserIdMap.remove(channel.id());
        if (userId != null) {
            userIdChannelMap.remove(userId);
            //断开删除习惯
            removeServerInfo(userId);
        }
    }

    //密钥删除 todo
    public void removeKey(ChannelId channelId, long userId) {
        ipCipherMap.remove(channelId);
        userIdCipherMap.remove(userId);
    }

    //服务器删除 todo
    public void removeServerInfo(Long userId) {
        userGroupServerMap.remove(userId);
    }


}
