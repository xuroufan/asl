# 慢 SQL 治理与索引优化

> 适用：order-service / matching-service / fund-service / risk-service / market-service
> 推导自客户端 API 调用模式，覆盖完整的期货交易后端表结构

---

## 1. 后端表结构定义

基于 Android 端 entity 和 API 端点推导的后端 MySQL 表模型：

```sql
-- ==================== 订单服务 ====================
CREATE TABLE t_order (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      VARCHAR(64)  NOT NULL COMMENT '全局订单号',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    symbol        VARCHAR(32)  NOT NULL COMMENT '合约代码',
    type          VARCHAR(16)  NOT NULL COMMENT 'MARKET/LIMIT/STOP/STOP_LIMIT',
    side          VARCHAR(8)   NOT NULL COMMENT 'BUY/SELL',
    status        VARCHAR(16)  NOT NULL COMMENT 'PENDING/PARTIALLY_FILLED/FILLED/CANCELLED/REJECTED/EXPIRED',
    price         DECIMAL(24,8) DEFAULT NULL COMMENT '委托价格',
    stop_price    DECIMAL(24,8) DEFAULT NULL COMMENT '触发价格',
    quantity      DECIMAL(24,8) NOT NULL COMMENT '委托数量',
    filled_qty    DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '已成交数量',
    avg_price     DECIMAL(24,8) DEFAULT NULL COMMENT '成交均价',
    total_amount  DECIMAL(24,8) NOT NULL COMMENT '总金额',
    fee           DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '手续费',
    created_at    BIGINT       NOT NULL COMMENT '创建时间戳',
    updated_at    BIGINT       NOT NULL COMMENT '更新时间戳',
    INDEX idx_user_id (user_id),
    INDEX idx_symbol (symbol),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ==================== 成交记录 ====================
CREATE TABLE t_trade (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_id      VARCHAR(64)  NOT NULL COMMENT '全局成交ID',
    order_id      VARCHAR(64)  NOT NULL COMMENT '关联订单号',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    symbol        VARCHAR(32)  NOT NULL COMMENT '合约代码',
    side          VARCHAR(8)   NOT NULL COMMENT 'BUY/SELL',
    price         DECIMAL(24,8) NOT NULL COMMENT '成交价',
    quantity      DECIMAL(24,8) NOT NULL COMMENT '成交量',
    total_amount  DECIMAL(24,8) NOT NULL COMMENT '成交额',
    fee           DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '手续费',
    fee_currency  VARCHAR(16)  NOT NULL DEFAULT 'USDT' COMMENT '手续费币种',
    executed_at   BIGINT       NOT NULL COMMENT '成交时间',
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_symbol (symbol),
    INDEX idx_executed_at (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成交记录表';

-- ==================== 持仓 ====================
CREATE TABLE t_position (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    position_id    VARCHAR(64)  NOT NULL COMMENT '持仓ID',
    user_id        BIGINT       NOT NULL COMMENT '用户ID',
    symbol         VARCHAR(32)  NOT NULL COMMENT '合约代码',
    side           VARCHAR(8)   NOT NULL COMMENT 'LONG/SHORT',
    quantity       DECIMAL(24,8) NOT NULL COMMENT '持仓数量',
    entry_price    DECIMAL(24,8) NOT NULL COMMENT '开仓均价',
    current_price  DECIMAL(24,8) NOT NULL COMMENT '当前价格',
    unrealized_pnl DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '未实现盈亏',
    realized_pnl   DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '已实现盈亏',
    margin_used    DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '占用保证金',
    leverage       INT          NOT NULL DEFAULT 1 COMMENT '杠杆倍数',
    liquidation_price DECIMAL(24,8) DEFAULT NULL COMMENT '强平价格',
    opened_at      BIGINT       NOT NULL COMMENT '开仓时间',
    updated_at     BIGINT       NOT NULL COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_symbol (symbol),
    INDEX idx_user_symbol (user_id, symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持仓表';

-- ==================== 行情数据 ====================
CREATE TABLE t_market_data (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol        VARCHAR(32)  NOT NULL COMMENT '合约代码',
    name          VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '合约名称',
    exchange      VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '交易所',
    price         DECIMAL(24,8) NOT NULL COMMENT '最新价',
    change_val    DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '涨跌额',
    change_pct    DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '涨跌幅',
    high          DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '最高价',
    low           DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '最低价',
    open_val      DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '开盘价',
    close_val     DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '昨收盘',
    volume        BIGINT       NOT NULL DEFAULT 0 COMMENT '成交量',
    turnover      DECIMAL(24,2) NOT NULL DEFAULT 0 COMMENT '成交额',
    open_interest BIGINT       NOT NULL DEFAULT 0 COMMENT '持仓量',
    ts            BIGINT       NOT NULL COMMENT '时间戳',
    UNIQUE KEY uk_symbol_ts (symbol, ts),
    INDEX idx_symbol (symbol),
    INDEX idx_ts (ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行情快照表';

-- ==================== K线数据 ====================
CREATE TABLE t_candle_data (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol        VARCHAR(32)  NOT NULL COMMENT '合约代码',
    candle_interval VARCHAR(8) NOT NULL COMMENT 'M1/M5/M15/M30/H1/H4/D1/W1',
    open_val      DECIMAL(24,8) NOT NULL COMMENT '开盘价',
    high          DECIMAL(24,8) NOT NULL COMMENT '最高价',
    low           DECIMAL(24,8) NOT NULL COMMENT '最低价',
    close_val     DECIMAL(24,8) NOT NULL COMMENT '收盘价',
    volume        BIGINT       NOT NULL COMMENT '成交量',
    ts            BIGINT       NOT NULL COMMENT '时间戳',
    UNIQUE KEY uk_symbol_int_ts (symbol, candle_interval, ts),
    INDEX idx_symbol (symbol),
    INDEX idx_ts (ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='K线数据表';

-- ==================== 资金流水 ====================
CREATE TABLE t_fund_flow (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    flow_id       VARCHAR(64)  NOT NULL COMMENT '流水号',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    asset         VARCHAR(16)  NOT NULL COMMENT '币种',
    amount        DECIMAL(24,8) NOT NULL COMMENT '变动金额',
    fee           DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '手续费',
    flow_type     VARCHAR(16)  NOT NULL COMMENT 'DEPOSIT/WITHDRAWAL/TRADE_FEE/TRANSFER_IN/TRANSFER_OUT',
    status        VARCHAR(16)  NOT NULL COMMENT 'PENDING/COMPLETED/FAILED/CANCELLED',
    description   VARCHAR(256) DEFAULT NULL COMMENT '描述',
    created_at    BIGINT       NOT NULL COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_flow_type (flow_type),
    INDEX idx_created_at (created_at),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表';

-- ==================== 账户余额 ====================
CREATE TABLE t_account_balance (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    asset         VARCHAR(16)  NOT NULL COMMENT '币种',
    free          DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '可用余额',
    locked        DECIMAL(24,8) NOT NULL DEFAULT 0 COMMENT '冻结余额',
    total         DECIMAL(24,8) AS (free + locked) STORED COMMENT '总余额',
    updated_at    BIGINT       NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_user_asset (user_id, asset)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户余额表';

-- ==================== 用户表 ====================
CREATE TABLE t_user (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(64)  NOT NULL COMMENT '用户名',
    display_name   VARCHAR(128) NOT NULL DEFAULT '' COMMENT '显示名',
    email          VARCHAR(128) NOT NULL COMMENT '邮箱',
    phone          VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    avatar_url     VARCHAR(256) DEFAULT NULL COMMENT '头像',
    account_status VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SUSPENDED/FROZEN/CLOSED',
    kyc_level      VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/BASIC/ADVANCED',
    created_at     BIGINT       NOT NULL COMMENT '创建时间',
    updated_at     BIGINT       NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

---

## 2. 慢 SQL 场景分析与索引优化

### 2.1 用户订单历史查询（高频）

**客户端调用**: `GET /api/order/history?symbol=xxx&page=1&size=20`

```sql
-- 风险: 全表扫描 + 深分页
SELECT * FROM t_order
WHERE user_id = ?
  AND symbol = ?
