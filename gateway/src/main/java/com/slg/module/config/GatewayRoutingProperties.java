package com.slg.module.config;

import com.slg.module.connection.ServerConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "gateway.routing")  // 从配置读取
public class GatewayRoutingProperties {
    private List<ServerConfig> servers;
    private static Map<Integer, ServerConfig> protoMap = new HashMap<>();//protoId-ServerConfig

    public void setServers(List<ServerConfig> servers) {
        this.servers = servers;
    }

    @PostConstruct
    public void init() {
        for (ServerConfig server : servers) {
            byte serverId = server.getServerId();
            int protoIdMin = server.getProtoIdMin();
            int protoIdMax = server.getProtoIdMax();
            for (int i = protoIdMin; i <= protoIdMax; i++) {
                protoMap.put(i, server);
            }
        }
    }

    public ServerConfig getServerByProtoId(int protocolId) {
        return protoMap.get(protocolId);
    }
}
