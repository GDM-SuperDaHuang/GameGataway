package com.slg.module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EntranceApplication {
	public static void main(String[] args) {
		System.out.println("网关服务器开始启动.......");
		SpringApplication.run(EntranceApplication.class, args);
	}

}
