package com.futures.fund;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 资金管理服务启动类
 * <p>
 * 端口: 8084
 * 注册到 Nacos: futures-fund
 */
@SpringBootApplication(scanBasePackages = {"com.futures.fund", "com.futures.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class FuturesFundApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuturesFundApplication.class, args);
    }
}
