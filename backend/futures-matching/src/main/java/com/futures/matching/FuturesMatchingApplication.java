package com.futures.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 撮合引擎服务启动类
 * <p>
 * 端口: 8082
 * 注册到 Nacos: futures-matching
 */
@SpringBootApplication(scanBasePackages = {"com.futures.matching", "com.futures.common"})
@EnableDiscoveryClient
public class FuturesMatchingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuturesMatchingApplication.class, args);
    }
}
