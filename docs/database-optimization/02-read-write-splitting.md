# 读写分离与从库路由

> 技术选型: ShardingSphere-JDBC（嵌入应用，零额外运维组件）
> 适用: order-service / fund-service / market-service / risk-service
> 不适用: matching-service（撮合引擎主要在内存运算，DB 仅写成交记录）

---

## 1. ShardingSphere-JDBC 配置

### 1.1 依赖

```groovy
// build.gradle（各微服务）
implementation 'org.apache.shardingsphere:shardingsphere-jdbc-core:5.5.0'
```

### 1.2 application.yml 读写分离配置

```yaml
spring:
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:sharding-readwrite.yaml
```

### 1.3 sharding-readwrite.yaml（核心配置）

```yaml
# order-service/src/main/resources/sharding-readwrite.yaml
#
# 架构: 1主2从
#   - master: trading-db-master.internal
#   - slave-a: trading-db-slave-a.internal
#   - slave-b: trading-db-slave-b.internal
#
# 健康检查: 从库延迟 > 5秒 时自动切回主库

mode:
  type: Standalone
  repository:
    type: JDBC
    props:
      jdbc_url: jdbc:h2:mem:shardingsphere_config

rules:
  - !READWRITE_SPLITTING
    data_sources:
      write_ds:       # 逻辑数据源名
        write_data_source_name: primary_ds
        read_data_source_names:
          - replica_ds_0
          - replica_ds_1
        transactional_read_query_strategy: PRIMARY  # 事务内读请求强制走主库
        load_balancer_name: round_robin              # 从库负载均衡策略

    load_balancers:
      round_robin:
        type: ROUND_ROBIN

  # ---------- 数据加密（可选：资金服务的敏感字段） ----------
  # - !ENCRYPT
  #   encryptors:
  #     aes_encryptor:
  #       type: AES
  #       props:
  #         aes-key-value: ${DB_ENCRYPT_KEY}
  #   tables:
  #     t_account_balance:
  #       columns:
  #         free:
  #           cipherColumn: free
  #           encryptorName: aes_encryptor

props:
  sql-show: false                      # 生产关闭 SQL 日志
  sql-simple: true                     # 简单模式，减少性能损耗
  check-table-metadata-enabled: true   # 启动时校验表元数据一致性
  max-connections-size-per-query: 1    # 每个查询的最大连接数
  kernel-executor-size: 20             # 内部执行器线程数
```

### 1.4 实际数据源配置

```yaml
# 主从库真实连接配置（通过 spring.shardingsphere 前缀或外部化）

spring:
  shardingsphere:
    datasource:
      names: primary_ds,replica_ds_0,replica_ds_1

      primary_ds:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://trading-db-master.internal:3306/trading?useSSL=false&serverTimezone=Asia/Shanghai
        username: ${DB_USER}
        password: ${DB_PASS}
        max-lifetime: 1800000
        idle-timeout: 600000
        connection-timeout: 30000

      replica_ds_0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://trading-db-slave-a.internal:3306/trading?useSSL=false&serverTimezone=Asia/Shanghai
        username: ${DB_USER_REPLICA}
        password: ${DB_PASS_REPLICA}
        max-lifetime: 1800000
        idle-timeout: 600000
        connection-timeout: 30000

      replica_ds_1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://trading-db-slave-b.internal:3306/trading?useSSL=false&serverTimezone=Asia/Shanghai
        username: ${DB_USER_REPLICA}
        password: ${DB_PASS_REPLICA}
        max-lifetime: 1800000
        idle-timeout: 600000
        connection-timeout: 30000
```

---

## 2. 读写分离路由注解

### 2.1 自定义注解

```java
import java.lang.annotation.*;

/**
 * 强制路由到主库（写操作 + 强一致性读）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Master {
}

/**
 * 强制路由到从库（容忍最终一致性的读操作）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Slave {
}
```

### 2.2 AOP 实现路由切换

```java
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReadWriteRoutingAspect {

    /**
     * @Master 注解: 强制写主库
     */
    @Around("@annotation(master)")
    public Object routeToMaster(ProceedingJoinPoint pjp, Master master) throws Throwable {
        try (HintManager hintManager = HintManager.getInstance()) {
            hintManager.setWriteRouteOnly();  // 强制写主库
            return pjp.proceed();
        }
    }

    /**
     * @Slave 注解: 强制读从库（默认 ShardingSphere 也自动读从库，
     *           此注解主要用于显式标注 + 文档化目的）
     */
    @Around("@annotation(slave)")
    public Object routeToSlave(ProceedingJoinPoint pjp, Slave slave) throws Throwable {
        try (HintManager hintManager = HintManager.getInstance()) {
            hintManager.setReadwriteSplittingHint(true);  // 强制读从库
            return pjp.proceed();
        }
    }
}
```

### 2.3 使用示例

```java
@Service
public class OrderQueryService {

    private final OrderMapper orderMapper;

    @Slave   // 读从库——适用于用户历史订单查询
    public PageResult<Order> queryHistory(Long userId, String symbol, Long cursor, int limit) {
        return orderMapper.findByCursor(userId, symbol, cursor, limit);
    }

    @Master  // 写主库——下单操作
    @Transactional
    public Order placeOrder(PlaceOrderRequest request) {
        Order order = Order.create(request);
        orderMapper.insert(order);
        return order;
    }
}

@Service
public class MarketDataService {

    private final MarketDataMapper marketDataMapper;

    @Slave   // 读从库——行情数据可以容忍秒级延迟
    public MarketData getLatestTicker(String symbol) {
        return marketDataMapper.findLatestBySymbol(symbol);
    }
}

@Service
public class FundService {

    @Master  // 写主库——资金变动必须在主库
    @Transactional
    public void freezeBalance(Long userId, String asset, BigDecimal amount) {
        accountMapper.freezeBalance(userId, asset, amount);
        fundFlowMapper.insert(...);
    }
}
```

