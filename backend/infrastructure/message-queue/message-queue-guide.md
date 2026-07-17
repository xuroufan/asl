# 消息队列使用指南

## 概述

期货交易平台采用**双消息队列架构**：

| 消息队列 | 用途 | 选型理由 |
|----------|------|----------|
| **RocketMQ** | 核心交易链路 | 事务消息保证最终一致性，与 Spring Cloud Alibaba 生态无缝集成 |
| **Kafka** | 行情数据流 | 超高吞吐量，适合海量 Tick 数据的分发与存储 |

### 架构图

```
┌─────────────┐     RocketMQ      ┌────────────────┐
│  订单服务    │────order-created──▶│   撮合引擎     │
│  (order)    │◀───order-matched──│  (matching)    │
└─────────────┘     order-cancelled│                │
                                  └────┬───────────┘
                                       │
                        ┌──────────────┼──────────────┐
                        ▼              ▼              ▼
                  ┌──────────┐ ┌──────────┐ ┌──────────┐
                  │ 账户服务 │ │ 资金服务 │ │ 风控服务 │
                  │ (account)│ │  (fund)  │ │  (risk)  │
                  └──────────┘ └──────────┘ └──────────┘
                        ▲              ▲
                        │              │
                  ┌─────┴──────────────┴─────┐
                  │       清结算服务          │
                  │     (settlement)         │
                  └──────────────────────────┘

┌─────────────┐       Kafka         ┌────────────────┐
│  行情服务    │────market-tick──────▶│  风控引擎      │
│  (market)   │                      │  + WebSocket   │
└─────────────┘                      │  终端推送      │
                                     └────────────────┘
```

---

## Topic 定义大全

### RocketMQ Topics（交易核心）

| Topic 名称 | 生产者 | 消费者 | 队列数 | 用途 | 可靠性要求 |
|------------|--------|--------|--------|------|-----------|
| `futures-order-created` | 订单服务 | 撮合引擎 | 8 | 新订单待撮合 | 不丢消息 |
| `futures-order-matched` | 撮合引擎 | 订单/账户/风控 | 8 | 成交结果通知 | 不丢消息 |
| `futures-order-cancelled` | 订单/撮合 | 资金服务 | 4 | 撤单通知 | 不丢消息 |
| `futures-position-changed` | 撮合引擎 | 账户/风控 | 4 | 持仓变动 | 允许少量延迟 |
| `futures-risk-alert` | 风控引擎 | 通知服务 | 2 | 风控预警 | 高优先推送 |
| `futures-market-tick` | 行情服务 | 风控引擎 | 16 | 实时行情 Tick | 允许丢失 |
| `futures-settlement-done` | 清结算 | 账户/通知 | 4 | 结算完成 | 不丢消息 |

### Kafka Topics（行情数据流）

| Topic 名称 | 分区数 | 副本数 | 保留时间 | 用途 |
|------------|--------|--------|----------|------|
| `futures-market-tick` | 16 | 1 | 1440min(1天) | 高吞吐行情流 |
| `futures-market-kline` | 8 | 1 | 4320min(3天) | K线数据 |
| `futures-market-depth` | 8 | 1 | 1440min(1天) | 盘口深度 |
| `futures-trade-audit` | 4 | 1 | 10080min(7天) | 交易审计日志 |
| `futures-system-log` | 4 | 1 | 4320min(3天) | 系统日志 |

---

## 快速启动

### 前置条件

```bash
# 进入项目目录
cd /Users/fangfang/Documents/黑期/futures-platform

# 确保 Docker 运行中
docker info > /dev/null 2>&1 || echo "请先启动 Docker Desktop"
```

### 启动 RocketMQ

