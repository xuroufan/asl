package com.futures.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 风控引擎服务启动类
 * <p>
 * 端口: 8085
 * 注册到 Nacos: futures-risk
 */
@SpringBootApplication(scanBasePackages = {"com.futures.risk", "com.futures.common"})
@EnableDiscoveryClient
public class FuturesRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuturesRiskApplication.class, args);
    }
}