---

## 3. 从库延迟监控与自动切主

### 3.1 MySQL 延迟监控

```sql
-- 在主库执行，查看所有从库的延迟
SHOW SLAVE HOSTS;

-- 在从库执行
SHOW SLAVE STATUS\G
-- 关注: Seconds_Behind_Master
```

### 3.2 Prometheus 采集 MySQL 延迟

```yaml
# mysqld_exporter 采集
scrape_configs:
  - job_name: 'mysql-replica'
    static_configs:
      - targets:
          - 'trading-db-slave-a.internal:9104'
          - 'trading-db-slave-b.internal:9104'
        labels:
          role: replica

# Grafana 告警规则
# - alert: ReplicationLagHigh
#   expr: mysql_slave_status_seconds_behind_master > 5
#   for: 30s
#   labels:
#     severity: critical
```

### 3.3 延迟过高时自动切回主库

```java
@Component
public class ReplicationLagDetector {

    private final JdbcTemplate jdbcTemplate;  // 连接从库

    /**
     * 检测从库延迟是否超过阈值。
     * 延迟超过 5 秒时，打印告警日志。
     * 实际切换由 ShardingSphere 的负载均衡器自动处理：
     * 当从库不可用时，ShardingSphere 自动将请求转发到主库。
     * 我们只需要监控和告警。
     */
    @Scheduled(fixedRate = 10000)  // 每 10 秒检查一次
    public void checkReplicationLag() {
        try {
            Long lag = jdbcTemplate.queryForObject(
                    "SHOW SLAVE STATUS", (rs, rowNum) -> {
                        long seconds = rs.getLong("Seconds_Behind_Master");
                        return rs.wasNull() ? -1L : seconds;
                    });

            if (lag == null || lag < 0) {
                log.warn("无法获取从库延迟信息（可能不是从库实例）");
                return;
            }

            if (lag > 5) {
                log.error("从库延迟过高! Seconds_Behind_Master={}s, 超过阈值 5s", lag);
                // 发送告警到 PagerDuty / 钉钉
                alertService.sendAlert("REPLICATION_LAG",
                        String.format("从库延迟 %d 秒", lag));
            } else if (lag > 2) {
                log.warn("从库延迟上升: {}s", lag);
            }
        } catch (Exception e) {
            log.error("检查从库延迟失败", e);
        }
    }

    /**
     * 手动降级开关: 延迟过高时将读流量全部切回主库。
     * 通过 Zookeeper/ConfigMap 控制:
     *   flag: force-read-master = true
     */
    @Value("${db.force-read-master:false}")
    private boolean forceReadMaster;

    /**
     * 降级函数: 当 forceReadMaster=true 时，
     * 读写全部走主库，不再使用从库负载均衡
     */
    public <T> T readWithFallback(Supplier<T> slaveOp, Supplier<T> masterOp) {
        if (forceReadMaster) {
            return masterOp.get();
        }
        try {
            return slaveOp.get();
        } catch (Exception e) {
            log.warn("从库读取失败，降级到主库", e);
            return masterOp.get();
        }
    }
}
```

---

## 4. 后台报表查询专用路由

### 4.1 @Hint 强制路由

```java
@Service
public class ReportQueryService {

    /**
     * 后台管理报表: 低频率、大查询、容忍高延迟。
     * 通过 @Hint 强制路由到从库，避免影响在线交易。
     */
    public List<TransactionReport> generateDailyReport(Long date) {
        try (HintManager hintManager = HintManager.getInstance()) {
            // 强制路由到从库
            hintManager.setReadwriteSplittingHint(true);
            // 可选: 指定特定从库
            // hintManager.setDataSourceName("replica_ds_0");

            return transactionMapper.queryDailyReport(date);
        }
    }

    /**
     * 风控批量计算: 读从库，避免对主库造成压力
     */
    public List<RiskReport> calculateRiskSummary(Set<String> symbols) {
        try (HintManager hintManager = HintManager.getInstance()) {
            hintManager.setReadwriteSplittingHint(true);
            return positionMapper.summarizeBySymbols(symbols);
        }
    }
}
```

### 4.2 压测验证

```sql
-- 验证读请求是否确实路由到从库 (需要开启 sql-show)
-- 在 sharding-readwrite.yaml 中设置 props.sql-show: true
-- 日志中会出现:
-- [ShardingSphere-SQL] Logic SQL: SELECT * FROM t_order WHERE user_id = ?
-- [ShardingSphere-SQL] Actual SQL: replica_ds_0 ::: SELECT * FROM t_order WHERE user_id = ?
```

---

## 5. 各微服务读写分离策略

| 微服务 | 主库 | 从库数 | 路由策略 | 说明 |
|--------|:---:|:------:|----------|------|
| **order-service** | 1 | 2 | 写主读从 | 订单查询（历史 + 状态轮询）走从库，下单/撤单走主库 |
| **fund-service** | 1 | 2 | 写主读从 | 资金变动必须主库，流水查询可走从库 |
| **market-service** | 1 | 3 | 全从库 | 行情纯读，可全部走从库，但写入用 Redis 更合适 |
| **risk-service** | 1 | 1 | 写主读从 | 风控查询走从库，风控冻结操作走主库 |
| **matching-service** | — | — | 不适用 | 撮合引擎数据在内存/Redis，DB 仅落盘成交记录 |
