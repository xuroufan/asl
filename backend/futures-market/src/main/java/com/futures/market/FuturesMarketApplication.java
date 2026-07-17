package com.futures.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.futures.market", "com.futures.common"})
@EnableDiscoveryClient
public class FuturesMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuturesMarketApplication.class, args);
    }
}
