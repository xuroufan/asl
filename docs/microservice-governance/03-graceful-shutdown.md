# 优雅关闭与全局异常处理

> 确保 K8s Pod 终止时正在处理的请求能完成、新请求不被路由到关闭中的 Pod

---

## 1. Spring Boot 优雅关闭配置

### 1.1 application.yml

```yaml
# ===== 所有微服务统一配置 =====

server:
  shutdown: graceful                          # 优雅关闭（默认是 immediate）
  port: 8080

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s           # 最大等待 30 秒，超时则强制关闭

  # Tomcat 连接等待（在关闭前不再接受新请求）
  tomcat:
    connection-timeout: 5000
    max-connections: 8192
    threads:
      max: 200
      min-spare: 20
```

### 1.2 关闭前清理钩子

```java
package com.hackfuture.common.shutdown;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 应用关闭时清理资源
 */
@Slf4j
@Component
public class GracefulShutdownHook {

    private final ExecutorService asyncExecutor;  // 注入异步线程池

    // 通过构造器注入

    @PreDestroy
    public void onShutdown() {
        log.info("应用开始优雅关闭，等待正在处理的请求完成...");

        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(25, TimeUnit.SECONDS)) {
                    log.warn("异步线程池 25s 未完全关闭，执行强制关闭");
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("关闭中断", e);
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("应用优雅关闭完成");
    }
}
```

---

## 2. K8s Pod 生命周期配置

### 2.1 完整 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: trading-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1            # 允许临时多出 1 个 Pod
      maxUnavailable: 0      # 确保至少 2 个 Pod 可用
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      terminationGracePeriodSeconds: 60    # K8s 总等待时间（大于 spring.lifecycle.timeout）
      containers:
        - name: order-service
          image: registry.blackfuture.io/order-service:latest
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: JAVA_OPTS
              value: "-Xms3g -Xmx3g ..."
          # ===== 关键：优雅关闭顺序 =====
          lifecycle:
            preStop:
              exec:
                command:
                  - "sh"
                  - "-c"
                  - |
                    echo "preStop: 通知注册中心摘除本实例..."
                    # 1. 从 Nacos/Consul 摘除（让新请求不再路由过来）
                    curl -X PUT "http://nacos.internal:8848/nacos/v1/ns/instance?serviceName=order-service&ip=${POD_IP}&port=8080&enabled=false" || true
                    
                    # 2. 等待 Spring Boot 优雅关闭（actuator shutdown）
                    echo "preStop: 等待 30s 让 Spring Boot 完成优雅关闭..."
                    sleep 30
                    
                    # 3. 如果应用进程还在，强制终止
                    echo "preStop: 完成"
          # ===== 探针 =====
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 2
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
            failureThreshold: 3
          resources:
            requests:
              memory: "4Gi"
              cpu: "2"
            limits:
              memory: "4Gi"
              cpu: "4"
```

### 2.2 优雅关闭时序图

```
K8s 发起 Pod 删除
    │
    ├─ (T+0s)  Pod 进入 Terminating 状态
    │   ├─ Endpoint 移除 -> 新请求不再路由
    │   └─ preStop Hook 执行
    │       ├─ 从 Nacos 摘除实例
    │       ├─ sleep 5s (等待 DNS 缓存刷新)
    │       └─ Spring Boot 收到 SIGTERM
    │
    ├─ (T+5s)  Spring Boot 开始优雅关闭
    │   ├─ /actuator/health 返回 DOWN
    │   ├─ /actuator/readiness 返回 NOT_READY
    │   ├─ 停止接受新请求（Tomcat connector pause）
    │   └─ 等待正在处理的请求完成（最多 30s）
    │
    ├─ (T+35s) Spring Boot 关闭完成
    │   ├─ 关闭线程池
    │   ├─ 关闭数据源连接
    │   └─ 进程退出
    │
    └─ (T+60s) K8s 强制 SIGKILL（terminationGracePeriodSeconds）
```

---

## 3. Gateway 层超时日志记录

### 3.1 Spring Cloud Gateway 超时记录

```java
package com.hackfuture.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

/**
 * Gateway 全局超时日志记录
 * 记录所有耗时超过阈值的请求
 */
@Slf4j
@Component
public class TimeoutLoggingFilter implements GlobalFilter, Ordered {

    /** 慢请求阈值 (ms) */
    private static final long SLOW_THRESHOLD_MS = 3000;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String traceId = exchange.getRequest().getHeaders()
                .getFirst("X-Trace-Id");
        String userId = exchange.getRequest().getHeaders()
                .getFirst("X-User-Id");

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long cost = System.currentTimeMillis() - start;
                    HttpStatus status = exchange.getResponse().getStatusCode();

