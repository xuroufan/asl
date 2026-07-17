# Feign 接口超时与重试配置

> 适用: order-service / fund-service / market-service / risk-service / admin-service

---

## 1. 超时配置总则

| 调用链路 | 业务场景 | connectTimeout | readTimeout | 说明 |
|----------|----------|:---------:|:--------:|------|
| order → fund | 验资（下单冻结资金） | 2s | 5s | 资金操作需快速响应 |
| order → risk | 验仓（风控检查） | 1s | 3s | 风控必须低延迟，超时则拒绝订单 |
| order → matching | 提交撮合 | 1s | 10s | 撮合可能等待队列 |
| admin → report | 后台报表查询 | 5s | 30s | 大查询可容忍更长时间 |
| market → quote | 第三方行情源 | 2s | 5s | 外部依赖需要设超时兜底 |

---

## 2. 全局默认配置

```yaml
# application.yml 基础模板（所有微服务）
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 2000       # 默认连接超时 2s
            read-timeout: 5000           # 默认读取超时 5s
            logger-level: BASIC          # 生产用 BASIC，开发用 FULL
      compression:
        request:
          enabled: true
          mime-types: application/json
          min-request-size: 2048
        response:
          enabled: true
```

---

## 3. 微服务级精细配置

### 3.1 order-service（最复杂的外部依赖）

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 2000
            read-timeout: 5000
          # ── 调 fund-service ──
          FundClient:
            url: ${FUND_SERVICE_URL:http://fund-service:8080}
            connect-timeout: 2000
            read-timeout: 5000
          # ── 调 risk-service ──
          RiskClient:
            url: ${RISK_SERVICE_URL:http://risk-service:8080}
            connect-timeout: 1000
            read-timeout: 3000
          # ── 调 matching-service ──
          MatchingClient:
            url: ${MATCHING_SERVICE_URL:http://matching-service:8080}
            connect-timeout: 1000
            read-timeout: 10000
```

### 3.2 admin-service（报表查询场景）

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000
            read-timeout: 10000
          # 报表查询可以容忍更长超时
          ReportClient:
            url: ${REPORT_SERVICE_URL:http://report-service:8080}
            connect-timeout: 5000
            read-timeout: 30000
```

### 3.3 market-service（外部行情源）

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 2000
            read-timeout: 5000
          # 第三方行情数据源
          QuoteProviderClient:
            url: ${QUOTE_PROVIDER_URL}
            connect-timeout: 2000
            read-timeout: 5000
```

---

## 4. Feign 客户端定义

```java
package com.hackfuture.order.client;

import com.hackfuture.common.model.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 资金服务 Feign 客户端
 */
@FeignClient(
    name = "fund-service",
    url = "${FUND_SERVICE_URL:http://fund-service:8080}",
    configuration = FundClientConfig.class,
    fallbackFactory = FundClientFallbackFactory.class   // 熔断降级
)
public interface FundClient {

    @GetMapping("/api/fund/balance")
    ApiResult getBalance(@RequestParam("userId") Long userId,
                         @RequestParam("asset") String asset);

    @PostMapping("/api/fund/freeze")
    ApiResult freezeBalance(@RequestBody FreezeRequest request);

    @PostMapping("/api/fund/unfreeze")
    ApiResult unfreezeBalance(@RequestBody FreezeRequest request);
}

/**
 * 风控服务 Feign 客户端
 */
@FeignClient(
    name = "risk-service",
    url = "${RISK_SERVICE_URL:http://risk-service:8080}",
    configuration = RiskClientConfig.class,
    fallbackFactory = RiskClientFallbackFactory.class
)
public interface RiskClient {

    @PostMapping("/api/risk/check-order")
    ApiResult checkOrder(@RequestBody RiskCheckRequest request);
}
```

---

## 5. Feign 重试策略

### 5.1 重试规则

| 操作 | 是否幂等 | 允许重试 | 重试次数 | 说明 |
|------|:--------:|:--------:|:--------:|------|
| 查询订单 | 是 | 是 | 2 | 网络抖动时自动恢复 |
| 查询余额 | 是 | 是 | 2 | 同上 |
| 下单 | 否 | 否 | 0 | 禁用重试，防止重复下单 |
| 冻结资金 | 否 | 否 | 0 | 禁用重试，由 Seata TCC 保证 |
| 撤单 | 否 | 否 | 0 | 禁用重试 |
| 行情查询 | 是 | 是 | 1 | 只重试一次 |

### 5.2 重试配置代码

```java
package com.hackfuture.order.client.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 通用 Feign 配置：仅对 GET 查询启用重试
 */
@Configuration
public class BaseFeignConfig {

    /**
     * 默认重试器（用于幂等查询接口）
     * 间隔 100ms，最大间隔 1s，最多重试 2 次
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100L, TimeUnit.SECONDS.toMillis(1L), 2);
    }
}

/**
 * 交易相关客户端配置：禁用重试
 * 用于 OrderClient、FundClient 的写操作
 */
@Configuration
public class NoRetryFeignConfig {

    /**
     * 从不重试（用于下单、冻结等非幂等写操作）
     */
    @Bean
    public Retryer noRetryFeign() {
        return Retryer.NEVER_RETRY;
    }
}
```

### 5.3 在 FeignClient 中引用

```java
// 查询类 FeignClient — 带重试
@FeignClient(
    name = "fund-service",
    configuration = BaseFeignConfig.class,   // 使用带重试的配置
    ...
)
public interface FundQueryClient { }

// 写操作 FeignClient — 无重试
@FeignClient(
    name = "fund-service",
    configuration = NoRetryFeignConfig.class,   // 禁用重试
    ...
)
public interface FundWriteClient { }
```

### 5.4 重试日志

```java
import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingRetryer extends Retryer.Default {

    public LoggingRetryer() {
        super(100L, TimeUnit.SECONDS.toMillis(1L), 2);
    }

    @Override
    public void continueOrPropagate(RetryableException e) {
        Request request = e.request();
        log.warn("Feign 重试: method={}, url={}, attempt={}, error={}",
                request.httpMethod(),
                request.url(),
                super,  // 无法直接获取当前重试次数，通过日志格式补充
                e.getMessage());
        super.continueOrPropagate(e);
    }
}
```

---

## 6. Feign 调试点配置

```yaml
# 开启 Feign 请求/响应日志 (生产用 BASIC，排查问题时用 HEADERS 或 FULL)
logging:
  level:
    com.hackfuture.order.client: DEBUG
    com.hackfuture.*.client: DEBUG
```

### 6.1 全局超时监控

```java
import feign.Request;
import feign.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign 拦截器：记录超时调用详情
 */
@Slf4j
public class TimeoutLoggingInterceptor implements feign.RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 请求发起时不处理，在 ResponseInterceptor 中处理
    }
}

// 或者通过 Feign 的 Capability 机制
import feign.Capability;
import feign.InvocationHandlerFactory;

public class TimeoutMetricCapability implements Capability {

    @Override
    public InvocationHandlerFactory customize(InvocationHandlerFactory factory) {
        return (target, dispatch, factory2) -> {
            // 包装原始方法调用，记录耗时
            return (proxy, method, args) -> {
                long start = System.currentTimeMillis();
                try {
                    return method.invoke(target, args);
                } finally {
                    long cost = System.currentTimeMillis() - start;
                    if (cost > 3000) {  // 超过 3 秒的调用记录警告
                        log.warn("Feign 慢调用: {}.{} 耗时={}ms",
                                target.type().getSimpleName(),
                                method.getName(), cost);
                    }
                }
            };
        };
    }
}
```
