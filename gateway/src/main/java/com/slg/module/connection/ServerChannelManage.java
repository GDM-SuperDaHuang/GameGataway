package com.slg.module.connection;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 目标服务器内部消息管理
 */
@Component
public class ServerChannelManage {
    //目标服务器连接管理
    private Map<Byte, Channel> serverChannelMap = new ConcurrentHashMap<>();//<serverId,channel>
    private Map<Byte, ServerConfig> serverConfigMap = new ConcurrentHashMap<>();////<serverId,ServerConfig>

    public Channel getChanelByIp(Byte serverId) {
        return serverChannelMap.getOrDefault(serverId, 0L);
    }

    public Channel removeChanelByIp(Byte serverId) {
        Channel remove = serverChannelMap.remove(serverId);
        serverConfigMap.remove(serverId);
        return remove;
    }

    public void put(Byte serverId, Channel channel, ServerConfig serverConfig) {
        serverChannelMap.put(serverId, channel);
        serverConfigMap.put(serverId, new ServerConfig(serverId, serverConfig.getHost(), serverConfig.getPort()));
    }

    public ServerChannelManage() {
    }

    public Map<Byte, ServerConfig> getServerConfigMap() {
        return serverConfigMap;
    }
}