ORDER BY created_at DESC
LIMIT 20 OFFSET 10000;
```

**问题分析**:
- `WHERE` 条件只有 `user_id` + `symbol`，现有索引 `idx_user_id` 只覆盖 user_id
- `ORDER BY created_at DESC` 需要 filesort
- `OFFSET 10000` 意味着 MySQL 读取了 10020 行后丢弃前 10000 行

**优化方案 A：复合索引**

```sql
-- 覆盖 user_id + symbol 筛选 + created_at 排序
ALTER TABLE t_order ADD INDEX idx_user_symbol_created (
    user_id, symbol, created_at DESC
);

-- 覆盖仅 user_id 查询（常见于"查全部"）
ALTER TABLE t_order ADD INDEX idx_user_created (
    user_id, created_at DESC
);
```

**优化方案 B：游标查询（推荐，替代深分页）**

```java
// 修复前: LIMIT/OFFSET 深分页
@Query("SELECT * FROM t_order WHERE user_id = :userId AND symbol = :symbol " +
       "ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
List<Order> findByUserIdAndSymbolWithOffset(@Param("userId") Long userId,
                                              @Param("symbol") String symbol,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

// 修复后: 游标查询（Seek Method）
@Query("SELECT * FROM t_order WHERE user_id = :userId AND symbol = :symbol " +
       "AND (:cursor IS NULL OR created_at < :cursor) " +
       "ORDER BY created_at DESC LIMIT :limit")
List<Order> findByUserIdAndSymbolWithCursor(@Param("userId") Long userId,
                                             @Param("symbol") String symbol,
                                             @Param("cursor") Long cursor,
                                             @Param("limit") int limit);

// 客户端: 用最后一条记录的 created_at 作为下一页的 cursor
```

### 2.2 活跃订单轮询（高频）

**业务场景**: 客户端每隔数秒拉取 PENDING/PARTIALLY_FILLED 状态的未完成订单

```sql
-- 风险: idx_status 选择性差（状态只有 6 种，大量行共享同一个 status）
SELECT * FROM t_order
WHERE user_id = ?
  AND status IN ('PENDING', 'PARTIALLY_FILLED')
ORDER BY created_at DESC;
```

**优化**:

```sql
-- 复合索引让 status 筛选后直接在索引中完成排序
ALTER TABLE t_order ADD INDEX idx_user_status_created (
    user_id, status, created_at DESC
);
```

### 2.3 行情快照查询（极高频）

**客户端调用**: `GET /api/market/ticker?symbol=xxx`（所有 ticker）/ `GET /api/market/ticker`

```sql
-- 查单个合约最新行情
SELECT * FROM t_market_data
WHERE symbol = ?
ORDER BY ts DESC LIMIT 1;

-- 查所有合约最新行情（全表扫描风险!）
SELECT md.*
FROM t_market_data md
INNER JOIN (
    SELECT symbol, MAX(ts) AS max_ts
    FROM t_market_data
    GROUP BY symbol
) latest ON md.symbol = latest.symbol AND md.ts = latest.max_ts;
```

**优化**:

```sql
-- 全覆盖索引 (Covering Index)
ALTER TABLE t_market_data ADD INDEX idx_symbol_ts (symbol, ts DESC);

-- 高频全表行情快照改用 Redis Hash 缓存，不查 MySQL
-- 推荐: Redis HSET market:ticker {symbol -> price_json}
-- MySQL 仅做历史归档
```

### 2.4 K 线查询（高频）

**客户端调用**: `GET /api/market/candles?symbol=xxx&interval=M5&limit=100`

```sql
-- 已有 UNIQUE KEY uk_symbol_int_ts (symbol, candle_interval, ts)
-- 查询走索引，无需进一步优化

EXPLAIN SELECT * FROM t_candle_data
WHERE symbol = 'rb2501.SHFE'
  AND candle_interval = 'M5'
  AND ts >= 1721164800000
ORDER BY ts ASC LIMIT 100;
-- 预期: type=range, key=uk_symbol_int_ts, rows≈100
```

### 2.5 资金流水查询（中频）

**客户端调用**: `GET /api/account/transactions?page=1&size=20`

```sql
-- 风险: 深分页
SELECT * FROM t_fund_flow
WHERE user_id = ?
ORDER BY created_at DESC
LIMIT 20 OFFSET 5000;
```

**优化**:

```sql
-- 覆盖索引，避免回表
ALTER TABLE t_fund_flow ADD INDEX idx_user_created_type (
    user_id, created_at DESC, flow_type
);

-- 改为游标查询
```

### 2.6 订单簿查询（高频）

**客户端调用**: `GET /api/market/orderbook?symbol=xxx&depth=10`

通常由 Redis 或内存直接提供服务，MySQL 仅做持久化备份。查询使用 `symbol` 主键即可。

### 2.7 持仓查询（中频）

**客户端调用**: `GET /api/position/list?symbol=xxx`

```sql
SELECT * FROM t_position
WHERE user_id = ? AND symbol = ?;
```

已有 `idx_user_symbol`，无需额外优化。

### 2.8 成交记录查询（中频）

```sql
-- 风险: 无 user_id + symbol 复合索引
SELECT * FROM t_trade
WHERE user_id = ? AND symbol = ?
ORDER BY executed_at DESC
LIMIT 20;
```

```sql
ALTER TABLE t_trade ADD INDEX idx_user_symbol_executed (
    user_id, symbol, executed_at DESC
);
```

---

## 3. 复合索引优化 DDL 汇总

### 3.1 order-service

```sql
-- 订单查询: 用户+合约+时间排序
ALTER TABLE t_order ADD INDEX idx_user_symbol_created (user_id, symbol, created_at DESC);
-- 订单查询: 用户+时间排序
ALTER TABLE t_order ADD INDEX idx_user_created (user_id, created_at DESC);
-- 活跃订单: 用户+状态+时间排序
ALTER TABLE t_order ADD INDEX idx_user_status_created (user_id, status, created_at DESC);
-- 撤销原单列索引
ALTER TABLE t_order DROP INDEX idx_user_id;
ALTER TABLE t_order DROP INDEX idx_created_at;
```

### 3.2 matching-service

```sql
-- 成交记录: 用户+合约+时间
ALTER TABLE t_trade ADD INDEX idx_user_symbol_executed (user_id, symbol, executed_at DESC);
-- 成交记录: order_id 关联查询
ALTER TABLE t_trade ADD INDEX idx_order_id (order_id);
```

### 3.3 market-service

```sql
-- 行情快照: 合约+时间倒序
ALTER TABLE t_market_data ADD INDEX idx_symbol_ts (symbol, ts DESC);
-- K 线查询已有 uk_symbol_int_ts 联合唯一索引
```

### 3.4 fund-service

```sql
-- 资金流水: 用户+时间
ALTER TABLE t_fund_flow ADD INDEX idx_user_created_type (user_id, created_at DESC, flow_type);
```

### 3.5 risk-service

```sql
-- 用户风控状态
ALTER TABLE t_position ADD INDEX idx_user_symbol (user_id, symbol);
```

---

## 4. 深分页解决方案

### 4.1 游标查询（推荐）

```java
// Mapper 层
@Select("""
    SELECT * FROM t_order
    WHERE user_id = #{userId}
        AND symbol = #{symbol}
        AND (#{cursor} IS NULL OR created_at < #{cursor})
    ORDER BY created_at DESC
    LIMIT #{limit}
    """)
List<Order> findByCursor(@Param("userId") long userId,
                         @Param("symbol") String symbol,
                         @Param("cursor") Long cursor,
                         @Param("limit") int limit);

// Service 层
public PageResult<Order> queryOrders(Long userId, String symbol, Long cursor, int limit) {
    List<Order> orders = orderMapper.findByCursor(userId, symbol, cursor, limit + 1);
    boolean hasMore = orders.size() > limit;
    if (hasMore) orders.remove(orders.size() - 1);
    Long nextCursor = orders.isEmpty() ? null : orders.get(orders.size() - 1).getCreatedAt();
    return new PageResult<>(orders, hasMore, nextCursor);
}
```

### 4.2 延迟关联（适用于需要回表的深分页）

```sql
-- 延迟关联: 先在索引中定位 ID，再回表取完整数据
SELECT * FROM t_order
INNER JOIN (
    SELECT id FROM t_order
    WHERE user_id = 1001 AND symbol = 'rb2501.SHFE'
    ORDER BY created_at DESC
    LIMIT 20 OFFSET 10000
) AS tmp ON t_order.id = tmp.id
ORDER BY t_order.created_at DESC;
```

---

## 5. IN 子句过多（>500 元素）

**风险场景**: 行情批量查询时 `symbol IN (...)` 超过 500 个参数

**修复方案: 分批 + 临时表**

```java
// 修复前: IN 5000 个元素
List<MarketData> findBySymbols(List<String> symbols) {
    return marketDataMapper.findBySymbolIn(symbols);
}

// 修复后: 分批查询
public List<MarketData> findBySymbols(List<String> symbols) {
    List<MarketData> result = new ArrayList<>();
    List<List<String>> batches = Lists.partition(symbols, 200); // Guava
    for (List<String> batch : batches) {
        result.addAll(marketDataMapper.findBySymbolIn(batch));
    }
    return result;
}

// 或者: 建临时表 JOIN（SQL 方式）
-- CREATE TEMPORARY TABLE tmp_symbols (symbol VARCHAR(32) PRIMARY KEY);
-- INSERT INTO tmp_symbols VALUES (...);  -- 批量 INSERT
-- SELECT md.* FROM t_market_data md INNER JOIN tmp_symbols t ON md.symbol = t.symbol;
```

---

## 6. ORDER BY / GROUP BY 索引检查清单

| SQL 模式 | 索引要求 | 检查结果 |
|----------|----------|----------|
| `WHERE user_id=? ORDER BY created_at DESC` | 需要 `(user_id, created_at)` | `idx_user_created` 已覆盖 |
| `WHERE user_id=? AND status=? ORDER BY created_at DESC` | 需要 `(user_id, status, created_at)` | `idx_user_status_created` 已覆盖 |
| `WHERE symbol=? AND ts>=? ORDER BY ts ASC` | 需要 `(symbol, ts)` | `uk_symbol_ts` 已覆盖 |
| `WHERE symbol=? AND candle_interval=? ORDER BY ts ASC` | 需要 `(symbol, candle_interval, ts)` | `uk_symbol_int_ts` 已覆盖 |
| `GROUP BY symbol ORDER BY ts DESC` | 需要 `(symbol, ts DESC)` | `idx_symbol_ts` 已覆盖 |
