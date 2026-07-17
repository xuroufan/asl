package com.futures.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FuturesGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuturesGatewayApplication.class, args);
    }
}
