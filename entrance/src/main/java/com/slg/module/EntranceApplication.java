package com.slg.module;

import com.slg.module.rpc.interMsg.ForwardClient;
import com.slg.module.rpc.outside.GatewayServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EntranceApplication {
    private static GatewayServer gatewayServer;
    public static void main(String[] args) {
        System.out.println("网关服务器开始启动.......");
        // 设置泄漏检测级别（建议在开发环境使用）
		System.setProperty("io.netty.leakDetection.level", "PARANOID");
        ConfigurableApplicationContext context = SpringApplication.run(EntranceApplication.class, args);
        gatewayServer = new GatewayServer();
        gatewayServer.start(gatewayServer.getPort());
        // 3. 注册优雅关闭钩子
        registerShutdownHook(context);

    }

    private static void registerShutdownHook(ConfigurableApplicationContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("正在关闭网关服务器...");
            if (gatewayServer != null) {
                gatewayServer.shutdown(); // 关闭Netty
            }
            context.close(); // 关闭Spring
            System.out.println("网关服务器已关闭");
        }));
    }

}
