# 3. 连接池与线程池调优

## 3.1 数据库连接池调优

### 现状：futures-order 使用 Druid（需保留）

```yaml
# futures-order 当前 application.yml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 30000
      test-on-borrow: true
      validation-query: SELECT 1
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      filters: stat,wall
```

### 优化后 Druid 配置（保留 stat,wall，增加泄漏检测）

```yaml
# ✅ futures-order 优化后的 Druid 配置
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 10            # 从20→10, CPU=2核 → (2*2)+2=6, 留余量设为10
      max-wait: 30000
      # ✅ 新增：连接泄漏检测
      remove-abandoned: true
      remove-abandoned-timeout: 60
      log-abandoned: true
      # ✅ 新增：连接有效性检查（更轻量）
      validation-query: SELECT 1
      validation-query-timeout: 3
      test-while-idle: true      # 代替 test-on-borrow（性能更好）
      test-on-borrow: false      # 改为 false，避免每次请求都检测
      test-on-return: false
      time-between-eviction-runs-millis: 30000
      min-evictable-idle-time-millis: 600000
      # ✅ 优化连接池大小
      max-pool-prepared-statement-per-connection-size: 20
      filters: stat,wall,slf4j   # 增加 slf4j 日志
```

### 其他使用 HikariCP 的服务

```yaml
# ✅ futures-account / futures-fund / futures-settlement / futures-admin 使用 HikariCP
# 统一推荐配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 10              # (2*2)+2=6 → 留余量设10
      minimum-idle: 5
      connection-timeout: 30000           # 30秒连接超时
      idle-timeout: 600000                # 10分钟空闲
      max-lifetime: 1800000               # 30分钟最大生命周期
      leak-detection-threshold: 60000     # 60秒连接泄漏检测
      connection-test-query: SELECT 1
      pool-name: HikariPool-${spring.application.name}
      validation-timeout: 3000
```

## 3.2 线程池调优

### 风险：Spring @Async 默认线程池为无界队列

```java
// 🔴 风险代码：futures-common 中的异步任务
// 当前：Spring @Async 使用默认 SimpleAsyncTaskExecutor（每次新建线程！）
@Service
public class NotificationService {
    @Async  // ← 默认 SimpleAsyncTaskExecutor，无界线程
    public void sendNotification(String userId, String message) {
        // ...
    }
}

// ✅ 修复：配置有界队列的线程池
@Configuration
@EnableAsync
public class AsyncTaskPoolConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);        // ✅ 有界队列
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()  // ✅ 调用者运行
        );
        executor.setThreadNamePrefix("async-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

### 各服务业务隔离的线程池

```yaml
# ✅ 为不同业务创建独立的、命名的线程池
spring:
  task:
    execution:
      thread-name-prefix: default-task-
      pool:
        core-size: 4
        max-size: 8
        queue-capacity: 1000
        keep-alive: 60s
```

### 订单处理线程池（futures-order）

```java
@Configuration
public class OrderThreadPoolConfig {

    /** 订单处理线程池 - 隔离于其他异步任务 */
    @Bean("orderExecutor")
    public ThreadPoolTaskExecutor orderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler((r, e) -> {
            log.error("订单线程池已满! active={}, queued={}, poolSize={}",
                e.getActiveCount(), e.getQueue().size(), e.getPoolSize());
            throw new RejectedExecutionException("Order thread pool exhausted");
        });
        executor.setThreadNamePrefix("order-");
        executor.initialize();
        return executor;
    }

    /** 行情推送线程池 - 独立于订单处理 */
    @Bean("marketPushExecutor")
    public ThreadPoolTaskExecutor marketPushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(2000);  // 行情突发量大，队列稍大
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.DiscardPolicy()  // 行情可丢弃
        );
        executor.setThreadNamePrefix("market-push-");
        executor.initialize();
        return executor;
    }
}
```

### 撮合引擎 Disruptor 配置（无需修改）

```java
// 当前 DisruptorConfig 设计合理：
// - Ring Buffer 大小 65536（2^16）
// - 单消费者单线程
// - 无锁设计
// ✅ 建议：增加 JMX 监控

// 监控建议：添加 Disruptor 指标暴露
@Bean
public MeterBinder disruptorMetrics(OrderEventProducer producer) {
    return registry -> {
        Gauge.builder("matching.ringbuffer.remaining", producer,
                p -> (double) p.getRingBuffer().remainingCapacity())
            .description("Disruptor Ring Buffer 剩余容量")
            .register(registry);
    };
}
```

## 3.3 Redis 连接池 (Jedis)

当前所有服务统一配置，建议微调：

```yaml
spring:
  data:
    redis:
      jedis:
        pool:
          max-active: 16        # 建议：matching/risk 可降至 8
          max-idle: 8
          min-idle: 4
          max-wait: 2000ms
```

## 3.4 Nacos 线程池

当前所有服务统一通过 bootstrap.yml 配置 Nacos，建议添加客户端参数：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR}
        heart-beat-interval: 5000
        heart-beat-retry-interval: 3000  # 心跳重试间隔
      config:
        server-addr: ${NACOS_ADDR}
        timeout: 3000
```