```bash
# 启动所有 RocketMQ 组件
docker compose up -d rocketmq-namesrv rocketmq-broker rocketmq-dashboard

# 验证启动状态
docker ps | grep rocketmq
# 预期看到:
# futures-rocketmq-ns       Up   0.0.0.0:9876->9876/tcp
# futures-rocketmq-broker   Up   0.0.0.0:10909->10909/tcp, 0.0.0.0:10911->10911/tcp
# futures-rocketmq-dash     Up   0.0.0.0:8089->8089/tcp

# 访问 Dashboard
open http://localhost:8089
```

### 启动 Kafka

```bash
# 启动 Kafka + ZooKeeper
docker compose up -d zookeeper kafka

# 验证
docker ps | grep -E "zookeeper|kafka"
```

### 初始化 Topic

```bash
# 方法1：通过 Docker 容器执行
cd infrastructure/message-queue

# RocketMQ Topic
docker exec futures-rocketmq-broker \
  sh /home/rocketmq/rocketmq-5.2.0/bin/mqadmin updateTopic \
  -n localhost:9876 -c futures-cluster \
  -t futures-order-created -r 8 -w 8

# Kafka Topic
docker exec futures-kafka \
  kafka-topics --bootstrap-server localhost:9092 \
  --create --topic futures-market-tick \
  --partitions 16 --replication-factor 1 --if-not-exists

# 方法2：使用自动化脚本
bash rocketmq/topic-init.sh
bash kafka/topic-init.sh
```

---

## Spring Boot 集成代码

### 1. 消息常量定义（futures-common）

```java
package com.futures.common.message;

/**
 * RocketMQ Topic 常量定义
 */
public final class RocketMQTopic {

    private RocketMQTopic() {}

    /** 订单创建 - 订单服务 → 撮合引擎 */
    public static final String ORDER_CREATED = "futures-order-created";
    /** 订单成交 - 撮合引擎 → 订单/账户/风控 */
    public static final String ORDER_MATCHED = "futures-order-matched";
    /** 订单取消 - 订单/撮合 → 资金 */
    public static final String ORDER_CANCELLED = "futures-order-cancelled";
    /** 持仓变动 - 撮合引擎 → 账户/风控 */
    public static final String POSITION_CHANGED = "futures-position-changed";
    /** 风控预警 - 风控引擎 → 通知 */
    public static final String RISK_ALERT = "futures-risk-alert";
    /** 行情Tick - 行情服务 → 风控/终端 */
    public static final String MARKET_TICK = "futures-market-tick";
    /** 结算完成 - 清结算 → 账户/通知 */
    public static final String SETTLEMENT_DONE = "futures-settlement-done";
}
```

```java
package com.futures.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单成交事件（通用消息体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMatchedEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private String direction;        // BUY / SELL
    private BigDecimal price;
    private int volume;
    private BigDecimal totalAmount;
    private BigDecimal fee;
    private String matchId;
    private LocalDateTime matchedAt;
}
```

```java
package com.futures.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行情Tick事件（通过Kafka高吞吐分发）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketTickEvent {
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private long volume;
    private BigDecimal openInterest;
    private String exchange;
    private LocalDateTime timestamp;
}
```

### 2. RocketMQ 配置（futures-common）

```java
package com.futures.common.message.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * RocketMQ 消息转换器配置
 * 支持 Java 8+ 时间类型序列化
 */
@Configuration
public class RocketMQConfig {

    @Bean
    public MessageConverter jacksonMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        converter.setObjectMapper(mapper);
        return converter;
    }
}
```

### 3. 生产者示例（订单服务）

```java
// futures-order 服务中发送订单创建消息
package com.futures.order.mq;

import com.futures.common.message.RocketMQTopic;
import com.futures.common.message.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送订单创建消息给撮合引擎
     * 使用同步发送确保消息不丢失
     */
    public void sendOrderCreated(OrderCreatedEvent event) {
        Message<OrderCreatedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader("orderId", event.getOrderId())
            .setHeader("userId", event.getUserId())
            .build();

        rocketMQTemplate.syncSend(RocketMQTopic.ORDER_CREATED, message);
        log.info("订单创建消息已发送: orderId={}", event.getOrderId());
    }

    /**
     * 发送撤单通知
     */
    public void sendOrderCancelled(String orderId, String userId) {
        rocketMQTemplate.syncSend(
            RocketMQTopic.ORDER_CANCELLED,
            OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .cancelledAt(LocalDateTime.now())
                .build()
        );
        log.info("撤单消息已发送: orderId={}", orderId);
    }
}
```

