package com.slg.module.connection;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端连接管理
 */
@Component
public class ClientChannelManage {
    //客户端连接管理
    private final Map<Channel, Long> channelUserIdMap = new ConcurrentHashMap<>();//channel-userId
    private final Map<Long, Channel> userIdChannelMap = new ConcurrentHashMap<>();//userId-channel

    //心跳
    private final Map<Long, Long> timeHeartMap = new ConcurrentHashMap<>();//userId-time

    //todo 删除密钥
    //加密密钥
    private final Map<Channel, DHKeyInfo> ipCipherMap = new ConcurrentHashMap<>();//ipInfo-key共享密钥
    private final Map<Long, DHKeyInfo> userIdCipherMap = new ConcurrentHashMap<>();//userID-key共享密钥

    //todo 删除服务器
    //用户服务器负载均衡
    Map<Long, Map<Byte, Byte>> userGroupServerMap = new ConcurrentHashMap<>();//userId--groupId--ServerId

    public Map<Long, Map<Byte, Byte>> getUserGroupServerMap() {
        return userGroupServerMap;
    }

    public ClientChannelManage() {
    }

    public void put(Channel channel, Long userId) {
        channelUserIdMap.put(channel, userId);
        userIdChannelMap.put(userId, channel);
    }

    public void updateHearTime(Long userId, Long time) {
        timeHeartMap.put(userId, userId);
    }

    public Long getHearTime(Long userId) {
        return timeHeartMap.getOrDefault(userId, 0L);
    }

    public Long getUserId(Channel channel) {
        return channelUserIdMap.getOrDefault(channel, 0L);
    }

    public void putCipher(Long userId, DHKeyInfo k) {
        userIdCipherMap.put(userId, k);
    }

    public DHKeyInfo getCipher(Long userId) {
        return userIdCipherMap.get(userId);
    }

    public void putCipher(Channel channel, DHKeyInfo k) {
        ipCipherMap.put(channel, k);
    }

    public DHKeyInfo getCipher(String ip) {
        return ipCipherMap.get(ip);
    }

    public Channel getChannelByUserId(Long userId) {
        return userIdChannelMap.getOrDefault(userId, null);
    }

    // 断开连接时
    public void remove(Channel channel) {
        Long userId = channelUserIdMap.remove(channel);
        if (userId != null) {
            userIdChannelMap.remove(userId);
            removeServerInfo(userId);
        }
    }

    //密钥删除 todo
    public void removeKey(Channel channel,long userId) {
        ipCipherMap.remove(channel);
        userIdCipherMap.remove(userId);
    }

    //服务器删除 todo
    public void removeServerInfo(Long userId) {
        userGroupServerMap.remove(userId);
    }


}
