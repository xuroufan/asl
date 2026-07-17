package com.futures.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.futures.order", "com.futures.common"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.futures.order.mapper")
public class FuturesOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuturesOrderApplication.class, args);
    }
}
