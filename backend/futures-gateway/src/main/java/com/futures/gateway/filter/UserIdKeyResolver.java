package com.futures.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 限流 Key 解析器 — 按用户 ID 进行令牌桶限流。
 *
 * <p>从请求头 {@code X-User-Id} 提取用户标识；对于未登录请求（白名单），
 * 回退到客户端 IP 地址进行限流。
 *
 * <p>配合 {@code RequestRateLimiter} 过滤器使用，确保每个用户独立令牌桶，
 * 避免单个恶意用户耗尽系统资源。
 */
@Slf4j
@Primary
@Component("userIdKeyResolver")
public class UserIdKeyResolver implements KeyResolver {

    /** 限流回退 Key 前缀 */
    private static final String ANONYMOUS_PREFIX = "anonymous:";

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return Mono.just(userId);
        }

        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        String key = ANONYMOUS_PREFIX + ip;
        log.debug("匿名请求限流Key: {}", key);
        return Mono.just(key);
    }
}
