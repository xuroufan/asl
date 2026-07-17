# 分布式事务超时与重试（Seata）

> 适用场景: 期货交易平台跨服务事务
>   - 下单 -> 冻结资金（order-service -> fund-service）
>   - 成交 -> 更新持仓 -> 划转资金（matching-service -> fund-service -> position-service）
>   - 撤单 -> 解冻资金（order-service -> fund-service）
> 模式选择: TCC（资金类强一致性）+ Saga（持仓类最终一致性）

---

## 1. Seata 服务端部署

### 1.1 docker-compose

```yaml
# seata-server/docker-compose.yml
version: '3.8'
services:
  seata-server:
    image: seataio/seata-server:1.8.0
    ports:
      - "7091:7091"   # HTTP 控制台
      - "8091:8091"   # 服务端口
    environment:
      SEATA_IP: seata-server.internal
      SEATA_PORT: 8091
      STORE_MODE: db
    volumes:
      - ./resources:/seata-server/resources
```

### 1.2 Seata 服务端数据库初始化

```sql
-- seata-server 需要独立数据库存储全局事务日志
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARSET utf8mb4;

USE seata;

-- 全局事务表
CREATE TABLE IF NOT EXISTS `global_table` (
    `xid`                       VARCHAR(128) NOT NULL,
    `transaction_id`            BIGINT,
    `status`                    TINYINT      NOT NULL,
    `application_id`            VARCHAR(32),
    `transaction_service_group` VARCHAR(32),
    `transaction_name`          VARCHAR(128),
    `timeout`                   INT,
    `begin_time`                BIGINT,
    `application_data`          VARCHAR(2000),
    `gmt_create`                DATETIME,
    `gmt_modified`              DATETIME,
    PRIMARY KEY (`xid`),
    KEY `idx_status` (`status`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 分支事务表
CREATE TABLE IF NOT EXISTS `branch_table` (
    `branch_id`         BIGINT       NOT NULL,
    `xid`               VARCHAR(128) NOT NULL,
    `transaction_id`    BIGINT,
    `resource_group_id` VARCHAR(32),
    `resource_id`       VARCHAR(256),
    `branch_type`       VARCHAR(8),
    `status`            TINYINT,
    `client_id`         VARCHAR(64),
    `application_data`  VARCHAR(2000),
    `gmt_create`        DATETIME(6),
    `gmt_modified`      DATETIME(6),
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 锁表
CREATE TABLE IF NOT EXISTS `lock_table` (
    `row_key`        VARCHAR(128) NOT NULL,
    `xid`            VARCHAR(128),
    `transaction_id` BIGINT,
    `branch_id`      BIGINT,
    `resource_id`    VARCHAR(256),
    `table_name`     VARCHAR(32),
    `pk`             VARCHAR(36),
    `status`         TINYINT      NOT NULL DEFAULT '0',
    `gmt_create`     DATETIME,
    `gmt_modified`   DATETIME,
    PRIMARY KEY (`row_key`),
    KEY `idx_branch_id` (`branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

---

## 2. Seata 客户端配置

### 2.1 application.yml

```yaml
# order-service / fund-service / matching-service 共用模板

seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: trading-tx-group        # 事务分组，与服务端一致
  enable-auto-data-source-proxy: true       # 自动代理数据源
  data-source-proxy-mode: AT                # 使用 AT 模式

  # ---------- 服务发现与配置中心 ----------
  registry:
    type: nacos                             # 用 Nacos 注册 Seata Server
    nacos:
      server-addr: ${NACOS_ADDR:nacos.internal:8848}
      namespace: trading
      group: SEATA_GROUP
      application: seata-server

  config:
    type: nacos
    nacos:
      server-addr: ${NACOS_ADDR:nacos.internal:8848}
      namespace: trading
      group: SEATA_GROUP

  # ---------- TCC 超时 ----------
  tcc:
    timeout: 30000                          # TCC 阶段超时 30 秒
    enable-auto-recovery: true              # 自动恢复

  # ---------- 客户端参数 ----------
  client:
    rm:
      report-success-enable: true           # 上报成功事务
      table-meta-check-enable: false        # 关闭表元数据检查（提升性能）
      sql-parser-type: druid                # SQL 解析器
      lock:
        retry-interval: 1000                # 获取全局锁重试间隔 1 秒
        retry-times: 10                     # 全局锁最多重试 10 次
    tm:
      commit-retry-count: 3                 # 全局提交重试 3 次
      rollback-retry-count: 3               # 全局回滚重试 3 次
    undo:
      log-table: undo_log                   # 回滚日志表名
      data-validation: true                 # 校验回滚前后数据
      log-serialization: jackson            # 回滚日志序列化方式
    log:
      exception-rate: 100                   # 异常日志输出频率限制

  # ---------- 传输配置 ----------
  transport:
    type: TCP
    server: NIO
    heartbeat: true
    serialization: seata
    compressor: none
    enable-client-batch-send-request: true  # 批量发送请求
    shutdown:
      wait: 3
    thread-factory:
      boss-thread-prefix: NettyBoss
      worker-thread-prefix: NettyServerNIOWorker
      server-executor-thread-prefix: NettyServerBizHandler
      share-boss-worker: false
      client-selector-thread-prefix: NettyClientSelector
      client-selector-thread-size: 1
      client-worker-thread-prefix: NettyClientWorkerThread
```

