# 数据库连接池防泄漏

> 适用: order-service / matching-service / fund-service / risk-service / market-service

---

## 1. HikariCP 防泄漏配置

### 1.1 application.yml 统一模板

```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      # ---------- 诊断 - 连接泄漏检测 ----------
      leak-detection-threshold: 30000    # 连接占用 > 30 秒 => 打印告警堆栈

      # ---------- 超时 ----------
      connection-timeout: 30000           # 等待连接超时
      idle-timeout: 600000                # 10 分钟空闲回收
      max-lifetime: 1800000               # 30 分钟连接最大存活
      keepalive-time: 60000               # 60 秒心跳（防防火墙断开）

      # ---------- 池大小 ----------
      maximum-pool-size: ${HIKARI_MAX_POOL:10}
      minimum-idle: ${HIKARI_MIN_IDLE:5}

      # ---------- 健康检查 ----------
      connection-test-query: SELECT 1
      validation-timeout: 5000
      initialization-fail-timeout: 30000

      # ---------- JMX ----------
      register-mbeans: true
      pool-name: ${spring.application.name:-unknown}-pool
```

### 1.2 各微服务差异化配置

```yaml
# order-service 的 application.yml
spring:
  application:
    name: order-service
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      leak-detection-threshold: 30000

---
# fund-service 的 application.yml
spring:
  application:
    name: fund-service
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      leak-detection-threshold: 15000    # 资金操作要求更严格的泄漏检测
```

---

## 2. 连接泄漏主动检测

### 2.1 HikariCP 内置检测机制

当 `leakDetectionThreshold` 开启后，HikariCP 会在以下场景打印完整堆栈：

```
WARN  HikariPool-1 - Connection leak detection triggered for
  connection com.mysql.cj.jdbc.ConnectionImpl@7a3e3d3f,
  stack trace follows:
  java.lang.Exception: Possible connection leak
    at com.zaxxer.hikari.util.ConcurrentBag.borrow(...)
    at com.zaxxer.hikari.pool.HikariPool.getConnection(...)
    at org.springframework.jdbc.datasource.DataSourceUtils.doGetConnection(...)
    at org.springframework.jdbc.datasource.DataSourceUtils.getConnection(...)
    at org.springframework.jdbc.core.JdbcTemplate.execute(...)
    at com.hackfuture.order.dao.OrderMapper.findByUserId(...)  <-- 从这里出去的连接
```

### 2.2 自定义泄漏检测器

```java
@Component
@Slf4j
public class ConnectionLeakDetector {

    private final HikariDataSource dataSource;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(
                    r -> new Thread(r, "connection-leak-detector"));

    public ConnectionLeakDetector(DataSource dataSource) {
        this.dataSource = (HikariDataSource) dataSource;
        scheduleCheck();
    }

    private void scheduleCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();

            int active = pool.getActiveConnections();
            int idle = pool.getIdleConnections();
            int pending = pool.getPendingThreads();
            int total = pool.getTotalConnections();

            // 正常情况: active < total
            if (active > total * 0.9) {
                log.warn("连接池高占用! active={}, idle={}, pending={}, total={}",
                        active, idle, pending, total);
            }

            // 连接耗尽 = 可能泄漏
            if (active == total && total > 0) {
                log.error("连接池耗尽! active={}, pending={}", active, pending);
                // 触发线程转储辅助排查
                dumpThreads();
            }

        }, 10, 10, TimeUnit.SECONDS);
    }

    private void dumpThreads() {
        StringBuilder sb = new StringBuilder();
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean()
                .dumpAllThreads(true, true);
        for (ThreadInfo ti : threads) {
            if (ti.getThreadState() == Thread.State.RUNNABLE
                    || ti.getLockName() != null) {
                sb.append(ti.toString()).append("\n");
            }
        }
        log.warn("连接泄漏时的线程转储:\n{}", sb);
    }
}
```

---

## 3. @Transactional 长事务防泄漏