### 4. 消费者示例（撮合引擎）

```java
// futures-matching 服务中消费订单创建消息
package com.futures.matching.mq;

import com.futures.common.message.RocketMQTopic;
import com.futures.common.message.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = RocketMQTopic.ORDER_CREATED,
    consumerGroup = "cg-order-created",
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    maxReconsumeTimes = 3
)
public class OrderCreatedConsumer implements RocketMQListener<OrderCreatedEvent> {

    private final MatchingEngine matchingEngine;

    @Override
    public void onMessage(OrderCreatedEvent event) {
        log.info("收到新订单: orderId={}, symbol={}, direction={}, price={}, volume={}",
            event.getOrderId(), event.getSymbol(), event.getDirection(),
            event.getPrice(), event.getVolume());

        try {
            matchingEngine.processOrder(event);
        } catch (Exception e) {
            log.error("处理订单失败: orderId={}", event.getOrderId(), e);
            // 抛出异常将触发 RocketMQ 重试（最多3次）
            throw new RuntimeException("Order processing failed", e);
        }
    }
}
```

### 5. 成交结果消费者（订单服务 + 账户服务）

```java
// futures-order 服务消费成交结果
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMQTopic.ORDER_MATCHED,
    consumerGroup = "cg-order-matched",
    selectorExpression = "*"
)
public class OrderMatchedConsumer implements RocketMQListener<OrderMatchedEvent> {

    @Override
    public void onMessage(OrderMatchedEvent event) {
        log.info("订单成交: orderId={}, price={}, volume={}",
            event.getOrderId(), event.getPrice(), event.getVolume());

        // 更新订单状态为 FILLED 或 PARTIAL
        // 发送 WebSocket 推送通知终端
    }
}
```

### 6. 事务消息（下单场景）

```java
// futures-order 服务 - 事务消息发送方
package com.futures.order.mq;

import com.futures.common.message.RocketMQTopic;
import com.futures.common.message.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTransactionProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final OrderService orderService;
    private final FundClient fundClient;

    /**
     * 下单事务消息：
     * 1. 创建订单（本地事务）
     * 2. 冻结保证金（通过事务消息保证最终一致性）
     * 3. 发送消息给撮合引擎
     */
    @Transactional
    public void placeOrder(OrderRequest request) {
        // Step 1: 创建订单（本地数据库）
        Order order = orderService.createOrder(request);
        
        // Step 2: 发送事务消息
        // 消息发送成功后才会提交本地事务
        // 若本地事务失败，RocketMQ 回查后回滚消息
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        
        Message<OrderCreatedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader("orderId", order.getOrderId())
            .setHeader("userId", order.getUserId())
            .build();

        // 使用事务消息发送
        rocketMQTemplate.sendMessageInTransaction(
            RocketMQTopic.ORDER_CREATED,
            message,
            null  // 可传递额外的业务参数
        );
        
        log.info("下单事务消息已发送: orderId={}", order.getOrderId());
    }

    /**
     * 本地事务执行器
     * RocketMQ 回调此方法执行本地事务
     */
    @RocketMQTransactionListener
    public class OrderTransactionListener implements RocketMQLocalTransactionListener {
        
        @Override
        public RocketMQLocalTransactionState executeLocalTransaction(
                Message msg, Object arg) {
            try {
                // 冻结保证金
                fundClient.freeze(userId, marginAmount);
                log.info("本地事务执行成功: 订单创建 + 保证金冻结");
                return RocketMQLocalTransactionState.COMMIT;
            } catch (Exception e) {
                log.error("本地事务执行失败: 回滚订单", e);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
        }

        @Override
        public RocketMQLocalTransactionState checkLocalTransaction(
                Message msg) {
            // 事务状态回查
            String orderId = (String) msg.getHeaders().get("orderId");
            Order order = orderService.getOrder(orderId);
            
            if (order == null) {
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            if (order.getStatus() == OrderStatus.PENDING) {
                return RocketMQLocalTransactionState.UNKNOWN;
            }
            return RocketMQLocalTransactionState.COMMIT;
        }
    }
}
```

