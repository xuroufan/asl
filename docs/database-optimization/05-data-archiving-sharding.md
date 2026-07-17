# 数据归档与分表策略

> 目标表: t_order（订单）、t_fund_flow（资金流水）、t_trade（成交记录）
> 数据量预估: 单表日增 50 万 ~ 200 万行（取决于用户活跃度）

---

## 1. 分表策略

### 1.1 按日期分表（推荐）

最适合期货交易场景：订单和资金流水天然按时间维度查询。

```sql
-- 订单表按月分表: t_order_202607, t_order_202608 ...
-- 资金流水按月分表: t_fund_flow_202607, t_fund_flow_202608 ...

-- 每月1日自动建表脚本
-- 调用: CALL create_monthly_table('t_order');
-- 调用: CALL create_monthly_table('t_fund_flow');
-- 调用: CALL create_monthly_table('t_trade');

DELIMITER $$

CREATE PROCEDURE create_monthly_table(IN base_name VARCHAR(64))
BEGIN
    SET @suffix = DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y%m');
    SET @table_name = CONCAT(base_name, '_', @suffix);
    SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @table_name,
        ' LIKE ', base_name, '_template');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END$$

DELIMITER ;
```

### 1.2 模板表定义

```sql
-- 使用模板表，每月从模板 CREATE LIKE

CREATE TABLE t_order_template (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      VARCHAR(64)  NOT NULL COMMENT '全局订单号',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    symbol        VARCHAR(32)  NOT NULL COMMENT '合约代码',
    type          VARCHAR(16)  NOT NULL COMMENT 'MARKET/LIMIT/STOP/STOP_LIMIT',
    side          VARCHAR(8)   NOT NULL COMMENT 'BUY/SELL',
    status        VARCHAR(16)  NOT NULL COMMENT 'PENDING/PARTIALLY_FILLED/...',
    price         DECIMAL(24,8) DEFAULT NULL,
    stop_price    DECIMAL(24,8) DEFAULT NULL,
    quantity      DECIMAL(24,8) NOT NULL,
    filled_qty    DECIMAL(24,8) NOT NULL DEFAULT 0,
    avg_price     DECIMAL(24,8) DEFAULT NULL,
    total_amount  DECIMAL(24,8) NOT NULL,
    fee           DECIMAL(24,8) NOT NULL DEFAULT 0,
    created_at    BIGINT       NOT NULL,
    updated_at    BIGINT       NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_symbol (symbol),
    INDEX idx_user_status_created (user_id, status, created_at DESC),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表模板';

CREATE TABLE t_fund_flow_template (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    flow_id       VARCHAR(64)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    asset         VARCHAR(16)  NOT NULL,
    amount        DECIMAL(24,8) NOT NULL,
    fee           DECIMAL(24,8) NOT NULL DEFAULT 0,
    flow_type     VARCHAR(16)  NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    description   VARCHAR(256) DEFAULT NULL,
    created_at    BIGINT       NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表模板';

CREATE TABLE t_trade_template (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_id      VARCHAR(64)  NOT NULL,
    order_id      VARCHAR(64)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    symbol        VARCHAR(32)  NOT NULL,
    side          VARCHAR(8)   NOT NULL,
    price         DECIMAL(24,8) NOT NULL,
    quantity      DECIMAL(24,8) NOT NULL,
    total_amount  DECIMAL(24,8) NOT NULL,
    fee           DECIMAL(24,8) NOT NULL DEFAULT 0,
    fee_currency  VARCHAR(16)  NOT NULL DEFAULT 'USDT',
    executed_at   BIGINT       NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_symbol (symbol),
    INDEX idx_executed_at (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成交记录表模板';
```

### 1.3 ShardingSphere 分表配置

```yaml
# order-service 分表配置
spring:
  shardingsphere:
    sharding:
      tables:
        t_order:
          actual-data-nodes: order-ds.t_order_2026$->{07..12}
          table-strategy:
            standard:
              sharding-column: created_at
              precise-algorithm-class-name: com.hackfuture.sharding.MonthShardingAlgorithm
              range-algorithm-class-name: com.hackfuture.sharding.MonthShardingAlgorithm
        t_trade:
          actual-data-nodes: order-ds.t_trade_2026$->{07..12}
          table-strategy:
            standard:
              sharding-column: executed_at
              precise-algorithm-class-name: com.hackfuture.sharding.MonthShardingAlgorithm

---
# fund-service 分表配置
spring:
  shardingsphere:
    sharding:
      tables:
        t_fund_flow:
          actual-data-nodes: fund-ds.t_fund_flow_2026$->{07..12}
          table-strategy:
            standard:
              sharding-column: created_at
              precise-algorithm-class-name: com.hackfuture.sharding.MonthShardingAlgorithm
```