---

## 3. 下单冻结资金 TCC 实现

### 3.1 TCC 接口定义

```java
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

/**
 * TCC 事务: 下单 -> 冻结资金
 *
 * Try:    冻结用户资金
 * Confirm: 扣减冻结金额（实际扣款）
 * Cancel:  解冻资金（退回）
 */
@LocalTCC
public interface FreezeBalanceTccAction {

    /**
     * Try 阶段: 预冻结资金
     *
     * @param userId  用户ID
     * @param asset   币种
     * @param amount  冻结金额
     * @param orderId 关联订单号（幂等控制用）
     */
    @TwoPhaseBusinessAction(
        name = "freezeBalanceTcc",
        commitMethod = "confirm",
        rollbackMethod = "cancel",
        useTCCFence = true,           // 开启 TCC Fence 防止悬挂
        timeout = 30000               // TCC 超时 30 秒
    )
    boolean tryFreeze(
        BusinessActionContext context,
        @BusinessActionContextParameter(paramName = "userId") Long userId,
        @BusinessActionContextParameter(paramName = "asset") String asset,
        @BusinessActionContextParameter(paramName = "amount") BigDecimal amount,
        @BusinessActionContextParameter(paramName = "orderId") String orderId
    );

    /**
     * Confirm 阶段: 确认冻结（将资金从 frozen 转入已用）
     * 注意: Confirm 必须 幂等（同一 xid 重复执行不产生副作用）
     */
    boolean confirm(BusinessActionContext context);

    /**
     * Cancel 阶段: 回滚冻结（解冻资金）
     * 注意: Cancel 必须 幂等
     */
    boolean cancel(BusinessActionContext context);
}
```

### 3.2 TCC 实现

```java
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreezeBalanceTccActionImpl implements FreezeBalanceTccAction {

    private final AccountBalanceMapper accountMapper;
    private final BalanceFreezeLogMapper freezeLogMapper;

    @Override
    @Transactional
    public boolean tryFreeze(BusinessActionContext context, Long userId,
                             String asset, BigDecimal amount, String orderId) {
        String xid = RootContext.getXID();
        log.info("TCC Try: freeze userId={}, asset={}, amount={}, xid={}",
                userId, asset, amount, xid);

        // 检查重复（幂等）
        if (freezeLogMapper.existsByXidAndPhase(xid, "TRY")) {
            log.info("Try 已执行，跳过重复请求 xid={}", xid);
            return true;
        }

        // 1. 冻结余额: UPDATE t_account_balance
        //    SET locked = locked + ?, free = free - ?
        int rows = accountMapper.freezeBalance(userId, asset, amount);
        if (rows == 0) {
            throw new InsufficientBalanceException(
                String.format("余额不足: userId=%d, asset=%s, need=%s", userId, asset, amount)
            );
        }

        // 2. 记录冻结日志
        freezeLogMapper.insert(new BalanceFreezeLog(xid, "TRY", userId, asset, amount, orderId));

        log.info("TCC Try 成功: xid={}", xid);
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(BusinessActionContext context) {
        String xid = context.getXid();
        log.info("TCC Confirm: xid={}", xid);

        // 幂等检查
        if (freezeLogMapper.existsByXidAndPhase(xid, "CONFIRM")) {
            log.info("Confirm 已执行，跳过重复 xid={}", xid);
            return true;
        }

        FreezeLog tryLog = freezeLogMapper.findByXidAndPhase(xid, "TRY");
        if (tryLog == null) {
            log.warn("Confirm 时找不到 Try 日志 xid={}", xid);
            return false;
        }

        // 将冻结金额转为已扣除
        // UPDATE t_account_balance SET locked = locked - ?
        accountMapper.deductFrozenBalance(tryLog.getUserId(), tryLog.getAsset(), tryLog.getAmount());
        freezeLogMapper.insert(new BalanceFreezeLog(xid, "CONFIRM", tryLog.getUserId(),
                tryLog.getAsset(), tryLog.getAmount(), tryLog.getOrderId()));
        log.info("TCC Confirm 成功: xid={}", xid);
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext context) {
        String xid = context.getXid();
        log.info("TCC Cancel: xid={}", xid);

        // 幂等检查
        if (freezeLogMapper.existsByXidAndPhase(xid, "CANCEL")) {
            log.info("Cancel 已执行，跳过重复 xid={}", xid);
            return true;
        }

        FreezeLog tryLog = freezeLogMapper.findByXidAndPhase(xid, "TRY");

        // 如果 Try 从未执行过（悬挂事务），直接记录 Cancel 并不操作余额
        if (tryLog == null) {
            log.warn("Cancel 时 Try 未执行（悬挂保护），直接记录 Cancel xid={}", xid);
            freezeLogMapper.insert(new BalanceFreezeLog(xid, "CANCEL", null, null,
                    BigDecimal.ZERO, null));
            return true;
        }

        // 解冻资金: UPDATE t_account_balance
        //    SET locked = locked - ?, free = free + ?
        accountMapper.unfreezeBalance(tryLog.getUserId(), tryLog.getAsset(), tryLog.getAmount());
        freezeLogMapper.insert(new BalanceFreezeLog(xid, "CANCEL", tryLog.getUserId(),
                tryLog.getAsset(), tryLog.getAmount(), tryLog.getOrderId()));
        log.info("TCC Cancel 成功: xid={}", xid);
        return true;
    }
}
```