### 3.1 长事务的危害

```java
// 反例: @Transactional 包了外部 API 调用 + 循环
@Service
public class OrderService {

    @Transactional  // 事务内做远程调用 + 循环 —— 连接占用数十秒
    public void batchCancel(List<String> orderIds) {
        for (String orderId : orderIds) {
            Order order = orderMapper.findByOrderId(orderId);
            // 远程调用交易所接口（可能耗时 3-5 秒）
            exchangeApi.cancelOrder(order.getSymbol(), order.getOrderId());
            orderMapper.updateStatus(...);
        }
        // 事务在方法返回后才提交，期间一直占用连接
    }
}
```

### 3.2 修复: 缩小事务范围

```java
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final ExchangeApiClient exchangeApi;

    /**
     * 修复: 远程调用在事务外执行，
     * 只有 DB 操作放在事务内，缩短连接占用时间
     */
    @Transactional
    public void updateOrderStatus(String orderId, String status,
                                   BigDecimal filledQty, BigDecimal avgPrice) {
        // 只包含 DB 操作 (几毫秒)
        orderMapper.updateStatus(orderId, status, filledQty, avgPrice, System.currentTimeMillis());
    }

    public void batchCancelSafely(List<String> orderIds) {
        for (String orderId : orderIds) {
            Order order = orderMapper.findByOrderId(orderId);

            // 远程调用在事务外
            ExchangeResult result = exchangeApi.cancelOrder(
                    order.getSymbol(), order.getOrderId());

            if (result.isSuccess()) {
                // 小事务: 只更新 DB
                updateOrderStatus(orderId, "CANCELLED",
                        order.getFilledQuantity(), order.getAverageFilledPrice());
            }
        }
    }
}
```

### 3.3 @Transactional 使用规范

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionalRule {
    // @Transactional 禁止的几种模式
}

// 禁止: 事务内调用远程服务
// @Transactional
// public void placeOrder() {
//     fundService.freeze();     // 远程 RPC —— 释放连接等待网络
//     orderMapper.insert();     // 占用连接数十秒
// }

// 推荐: 事务只包裹 DB 操作
// public void placeOrder() {
//     fundService.freeze();     // 远程调用前释放拦截器连接
//     saveOrderToDb();          // 小事务
// }
//
// @Transactional
// public void saveOrderToDb(Order order) {
//     orderMapper.insert(order);
// }
```

---

## 4. K8s 存活/就绪探针

### 4.1 Deployment 配置

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: trading-backend
spec:
  template:
    spec:
      containers:
        - name: order-service
          # ...
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 2
```

### 4.2 自定义数据库健康检查

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {  // 3 秒超时
                return Health.up()
                        .withDetail("database", "reachable")
                        .build();
            }
            return Health.down()
                    .withDetail("database", "connection invalid")
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("database", "unreachable")
                    .build();
        }
    }
}
```

### 4.3 配置 liveness 与 readiness 区分

```yaml
# liveness: 数据库不可用 -> 重启 Pod
# readiness: 数据库不可用 -> 从 Service 摘除
#
# 两者区别:
# - livenessProbe 失败 -> kubelet 重启容器
# - readinessProbe 失败 -> 从 Service endpoints 中移除，不接收新流量

spring:
  actuator:
    health:
      livenessState:
        enabled: true
      readinessState:
        enabled: true
```

---

## 5. 防泄漏配置自查清单

- [ ] `leakDetectionThreshold=30000` 已开启（30 秒告警阈值）
- [ ] HikariCP `register-mbeans=true` 已开启（JMX 暴露给 Prometheus）
- [ ] 所有 @Transactional 方法不包含远程调用
- [ ] 存在连接池高占用的监控告警（active > 90% 触发）
- [ ] K8s readinessProbe 配置了数据库健康检查
- [ ] 连接耗尽时触发线程转储，辅助排查泄漏
- [ ] `maximum-pool-size` 未超过数据库最大连接数