### 1.4 月份分片算法

```java
package com.hackfuture.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 按月份分片算法
 * 输入: created_at (毫秒时间戳)
 * 输出: 表后缀 _202607
 */
public class MonthShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public String doSharding(Collection<String> availableTargetNames,
                             PreciseShardingValue<Long> shardingValue) {
        Long timestamp = shardingValue.getValue();
        String suffix = toMonthSuffix(timestamp);

        String target = shardingValue.getLogicTableName() + "_" + suffix;
        if (availableTargetNames.contains(target)) {
            return target;
        }
        throw new IllegalArgumentException("分片表不存在: " + target);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Long> shardingValue) {
        Long lower = shardingValue.getValueRange().lowerEndpoint();
        Long upper = shardingValue.getValueRange().upperEndpoint();
        String lowerMonth = toMonthSuffix(lower);
        String upperMonth = toMonthSuffix(upper);
        String tableName = shardingValue.getLogicTableName();

        Collection<String> result = new LinkedHashSet<>();
        for (String target : availableTargetNames) {
            if (target.compareTo(tableName + "_" + lowerMonth) >= 0
                    && target.compareTo(tableName + "_" + upperMonth) <= 0) {
                result.add(target);
            }
        }
        return result;
    }

    private String toMonthSuffix(Long timestampMs) {
        LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMs), ZoneId.of("Asia/Shanghai"));
        return dt.format(FMT);
    }

    @Override
    public Properties getProps() { return null; }

    @Override
    public void init(Properties props) { }
}
```

---

## 2. 数据归档策略

### 2.1 归档规则

| 数据 | 在线保留 | 归档策略 | 删除策略 |
|------|:--------:|----------|----------|
| t_order（已成交） | 3 个月 | 迁入 t_order_archive | 迁后删除 |
| t_order（未成交） | 永久在线 | 一直保留 | 不移除 |
| t_fund_flow | 6 个月 | 迁入 t_fund_flow_archive | 迁后删除 |
| t_trade | 6 个月 | 迁入 t_trade_archive | 迁后删除 |
| t_market_data | 7 天 | 不归档，直接清理 | 清理 |
| t_candle_data | 1 年 | 迁入 t_candle_archive | 迁后删除 |

### 2.2 归档库表结构

```sql
-- 历史库 (trading_archive)

CREATE TABLE t_order_archive LIKE t_order_template;
ALTER TABLE t_order_archive ADD INDEX idx_archived_at (archived_at);

CREATE TABLE t_fund_flow_archive LIKE t_fund_flow_template;
ALTER TABLE t_fund_flow_archive ADD INDEX idx_archived_at (archived_at);

CREATE TABLE t_trade_archive LIKE t_trade_template;
ALTER TABLE t_trade_archive ADD INDEX idx_archived_at (archived_at);
```

---

## 3. 归档脚本

### 3.1 归档工具类

