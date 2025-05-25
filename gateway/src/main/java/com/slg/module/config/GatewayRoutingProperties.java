package com.slg.module.config;

import com.slg.module.connection.ServerConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
@ConfigurationProperties(prefix = "gateway.routing")  // 从配置读取
public class GatewayRoutingProperties {
    private List<ServerConfig> servers;
    private static Map<Integer, Byte> protoGroupIdMap = new HashMap<>();//protoId-groupIdList

    private static Map<Byte, List<ServerConfig>> protoServerMap = new HashMap<>();//groupId-ServerConfig


    //统计
    // Key: 服务器IP或标识, Value: 连接数计数器
    private static final Map<Byte, LongAdder> SERVER_CONNECTION_COUNTS = new ConcurrentHashMap<>();//serverId - 数量

    /**
     * 客户端连接时调用（线程安全）
     * @param serverId 服务器唯一标识（如IP）
     */
    public static void increment(Byte serverId) {
        SERVER_CONNECTION_COUNTS.computeIfAbsent(serverId, k -> new LongAdder()).increment();
    }

    /**
     * 客户端断开时调用（线程安全）
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

    @PostConstruct
    public void init() {
        for (ServerConfig server : servers) {
            byte serverId = server.getServerId();
            String host = server.getHost();
            int port = server.getPort();
            byte groupId = server.getGroupId();
            int protoIdMin = server.getProtoIdMin();
            int protoIdMax = server.getProtoIdMax();
            for (int protoId = protoIdMin; protoId <= protoIdMax; protoId++) {
                protoGroupIdMap.put(protoId, groupId);
                List<ServerConfig> serverList = protoServerMap.get(groupId);
                if (serverList == null) {
                    serverList = new ArrayList<>();
                }
                serverList.add(new ServerConfig(serverId,host,port));
                protoServerMap.put(groupId, serverList);
            }
        }
    }

    /**
     * @param protocolId
     * @param flag       0:第一个,1:随机,2:连接旧服务器,其他连接特定服务器；
     * @return
     */
    public ServerConfig getServerIDByProtoId(int protocolId, int flag) {
        Byte groupId= protoGroupIdMap.get(protocolId);
        List<ServerConfig> serverConfigList = protoServerMap.get(groupId);
        if (flag == 0) {
            return serverConfigList.get(0);
        } else if (flag == 1) {
            return serverConfigList.get(0);
        }else if (flag == 2) {
            return serverConfigList.get(0);
        }
        return null;
    }
}
