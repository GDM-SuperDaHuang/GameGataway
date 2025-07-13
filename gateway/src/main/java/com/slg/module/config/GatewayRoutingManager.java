package com.slg.module.config;

import com.slg.module.connection.ClientChannelManage;
import com.slg.module.connection.ServerConfigManager;

import java.util.*;

public class GatewayRoutingManager {

    private static List<ServerConfig> servers;
    private static GatewayRoutingManager instance;
    ClientChannelManage channelManage = ClientChannelManage.getInstance();

    // 私有构造方法
    private GatewayRoutingManager() {
    }

    // 双重检查锁定获取实例
    public static GatewayRoutingManager getInstance() {
        if (instance == null) {
            synchronized (GatewayRoutingManager.class) {
                if (instance == null) {
                    instance = new GatewayRoutingManager();
                }
            }
        }
        return instance;
    }


    //todo 目前放回首个
    public ServerConfig getChannelKey(int protocolId, Long userId, int flag) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getAlreadyInstance();
        if (serverConfigManager == null) {
            return null;
        }
        List<ServerConfig> channelKey = serverConfigManager.getChannelKey(protocolId);
        if (channelKey == null) {
            return null;
        }
        Map<Integer, Integer> userServerMap = channelManage.getUserGroupServerMap().get(userId);
        if (userServerMap == null) {
            ServerConfig serverConfig = channelKey.get(0);
            channelManage.putUserGroupServerMap(userId, serverConfig.getGroupId(), serverConfig.getServerId());
        }

        ServerConfig serverConfig = channelKey.get(0);
        return serverConfig;
    }
}
