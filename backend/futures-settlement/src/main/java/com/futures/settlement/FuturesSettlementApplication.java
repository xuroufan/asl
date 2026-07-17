package com.futures.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.futures.settlement", "com.futures.common"})
@EnableDiscoveryClient
public class FuturesSettlementApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuturesSettlementApplication.class, args);
    }
}