```java
@Component
@Slf4j
public class DataArchiver {

    private final JdbcTemplate onlineJdbc;    // 主库
    private final JdbcTemplate archiveJdbc;   // 归档库
    private final TransactionTemplate txTemplate;

    /**
     * 归档指定时间前的数据
     *
     * @param tableName    表名
     * @param archiveTable 归档表名
     * @param timeColumn   时间字段 (created_at / executed_at)
     * @param beforeMs     截止时间戳
     * @param batchSize    每批处理条数
     * @return 归档总行数
     */
    public long archiveOldData(String tableName, String archiveTable,
                               String timeColumn, long beforeMs, int batchSize) {
        long total = 0;
        boolean hasMore = true;

        while (hasMore) {
            // 1. 查询需要归档的数据
            List<Map<String, Object>> batch = onlineJdbc.queryForList(
                    "SELECT * FROM " + tableName +
                    " WHERE " + timeColumn + " < ? AND status = 'FILLED'" +
                    " LIMIT ?", beforeMs, batchSize);

            if (batch.isEmpty()) {
                hasMore = false;
                break;
            }

            // 2. 逐批事务：插入归档 + 删除在线
            txTemplate.execute(status -> {
                // 插入归档
                archiveBatch(archiveTable, batch);

                // 删除在线
                List<Long> ids = batch.stream()
                        .map(r -> (Long) r.get("id"))
                        .collect(Collectors.toList());
                onlineJdbc.update("DELETE FROM " + tableName + " WHERE id IN (" +
                        ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");

                log.info("归档 {}: {} 条 (beforeMs={})", tableName, batch.size(), beforeMs);
                return null;
            });

            total += batch.size();

            if (batch.size() < batchSize) {
                hasMore = false;
            }
        }

        log.info("归档完成: {} -> {}, 共 {} 条", tableName, archiveTable, total);
        return total;
    }

    private void archiveBatch(String archiveTable, List<Map<String, Object>> rows) {
        // 构建 INSERT IGNORE 语句（防重复归档）
        // 此处省略具体实现，可使用 Spring Batch 或直接 JDBC batch insert
    }
}
```

### 3.2 定时任务

```java
@Component
@Slf4j
public class ArchiveScheduler {

    private final DataArchiver archiver;

    // 每天凌晨 3:00 执行归档
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyArchive() {
        long now = System.currentTimeMillis();
        long threeMonthsAgo = now - 90L * 24 * 60 * 60 * 1000;
        long sixMonthsAgo  = now - 180L * 24 * 60 * 60 * 1000;

        // 归档已成交订单（3 个月）
        archiver.archiveOldData("t_order", "t_order_archive",
                "created_at", threeMonthsAgo, 500);

        // 归档资金流水（6 个月）
        archiver.archiveOldData("t_fund_flow", "t_fund_flow_archive",
                "created_at", sixMonthsAgo, 500);

        // 归档成交记录（6 个月）
        archiver.archiveOldData("t_trade", "t_trade_archive",
                "executed_at", sixMonthsAgo, 500);
    }

    // 每周日凌晨 4:00 清理历史行情快照
    @Scheduled(cron = "0 0 4 * * SUN")
    public void cleanMarketData() {
        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        int deleted = marketDataMapper.deleteOlderThan(sevenDaysAgo);
        log.info("清理行情快照: {} 条", deleted);
    }
}
```

### 3.3 分月建表定时任务

```java
@Component
public class MonthlyTableCreator {

    // 每月 28 日提前创建下月分表
    @Scheduled(cron = "0 0 2 28 * ?")
    public void createNextMonthTables() {
        String nextMonthSuffix = LocalDateTime.now()
                .plusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyyMM"));

        for (String base : List.of("t_order", "t_fund_flow", "t_trade")) {
            String tableName = base + "_" + nextMonthSuffix;
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS " + tableName +
                    " LIKE " + base + "_template");
            log.info("预创建分表: {}", tableName);
        }
    }
}
```

---

## 4. 数据生命周期总览

```
                    ┌─────────────────────┐
                    │   在线交易表 (MySQL)  │
                    │   t_order_202607     │  ← 本月数据
                    │   t_fund_flow_202607 │
                    │   t_trade_202607     │
                    └─────────┬───────────┘
                              │ 定时归档（每日 3:00）
                              ▼
                    ┌─────────────────────┐
                    │   归档库 (MySQL)      │
                    │   t_order_archive     │  ← 历史数据
                    │   t_fund_flow_archive │
                    │   t_trade_archive     │
                    └─────────┬───────────┘
                              │ 超过 2 年的冷数据
                              ▼
                    ┌─────────────────────┐
                    │   冷存储 (OSS/S3)     │  ← 压缩后长期保存
                    │   archive_2026Q1.parquet │
                    └─────────────────────┘
```

---

## 5. 分表 + 归档速查

| 操作 | 频率 | 工具 |
|------|:----:|------|
| 预创建下月分表 | 每月 28 日 | `CREATE TABLE ... LIKE` |
| 已成交订单归档 | 每日 3:00 | `INSERT + DELETE` 批处理 |
| 资金流水归档 | 每日 3:00 | `INSERT + DELETE` 批处理 |
| 行情快照清理 | 每周日 | `DELETE WHERE ts < ?` |
| 冷数据导出 | 每季度 | mysqldump / parquet 导出到 S3 |