### 7. 应用配置（application.yml）

```yaml
# 每个需要 RocketMQ 的服务的 application.yml 配置
rocketmq:
  name-server: ${ROCKETMQ_NS:localhost:9876}
  producer:
    group: futures-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 3
    retry-times-when-send-async-failed: 2
    compress-message-body-threshold: 4096
    max-message-size: 4194304
  consumer:
    pull-batch-size: 32
```

### 8. 消息重试与死信队列

```java
/**
 * 消息消费失败时的处理策略
 * 
 * 1. RocketMQ 自动重试（maxReconsumeTimes=3）
 * 2. 重试3次仍失败 → 进入死信队列
 * 3. 死信队列名称: %DLQ%cg-order-created
 * 4. 运维定期处理死信队列中的消息
 */

// 消费者配置 - 重试和死信
@RocketMQMessageListener(
    topic = RocketMQTopic.ORDER_CREATED,
    consumerGroup = "cg-order-created",
    maxReconsumeTimes = 3,          // 最大重试次数
    consumeTimeout = 15000,         // 消费超时(15秒)
    delayLevelWhenNextConsume = 3,  // 重试间隔等级(3=10s)
    reconsumeTimes = 3
)
```

### 9. 消息监控配置（Prometheus）

```yaml
# prometheus.yml - 添加 RocketMQ 和 Kafka 的监控采集
scrape_configs:
  # RocketMQ Exporter
  - job_name: 'rocketmq'
    static_configs:
      - targets: ['rocketmq-exporter:5557']
  
  # Kafka Exporter
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
  
  # JMX Exporter (每个 Java 服务)
  - job_name: 'futures-order'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:8081']
```

---

## 常见问题

### Q: RocketMQ 启动失败

**症状**：容器不断重启或无法连接

**检查清单**：
1. `docker logs futures-rocketmq-broker` 查看错误日志
2. 确认 namesrv 已先启动（broker 依赖 namesrv）
3. 检查磁盘空间是否充足（RocketMQ 需要大量磁盘空间）
4. 确认 9876/10909/10911 端口未被占用

### Q: 消费消息延迟高

**排查步骤**：
1. Dashboard 查看消费积压量
2. 检查消费者是否挂掉或处理慢
3. 确认是否网络延迟导致
4. 增加分区数提高并发消费能力

### Q: 消息丢失

**排查步骤**：
1. 检查生产者日志：确认 `sendResult.getSendStatus() == SEND_OK`
2. 检查 Broker 日志：确认消息已持久化
3. 确认消费者手动提交 offset

---

## RocketMQ Dashboard 访问

| 环境 | 地址 | 说明 |
|------|------|------|
| 开发环境 | http://localhost:8089 | Docker 部署 |
| 测试环境 | http://mq-dashboard.test.futures.com | K8s 部署 |
| 生产环境 | http://mq-dashboard.futures.com | K8s 部署 |

---

## 相关文档

- [RocketMQ 官方文档](https://rocketmq.apache.org/docs/)
- [Kafka 官方文档](https://kafka.apache.org/documentation/)
- [Spring Cloud Stream RocketMQ](https://sca.aliyun.com/zh-cn/docs/next/user-guide/rocketmq/overview)
- [Prometheus RocketMQ Exporter](https://github.com/apache/rocketmq-exporter)
- [基础设施架构文档](../architecture.md)