### 3.3 TCC 幂等表 DDL

```sql
-- 用于 TCC 幂等控制
CREATE TABLE t_balance_freeze_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    xid        VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    phase      VARCHAR(16)  NOT NULL COMMENT 'TRY/CONFIRM/CANCEL',
    user_id    BIGINT       DEFAULT NULL COMMENT '用户ID',
    asset      VARCHAR(16)  DEFAULT NULL COMMENT '币种',
    amount     DECIMAL(24,8) DEFAULT NULL COMMENT '金额',
    order_id   VARCHAR(64)  DEFAULT NULL COMMENT '订单号',
    created_at BIGINT       NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_xid_phase (xid, phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TCC 事务日志表';

-- TCC Fence 表（Seata 内置，防止悬挂事务）
CREATE TABLE IF NOT EXISTS tcc_fence_log (
    xid              VARCHAR(128)  NOT NULL COMMENT '全局事务ID',
    branch_id        BIGINT        NOT NULL COMMENT '分支事务ID',
    action_name      VARCHAR(64)   NOT NULL COMMENT 'TCC 方法名',
    status           TINYINT       NOT NULL COMMENT '状态 0=TODO, 1=COMMITTED, 2=ROLLBACKED, 3=SUSPENDED',
    gmt_create       DATETIME(3)   NOT NULL COMMENT '创建时间',
    gmt_modified     DATETIME(3)   NOT NULL COMMENT '修改时间',
    UNIQUE KEY uk_xid_branch (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TCC Fence 日志';
```

---

## 4. TCC 重试机制

### 4.1 确认/取消阶段重试

```java
@Component
@Slf4j
public class TccRetryHandler {

    @Value("${seata.tcc.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${seata.tcc.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    /**
     * 带重试的 Confirm 执行
     *
     * @param retryable 业务逻辑
     * @param xid       全局事务ID 用于日志追踪
     */
    public <T> T executeWithRetry(Supplier<T> retryable, String xid, String phase) {
        Throwable lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = retryable.get();
                log.info("TCC {} 成功: xid={}, attempt={}", phase, xid, attempt);
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("TCC {} 失败(第{}次): xid={}, error={}",
                        phase, attempt, xid, e.getMessage());

                if (attempt < maxAttempts) {
                    // 指数退避: 1s -> 2s -> 4s
                    long backoff = initialIntervalMs * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 所有重试均失败：发送告警，人工介入
        log.error("TCC {} 所有重试均失败: xid={}, maxAttempts={}", phase, xid, maxAttempts);
        alertService.sendAlert("TCC_RETRY_EXHAUSTED",
                String.format("TCC %s xid=%s 重试%d次全部失败", phase, xid, maxAttempts));
        throw new TccRetryExhaustedException("TCC " + phase + " 重试耗尽", lastException);
    }
}
```

