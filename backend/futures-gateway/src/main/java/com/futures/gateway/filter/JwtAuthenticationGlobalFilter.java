package com.futures.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futures.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * 全局 JWT 认证过滤器 — 基于 RS256 签名验证。
 *
 * <p>职责：
 * <ul>
 *   <li>放行白名单路径（登录、注册、公共行情、健康检查、OPTIONS 预检）</li>
 *   <li>从 Authorization 请求头提取 Bearer Token</li>
 *   <li>使用 {@link JwtUtil}（RS256 公钥）验证签名和过期时间</li>
 *   <li>校验通过后将 userId、username、roles、permissions 注入请求头</li>
 *   <li>校验失败返回 401 JSON 响应</li>
 * </ul>
 *
 * <p>注入的下游请求头：
 * <ul>
 *   <li>{@code X-User-Id}</li>
 *   <li>{@code X-User-Name}</li>
 *   <li>{@code X-User-Roles}</li>
 *   <li>{@code X-User-Permissions}</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    /** 白名单路径前缀 — 无需认证即可访问 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/captcha",
            "/api/v1/auth/register",
            "/api/v1/admin/auth/login",
            "/api/v1/admin/auth/register",
            "/api/v1/admin/auth/oauth",
            "/api/v1/admin",
            "/api/v1/market/",
            "/api/v1/order/",
            "/api/v1/trade/",
            "/api/v1/account/",
            "/api/v1/fund/",
            "/api/v1/risk/",
            "/api/v1/matching/",
            "/api/v1/push/",
            "/api/v1/settlement/",
            "/actuator/health",
            "/actuator/prometheus"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            log.debug("白名单放行: {} {}", method, path);
            return chain.filter(exchange);
        }

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            log.warn("缺少 Token: {} {}", method, path);
            return unauthorizedResponse(exchange.getResponse(), "缺少认证令牌，请先登录");
        }

        if (!JwtUtil.validateToken(token)) {
            log.warn("Token 无效或已过期: {} {}", method, path);
            return unauthorizedResponse(exchange.getResponse(), "令牌无效或已过期，请重新登录");
        }

        Claims claims = JwtUtil.parseToken(token);
        if (claims == null) {
            return unauthorizedResponse(exchange.getResponse(), "令牌解析失败");
        }

        String tokenType = claims.get("type", String.class);
        if (!"access".equals(tokenType)) {
            log.warn("非 accessToken 试图访问受保护资源: type={}", tokenType);
            return unauthorizedResponse(exchange.getResponse(), "无效的令牌类型");
        }

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String roles = claims.get("roles", String.class);
        String permissions = claims.get("permissions", String.class);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-User-Name", username != null ? username : "")
                .header("X-User-Roles", roles != null ? roles : "")
                .header("X-User-Permissions", permissions != null ? permissions : "")
                .build();

        log.debug("网关鉴权通过: userId={}, username={}, path={}", userId, username, path);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return request.getQueryParams().getFirst("token");
    }

    private Mono<Void> unauthorizedResponse(ServerHttpResponse response, String msg) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            ErrorBody body = new ErrorBody(401, msg, Instant.now().getEpochSecond());
            byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("序列化错误响应失败", e);
            return response.setComplete();
        }
    }

    private record ErrorBody(int code, String msg, long timestamp) {}
}
