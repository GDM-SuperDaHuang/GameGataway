package com.slg.module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EntranceApplication {
	public static void main(String[] args) {
		System.out.println("网关服务器开始启动.......");
		// 设置泄漏检测级别（建议在开发环境使用）
		System.setProperty("io.netty.leakDetection.level", "PARANOID");
		SpringApplication.run(EntranceApplication.class, args);
	}

}