### 4.2 Seata 自动恢复机制

```yaml
# 在 seata-server 端启用自动恢复
recovery:
  committing-retry-period: 1000          # 提交重试周期 1 秒
  asyn-committing-retry-period: 1000      # 异步提交重试周期 1 秒
  rollbacking-retry-period: 1000          # 回滚重试周期 1 秒
  timeout-retry-period: 1000              # 超时重试周期 1 秒
  max-retry: 10                           # 最大重试次数
```

---

## 5. Saga 模式（持仓更新 -> 资金划转）

### 5.1 Saga 状态机配置

```yaml
# matching-service/src/main/resources/saga/trade-settlement.json
{
  "name": "trade-settlement",
  "startState": "UpdatePosition",
  "states": {
    "UpdatePosition": {
      "type": "ServiceTask",
      "serviceName": "positionService",
      "serviceMethod": "updatePosition",
      "compensateState": "CompensatePosition",
      "next": "TransferFunds",
      "retryCount": 3
    },
    "TransferFunds": {
      "type": "ServiceTask",
      "serviceName": "fundService",
      "serviceMethod": "transferForTrade",
      "compensateState": "CompensateFunds",
      "next": "Success",
      "retryCount": 3
    },
    "CompensatePosition": {
      "type": "ServiceTask",
      "serviceName": "positionService",
      "serviceMethod": "rollbackPosition",
      "isForUpdate": false
    },
    "CompensateFunds": {
      "type": "ServiceTask",
      "serviceName": "fundService",
      "serviceMethod": "rollbackTransfer",
      "isForUpdate": false
    },
    "Success": {
      "type": "SuccessState"
    }
  }
}
```

### 5.2 Saga 补偿逻辑

```java
@Service
@Slf4j
public class PositionService {

    /**
     * 正向操作: 更新持仓
     */
    public boolean updatePosition(String tradeId, Long userId,
                                  String symbol, String side, BigDecimal quantity,
                                  BigDecimal price) {
        log.info("Saga: 更新持仓 tradeId={}, userId={}, symbol={}", tradeId, userId, symbol);
        // 更新 t_position（增加持仓量、更新浮动盈亏等）
        // 记录操作快照，供补偿用
        saveSnapshot(tradeId, userId, symbol, side, quantity, price);
        return positionMapper.mergePosition(userId, symbol, side, quantity, price) > 0;
    }

    /**
     * 补偿操作: 回滚持仓（必须是对正向操作的精确逆反）
     */
    public boolean rollbackPosition(String tradeId) {
        PositionSnapshot snapshot = findSnapshot(tradeId);
        if (snapshot == null) {
            log.warn("回滚持仓时找不到快照: tradeId={}", tradeId);
            return false;
        }
        log.info("Saga 补偿: 回滚持仓 tradeId={}", tradeId);
        return positionMapper.reversePosition(snapshot) > 0;
    }

    /**
     * 正向操作快照
     */
    private void saveSnapshot(String tradeId, Long userId, String symbol,
                              String side, BigDecimal quantity, BigDecimal price) {
        // INSERT INTO t_saga_snapshot (trade_id, user_id, symbol, side, qty, price, created_at)
    }
}
```

---

## 6. 分布式事务超时与重试速查

| 配置项 | 推荐值 | 说明 |
|--------|:------:|------|
| `seata.tcc.timeout` | 30000 | TCC 阶段超时 30s |
| `seata.client.tm.commit-retry-count` | 3 | 全局提交重试 3 次 |
| `seata.client.tm.rollback-retry-count` | 3 | 全局回滚重试 3 次 |
| TCC Confirm/Cancel 应用层重试 | 3 次（指数退避） | 1s -> 2s -> 4s |
| `recovery.committing-retry-period` | 1000ms | Seata Server 提交重试周期 |
| `recovery.rollbacking-retry-period` | 1000ms | Seata Server 回滚重试周期 |

### 一致性检查

- [ ] 每个 TCC 的 Try 都记录幂等日志
- [ ] Confirm 和 Cancel 都是幂等的
- [ ] 所有正向 Saga 操作都有对应的补偿逻辑
- [ ] 无悬挂事务（通过 TCC Fence 保护）
- [ ] 重试耗尽后自动发告警
- [ ] 资金类用 TCC（强一致性），持仓类用 Saga（最终一致性）
