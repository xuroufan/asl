package com.futures.order.config;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign 客户端全局配置 — 超时 + 重试
 *
 * 优化:
 * - connectTimeout: 1s (原默认 10s+)
 * - readTimeout: 3s (原默认 60s)
 * - Retryer: 关闭重试 (由 Sentinel 熔断替代)
 */
@Configuration
public class FeignConfig {

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
            1000L, TimeUnit.MILLISECONDS,   // connectTimeout: 1s
            3000L, TimeUnit.MILLISECONDS,   // readTimeout: 3s
            false                           // followRedirects
        );
    }

    /**
     * 关闭 Feign 自带重试, 由 Sentinel 统一管理熔断降级。
     * 防止重试风暴 + 超时堆积。
     */
    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
