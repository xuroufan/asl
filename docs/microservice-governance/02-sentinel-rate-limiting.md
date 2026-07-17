# Sentinel 限流与熔断配置

> 技术选型: Sentinel + Nacos 动态规则推送
> 适用: order-service / market-service / auth-service / matching-service

---

## 1. 依赖引入

```groovy
// build.gradle（各微服务）
implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel:2023.0.1.0'
implementation 'com.alibaba.csp:sentinel-datasource-nacos:1.8.8'  // Nacos 动态规则
```

---

## 2. 基础配置

```yaml
# application.yml 统一模板
spring:
  cloud:
    sentinel:
      enabled: true
      transport:
        dashboard: ${SENTINEL_DASHBOARD_URL:sentinel-dashboard:8080}  # 控制台地址
        port: 8719                            # 与 Dashboard 通信的端口
      eager: true                             # 提前加载 Sentinel（否则首次请求才初始化）
      datasource:
        # 限流规则 - 从 Nacos 动态拉取
        flow-rules:
          nacos:
            server-addr: ${NACOS_ADDR:nacos.internal:8848}
            namespace: trading
            data-id: ${spring.application.name}-sentinel-flow
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: flow
        # 熔断降级规则
        degrade-rules:
          nacos:
            server-addr: ${NACOS_ADDR:nacos.internal:8848}
            namespace: trading
            data-id: ${spring.application.name}-sentinel-degrade
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: degrade
      filter:
        enabled: true
        order: -100
```

---

## 3. 限流规则配置

### 3.1 order-service 限流规则

```json
// Nacos data-id: order-service-sentinel-flow
// group: SENTINEL_GROUP
[
    {
        "resource": "POST:/api/order/place",
        "count": 50.0,
        "grade": 1,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    },
    {
        "resource": "POST:/api/order/cancel",
        "count": 30.0,
        "grade": 1,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    },
    {
        "resource": "GET:/api/order/history",
        "count": 100.0,
        "grade": 1,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    }
]
```

### 3.2 market-service 限流规则

```json
// Nacos data-id: market-service-sentinel-flow
[
    {
        "resource": "GET:/api/market/ticker",
        "count": 200.0,
        "grade": 1,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    },
    {
        "resource": "GET:/api/market/kline",
        "count": 100.0,
        "grade": 1,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    }
]
```

### 3.3 auth-service 限流规则（登录防暴力破解）

```json
// Nacos data-id: auth-service-sentinel-flow
[
    {
        "resource": "POST:/api/auth/login",
        "count": 5.0,
        "grade": 1,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    },
    {
        "resource": "POST:/api/auth/register",
        "count": 2.0,
        "grade": 1,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    }
]
```

**规则字段说明**:
| 字段 | 值 | 含义 |
|------|:---:|------|
| `resource` | POST:/api/order/place | 被保护的资源名（URL + Method） |
| `count` | 50.0 | 限流阈值 |
| `grade` | 1 | QPS 模式（0=线程数, 1=QPS） |
| `limitApp` | default | 针对来源（default=不区分） |
| `strategy` | 0 | 0=直接限流, 1=关联限流, 2=链路限流 |
| `controlBehavior` | 0 | 0=快速失败, 1=Warm Up, 2=匀速排队 |

---

## 4. 熔断降级规则

### 4.1 熔断规则配置

```json
// Nacos data-id: order-service-sentinel-degrade
[
    {
        "resource": "POST:/api/order/place",
        "grade": 0,
        "count": 0.2,
        "timeWindow": 5,
        "minRequestAmount": 10,
        "statIntervalMs": 10000
    },
    {
        "resource": "FundClient#freezeBalance(FreezeRequest)",
        "grade": 0,
        "count": 0.2,
        "timeWindow": 5,
        "minRequestAmount": 10,
        "statIntervalMs": 10000
    },
    {
        "resource": "POST:/api/order/cancel",
        "grade": 0,
        "count": 0.2,
        "timeWindow": 5,
        "minRequestAmount": 10,
        "statIntervalMs": 10000
    }
]
```

```json
// Nacos data-id: market-service-sentinel-degrade
[
    {
        "resource": "GET:/api/market/kline",
        "grade": 0,
        "count": 0.2,
        "timeWindow": 5,
        "minRequestAmount": 10,
        "statIntervalMs": 10000
    }
]
```

**熔断字段说明**:
| 字段 | 值 | 含义 |
|------|:---:|------|
| `grade` | 0 | 0=异常比例, 1=异常数, 2=慢调用比例 |
| `count` | 0.2 | 阈值：错误率 20% |
| `timeWindow` | 5 | 熔断时长：5 秒后进入半开状态 |
| `minRequestAmount` | 10 | 触发熔断的最小请求数 |
| `statIntervalMs` | 10000 | 统计时长：10 秒 |