                    // 记录慢请求（> 3s）
                    if (cost > SLOW_THRESHOLD_MS) {
                        log.warn("[慢请求] traceId={}, userId={}, method={}, uri={}, status={}, cost={}ms",
                                traceId, userId,
                                exchange.getRequest().getMethod(),
                                exchange.getRequest().getURI(),
                                status, cost);
                    }

                    // 记录所有超时
                    if (status == HttpStatus.GATEWAY_TIMEOUT
                            || status == HttpStatus.SERVICE_UNAVAILABLE) {
                        log.error("[超时/不可用] traceId={}, userId={}, uri={}, status={}, cost={}ms",
                                traceId, userId,
                                exchange.getRequest().getURI(),
                                status, cost);
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;  // 最后执行
    }
}
```

### 3.2 Gateway 响应超时配置

```yaml
# gateway-service 配置文件
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000       # Gateway 到下游的连接超时
        response-timeout: 10s       # Gateway 到下游的响应超时（可用 Duration 格式）
      default-filters:
        - name: Retry
          args:
            retries: 1
            statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE
            methods: GET
```

---

## 4. 全局超时异常处理器

### 4.1 @ControllerAdvice 统一处理

```java
package com.hackfuture.common.exception;

import com.hackfuture.common.model.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * 全局超时异常处理器
 */
@Slf4j
@RestControllerAdvice
public class TimeoutExceptionHandler {

    /**
     * Feign 请求超时
     */
    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiResult handleTimeout(TimeoutException e) {
        log.warn("请求超时: {}", e.getMessage());
        return ApiResult.error(408, "请求超时，请稍后重试");
    }

    /**
     * Socket 读取超时
     */
    @ExceptionHandler(SocketTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiResult handleSocketTimeout(SocketTimeoutException e) {
        log.warn("Socket 读取超时: {}", e.getMessage());
        return ApiResult.error(408, "服务响应超时，请稍后重试");
    }

    /**
     * Sentinel 限流/熔断异常
     */
    @ExceptionHandler(com.alibaba.csp.sentinel.slots.block.BlockException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResult handleBlock(com.alibaba.csp.sentinel.slots.block.BlockException e) {
        log.warn("接口被限流/熔断: resource={}, rule={}", e.getRuleLimitApp(), e.getRule());
        return ApiResult.error(429, "请求过于频繁，请稍后重试");
    }

    /**
     * 通用服务不可用
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult handleGeneral(Exception e) {
        log.error("服务内部错误", e);
        return ApiResult.error(500, "系统繁忙，请稍后重试");
    }
}
```

### 4.2 标准错误响应格式

```json
// 超时响应
{
    "code": 408,
    "msg": "请求超时，请稍后重试",
    "data": null
}

// 限流响应
{
    "code": 429,
    "msg": "请求过于频繁，请稍后重试",
    "data": null
}

// 通用错误
{
    "code": 500,
    "msg": "系统繁忙，请稍后重试",
    "data": null
}
```

---

## 5. OpenAPI 响应定义

```java
// 在 Feign 接口上标注可能返回的 HTTP 状态码
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@FeignClient(name = "order-service")
public interface OrderClient {

    @ApiResponse(responseCode = "200", description = "成功")
    @ApiResponse(responseCode = "408", description = "请求超时")
    @ApiResponse(responseCode = "429", description = "请求被限流")
    @ApiResponse(responseCode = "503", description = "服务熔断")
    @PostMapping("/api/order/place")
    ApiResult placeOrder(@RequestBody PlaceOrderRequest request);
}
```

---

## 6. 配置自查清单

- [ ] `server.shutdown: graceful` 已配置
- [ ] `spring.lifecycle.timeout-per-shutdown-phase: 30s` 已配置
- [ ] K8s `terminationGracePeriodSeconds: 60`（大于 30s + preStop 时间）
- [ ] preStop 已执行 Nacos 实例摘除
- [ ] readinessProbe 使用 `/actuator/health/readiness`
- [ ] livenessProbe 使用 `/actuator/health/liveness`
- [ ] RollingUpdate 设置 `maxUnavailable: 0`（滚动更新不中断服务）
- [ ] Gateway 层的慢请求日志已开启
- [ ] 全局超时异常处理器已注册
- [ ] 非幂等接口的 Feign 重试已禁用
