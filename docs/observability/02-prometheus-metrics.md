# 业务指标监控（Micrometer + Prometheus）

> 自定义指标: 订单量、撮合延迟、资金冻结、WebSocket 连接

---

## 1. Micrometer 依赖

```groovy
// build.gradle
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
      base-path: /actuator
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
        step: 15s   # 每 15 秒暴露一次指标
```

---

## 2. 订单业务指标（order-service）

```java
package com.hackfuture.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单业务指标
 */
@Component
public class OrderMetrics {

    private final MeterRegistry registry;

    // 按方向 + 合约维度统计下单量
    private final Counter orderPlaceCounter;

    // 下单到成交的耗时分布
    private final Timer orderLatencyTimer;

    // 挂单深度（当前活跃订单数）
    private final AtomicLong activeOrdersGauge;

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;

        // ── 1. 下单量计数器 ──
        this.orderPlaceCounter = Counter.builder("order.place.total")
                .description("Total number of orders placed")
                .register(registry);

        // ── 2. 下单到成交延迟 ──
        this.orderLatencyTimer = Timer.builder("order.latency.seconds")
                .description("Order placement to fill latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .sla(TimeUnit.SECONDS.toNanos(1),
                     TimeUnit.SECONDS.toNanos(5),
                     TimeUnit.SECONDS.toNanos(10))
                .register(registry);

        // ── 3. 活跃订单数（Gauge） ──
        this.activeOrdersGauge = registry.gauge("order.active.total",
                new AtomicLong(0));
    }

    /**
     * 记录下单
     * @param symbol 合约代码
     * @param side   BUY/SELL
     */
    public void recordOrderPlaced(String symbol, String side) {
        orderPlaceCounter.increment();
        // 带标签的计数器，用于按维度切片
        Counter.builder("order.place.by_symbol")
                .tag("symbol", symbol)
                .tag("side", side)
                .register(registry)
                .increment();
    }

    /**
     * 记录下单到成交的耗时
     */
    public void recordOrderLatency(long latencyMs) {
        orderLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 更新活跃订单数
     */
    public void setActiveOrders(int count) {
        activeOrdersGauge.set(count);
    }
}
```

```prometheus
# 采集到的 Prometheus 指标示例：
# HELP order_place_total Total number of orders placed
# TYPE order_place_total counter
order_place_total 15234.0

# HELP order_place_by_symbol Orders by symbol and side
# TYPE order_place_by_symbol counter
order_place_by_symbol{symbol="HSI2309",side="BUY"} 8234.0
order_place_by_symbol{symbol="HSI2309",side="SELL"} 7000.0

# HELP order_latency_seconds Order placement to fill latency
# TYPE order_latency_seconds summary
order_latency_seconds_count 15234.0
order_latency_seconds_sum 7645.0
order_latency_seconds{quantile="0.5"} 0.35
order_latency_seconds{quantile="0.95"} 2.1
order_latency_seconds{quantile="0.99"} 5.8
```

---

## 3. 撮合延迟指标（matching-service）

```java
package com.hackfuture.matching.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MatchingMetrics {

    private final MeterRegistry registry;

    // 撮合引擎延迟分布
    private final Timer matchingLatency;

    // 撮合吞吐量（每分钟撮合笔数）
    private final Timer matchingThroughput;

    public MatchingMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.matchingLatency = Timer.builder("matching.latency.microseconds")
                .description("Matching engine latency in microseconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .sla(
                    TimeUnit.MICROSECONDS.toNanos(50),
                    TimeUnit.MICROSECONDS.toNanos(100),
                    TimeUnit.MILLISECONDS.toNanos(1)
                )
                .register(registry);

        this.matchingThroughput = Timer.builder("matching.throughput")
                .description("Matching throughput (trades per interval)")
                .register(registry);
    }

    public void recordMatchLatency(long microseconds) {
        matchingLatency.record(microseconds, TimeUnit.MICROSECONDS);
    }

    public void recordTrade() {
        matchingThroughput.record(1, TimeUnit.MILLISECONDS);
    }
}
```

---

## 4. 资金冻结错误指标（fund-service）

```java
@Component
public class FundMetrics {

    private final Counter freezeErrors;
    private final Counter freezeSuccess;

    public FundMetrics(MeterRegistry registry) {
        // 资金冻结成功/失败计数
        this.freezeErrors = Counter.builder("fund.freeze.errors.total")
                .description("Total fund freeze failures")
                .register(registry);
        this.freezeSuccess = Counter.builder("fund.freeze.success.total")
                .description("Total fund freeze successes")
                .register(registry);

        // 账户余额 Gauge
        registry.gauge("fund.balance.total",
                new AtomicLong(0));
    }

    public void recordFreezeSuccess() {
        freezeSuccess.increment();
    }

    public void recordFreezeError(String reason) {
        freezeErrors.increment();
        Counter.builder("fund.freeze.errors.by_reason")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }
}
```

---

## 5. WebSocket 连接指标（market-service）

```java
@Component
public class WebSocketMetrics {

    private final AtomicLong connectionsGauge;

    public WebSocketMetrics(MeterRegistry registry) {
        this.connectionsGauge = registry.gauge(
                "websocket.connection.total",
                new AtomicLong(0));

        // 按订阅维度统计
        registry.gauge("websocket.subscriptions.total",
                new AtomicLong(0));
    }

    public void onConnect() {
        connectionsGauge.incrementAndGet();
    }

    public void onDisconnect() {
        connectionsGauge.decrementAndGet();
    }
}
```

---

## 6. 自定义指标汇总

| 指标名 | 类型 | 标签 | 微服务 |
|--------|:----:|------|--------|
| `order.place.total` | Counter | — | order-service |
| `order.place.by_symbol` | Counter | symbol, side | order-service |
| `order.latency.seconds` | Summary | P50/P95/P99 | order-service |
| `matching.latency.microseconds` | Summary | P50/P95/P99 | matching-service |
| `matching.throughput` | Timer | — | matching-service |
| `fund.freeze.errors.total` | Counter | — | fund-service |
| `fund.freeze.success.total` | Counter | — | fund-service |
| `fund.freeze.errors.by_reason` | Counter | reason | fund-service |
| `websocket.connection.total` | Gauge | — | market-service |
| `order.active.total` | Gauge | — | order-service |

---

## 7. Prometheus 抓取配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'trading-services'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: '(order|matching|fund|market|risk)-service'
        action: keep
      - source_labels: [__meta_kubernetes_pod_container_port_number]
        regex: '8080'
        action: keep
      - source_labels: [__address__]
        action: replace
        regex: '([^:]+)(?::\d+)?'
        replacement: '$1:8080'
        target_label: __address__
      - source_labels: [__meta_kubernetes_pod_label_app]
        target_label: service
```