---

## 5. 代码级限流 —— 灵活控制

### 5.1 注解方式

```java
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;

@Service
public class OrderService {

    /**
     * 下单接口：按用户 QPS 限流 50
     */
    @SentinelResource(
        value = "order:place",
        blockHandler = "handleBlock",
        fallback = "handleFallback"
    )
    public ApiResult placeOrder(PlaceOrderRequest request) {
        // 业务逻辑
        return ApiResult.success(order);
    }

    /**
     * 限流降级方法（触发限流时调用）
     */
    public ApiResult handleBlock(PlaceOrderRequest request, BlockException ex) {
        log.warn("下单被限流: userId={}, symbol={}", request.userId, request.symbol);
        return ApiResult.error(429, "请求过于频繁，请稍后重试");
    }

    /**
     * 熔断降级方法（触发熔断时调用）
     */
    public ApiResult handleFallback(PlaceOrderRequest request, Throwable ex) {
        log.error("下单熔断降级: userId={}, symbol={}, error={}",
                request.userId, request.symbol, ex.getMessage());
        return ApiResult.error(503, "交易系统繁忙，请稍后重试");
    }
}
```

### 5.2 按用户 ID 限流（自定义参数限流）

```java
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;

@Service
public class OrderService {

    private static final String KEY = "order:place:user";

    /**
     * 按用户 ID 限流：每个用户 QPS ≤ 5
     */
    public ApiResult placeOrder(PlaceOrderRequest request) {
        // 构建资源名: order:place:user:{userId}
        String resourceKey = KEY + ":" + request.getUserId();

        try (Entry entry = SphU.entry(resourceKey, EntryType.IN)) {
            // 业务逻辑
            return doPlaceOrder(request);
        } catch (BlockException ex) {
            log.warn("用户 {} 下单被限流", request.getUserId());
            return ApiResult.error(429, "下单太频繁，请稍后重试");
        }
    }
}
```

### 5.3 热点参数限流（与 Nacos 规则配合）

```json
// Nacos 热点参数限流规则
// data-id: order-service-sentinel-param-flow
[
    {
        "resource": "order:place",
        "count": 5,
        "paramIdx": 0,
        "paramFlowItemList": [],
        "durationInSec": 1,
        "grade": 1
    }
]
```

---

## 6. Sentinel 降级 Fallback 工厂

```java
package com.hackfuture.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * FundClient 降级工厂：熔断时返回兜底结果
 */
@Slf4j
@Component
public class FundClientFallbackFactory implements FallbackFactory<FundClient> {

    @Override
    public FundClient create(Throwable cause) {
        log.error("FundClient 熔断降级: {}", cause.getMessage());

        return new FundClient() {
            @Override
            public ApiResult getBalance(Long userId, String asset) {
                // 查询类降级：返回空余额，不影响主流程
                return ApiResult.warning("资金服务暂不可用，余额显示可能滞后");
            }

            @Override
            public ApiResult freezeBalance(FreezeRequest request) {
                // 写操作降级：告知调用方失败
                return ApiResult.error(503,
                        "资金服务繁忙，订单提交失败，请稍后重试");
            }

            @Override
            public ApiResult unfreezeBalance(FreezeRequest request) {
                return ApiResult.error(503, "资金服务繁忙");
            }
        };
    }
}
```

---

## 7. Sentinel 控制台部署

```yaml
# docker-compose for Sentinel Dashboard
version: '3.8'
services:
  sentinel-dashboard:
    image: bladex/sentinel-dashboard:1.8.8
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Dserver.port=8080
      - auth.username=sentinel
      - auth.password=sentinel
    volumes:
      - ./sentinel-logs:/root/logs/csp
    restart: always
```

访问 `http://sentinel-dashboard:8080` 可查看：
- 实时 QPS、响应时间、通过/拒绝数量
- 动态修改限流/熔断规则
- 查看调用链路（簇点链路）

---

## 8. 限流熔断配置速查

| 微服务 | 接口 | 限流 QPS | 熔断阈值 | 熔断时间 |
|--------|------|:--------:|:--------:|:--------:|
| order-service | POST /api/order/place | 50 | 20% 错误 | 5s |
| order-service | POST /api/order/cancel | 30 | 20% 错误 | 5s |
| market-service | GET /api/market/kline | 100 | 20% 错误 | 5s |
| market-service | GET /api/market/ticker | 200 | 20% 错误 | 5s |
| auth-service | POST /api/auth/login | 5 | — | — |
| auth-service | POST /api/auth/register | 2 | — | — |
