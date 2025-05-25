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

    //加密密钥
    private final Map<String, DHKeyInfo> ipCipherMap = new ConcurrentHashMap<>();//ipInfo-key共享密钥
    private final Map<Long, DHKeyInfo> userIdCipherMap = new ConcurrentHashMap<>();//userID-key共享密钥

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

    public void putCipher(String ip, DHKeyInfo k) {
        ipCipherMap.put(ip, k);
    }

    public DHKeyInfo getCipher(String ip) {
        return ipCipherMap.get(ip);
    }

    public Channel getChannelByUserId(Long userId) {
        return userIdChannelMap.getOrDefault(userId, null);
    }

    public void remove(Channel channel) {
        Long userId = channelUserIdMap.remove(channel);
        if (userId!=null){
            userIdChannelMap.remove(userId);
        }
    }


}
