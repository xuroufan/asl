package com.futures.account;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 账户服务启动类。
 *
 * 负责用户认证、KYC、2FA、权限管理功能。
 */
@SpringBootApplication(scanBasePackages = {"com.futures.account", "com.futures.common"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@MapperScan("com.futures.account.mapper")
public class FuturesAccountApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuturesAccountApplication.class, args);
    }
}
