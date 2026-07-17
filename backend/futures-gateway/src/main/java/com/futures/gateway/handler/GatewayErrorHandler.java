package com.futures.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * 网关层统一异常处理器。
 *
 * <p>覆盖默认 WhiteLabel 错误响应，统一返回标准 JSON：
 * {@code { "code": 503, "msg": "Service Unavailable", "timestamp": 1700000000 }}
 *
 * <p>处理：NotFoundException, TimeoutException, ResponseStatusException,
 * ExpiredJwtException, SignatureException, IllegalArgumentException 等。
 */
@Slf4j
@Order(-1)
@Configuration
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status;
        String msg;
        String detail = null;

        switch (ex) {
            case NotFoundException e -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                msg = "服务不可达，请检查目标服务是否已启动";
                detail = e.getMessage();
            }
            case TimeoutException e -> {
                status = HttpStatus.GATEWAY_TIMEOUT;
                msg = "服务响应超时";
                detail = e.getMessage();
            }
            case ResponseStatusException e -> {
                status = HttpStatus.valueOf(e.getStatusCode().value());
                msg = e.getReason();
            }
            case ExpiredJwtException e -> {
                status = HttpStatus.UNAUTHORIZED;
                msg = "令牌已过期，请重新登录";
            }
            case SignatureException e -> {
                status = HttpStatus.UNAUTHORIZED;
                msg = "令牌签名校验失败";
            }
            case IllegalArgumentException e -> {
                status = HttpStatus.BAD_REQUEST;
                msg = e.getMessage() != null ? e.getMessage() : "请求参数错误";
            }
            case IllegalStateException e -> {
                status = HttpStatus.BAD_REQUEST;
                msg = e.getMessage() != null ? e.getMessage() : "请求状态异常";
            }
            case null, default -> {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                msg = "网关内部错误";
                detail = ex != null ? ex.getMessage() : "未知错误";
            }
        }

        if (status.is5xxServerError()) {
            log.error("[GatewayError] {} | path={} | error={}",
                    msg, exchange.getRequest().getURI().getPath(), detail, ex);
        } else {
            log.warn("[GatewayError] {} | path={} | detail={}",
                    msg, exchange.getRequest().getURI().getPath(), detail);
        }

        response.setStatusCode(status);
        return writeJson(response, Map.of(
                "code", status.value(),
                "msg", msg,
                "timestamp", Instant.now().getEpochSecond()
        ));
    }

    private Mono<Void> writeJson(ServerHttpResponse response, Map<String, Object> body) {
        try {
            byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("序列化错误响应失败", e);
            return response.setComplete();
        }
    }
}
