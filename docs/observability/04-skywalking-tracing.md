# 全链路追踪（SkyWalking）

> 采样策略: 生产 10% 采样，错误请求 100% 采样
> 自定义 Span: 下单、撮合、资金冻结等关键业务

---

## 1. Agent 配置

### 1.1 Docker 启动参数

```bash
# 在 JAVA_OPTS 中添加 SkyWalking Agent
-javaagent:/opt/skywalking/agent/skywalking-agent.jar
-Dskywalking.agent.service_name=${SERVICE_NAME}
-Dskywalking.collector.backend_service=${SW_BACKEND:skywalking-oap:11800}
-Dskywalking.agent.sample_n_per_3_secs=30          # 采样率 ~10%
-Dskywalking.agent.span_limit_per_segment=300       # 单条链路上限
-Dskywalking.agent.ignore_suffix=.jpg,.js,.css,.png # 忽略静态资源
```

### 1.2 agent/config/agent.config

```properties
# agent.config 关键配置
agent.service_name=${SW_AGENT_NAME:unknown-service}
collector.backend_service=${SW_BACKEND:skywalking-oap:11800}

# ── 采样配置 ──
agent.sample_n_per_3_secs=30           # 每 3 秒采样 30 条，约 10% 流量
agent.span_limit_per_segment=300
agent.ignore_suffix=.jpg,.js,.css,.png,.ico,.woff

# ── 错误请求 100% 采样 ──
agent.force_auto_span_segment_on_error=true

# ── 数据库监控 ──
plugin.mysql.trace_sql_parameters=true   # 记录 SQL 参数
plugin.mysql.sql_tracing_max_length=2048

# ── Redis 监控 ──
plugin.redis.parameters_max_len=256

# ── MQ 监控 ──
plugin.rocketmq.trace_message=true

# ── 日志框架集成 ──
plugin.toolkit.log.grpc.reporter_active=true
plugin.toolkit.log.grpc.reporter_interval=30
```

---

## 2. 自定义 Span（关键业务链路）

### 2.1 下单链路（order-service）

```java
package com.hackfuture.order.service;

import com.hackfuture.order.metrics.OrderMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Tags;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final FundClient fundClient;
    private final RiskClient riskClient;
    private final MatchingClient matchingClient;
    private final OrderMetrics orderMetrics;

    /**
     * 下单 —— 全链路追踪的关键方法
     *
     * 自定义 Span 记录:
     *   - 合约代码 (symbol)
     *   - 订单方向 (side)
     *   - 订单数量 (quantity)
     *   - 下游调用的耗时
     */
    @Trace(operationName = "OrderService#placeOrder")
    @Tags({
        @Tag(key = "symbol", value = "arg[0].symbol"),
        @Tag(key = "side", value = "arg[0].side"),
        @Tag(key = "quantity", value = "arg[0].quantity"),
        @Tag(key = "userId", value = "arg[0].userId")
    })
    public ApiResult placeOrder(PlaceOrderRequest request) {
        long start = System.currentTimeMillis();

        // 1. 风控检查（自定义子 Span）
        RiskCheckResult riskResult = checkRiskWithSpan(request);

        // 2. 冻结资金（TCC Try，Feign 调用）
        FundFreezeResult fundResult = freezeFundWithSpan(request);

        // 3. 提交撮合
        MatchingResult matchResult = submitToMatchingWithSpan(request);

        orderMetrics.recordOrderPlaced(
                request.getSymbol(), request.getSide().name());

        long cost = System.currentTimeMillis() - start;
        log.info("订单创建成功: orderId={}, userId={}, symbol={}, cost={}ms",
                matchResult.getOrderId(), request.getUserId(),
                request.getSymbol(), cost);

        return ApiResult.success(matchResult);
    }

    /**
     * 风控检查 — 创建自定义子 Span
     */
    @Trace(operationName = "OrderService#checkRisk")
    @Tag(key = "riskResult", value = "returnVal")
    protected RiskCheckResult checkRiskWithSpan(PlaceOrderRequest request) {
        return riskClient.checkOrder(new RiskCheckRequest(request));
    }

    /**
     * 冻结资金 — 创建自定义子 Span
     */
    @Trace(operationName = "OrderService#freezeFund")
    @Tags({
        @Tag(key = "amount", value = "arg[0].totalAmount"),
        @Tag(key = "asset", value = "returnVal.asset")
    })
    protected FundFreezeResult freezeFundWithSpan(PlaceOrderRequest request) {
        return fundClient.freezeBalance(new FreezeRequest(request));
    }

    /**
     * 提交撮合 — 创建自定义子 Span
     */
    @Trace(operationName = "OrderService#submitToMatching")
    @Tag(key = "orderId", value = "returnVal.orderId")
    protected MatchingResult submitToMatchingWithSpan(PlaceOrderRequest request) {
        return matchingClient.submitOrder(request);
    }
}
```

### 2.2 撮合引擎（matching-service）

```java
package com.hackfuture.matching.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Tags;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngine {

    /**
     * 撮合核心方法
     * 记录合约代码、撮合耗时、匹配数量
     */
    @Trace(operationName = "MatchingEngine#match")
    @Tags({
        @Tag(key = "symbol", value = "arg[0]"),
        @Tag(key = "matchCount", value = "returnVal.size()"),
        @Tag(key = "totalQty", value = "returnVal.totalQuantity")
    })
    public List<Trade> match(String symbol, Order order) {
        long start = System.nanoTime();
        try {
            // 从订单簿查找对手单
            OrderBook book = orderBookManager.getOrderBook(symbol);
            List<Trade> trades = book.match(order);

            if (!trades.isEmpty()) {
                // 落盘成交记录
                tradeRepository.batchInsert(trades);
                // 更新订单状态
                orderRepository.updateStatus(order.getOrderId(),
                        order.getStatus(), order.getFilledQuantity(),
                        order.getAverageFilledPrice());
            }

            return trades;
        } finally {
            long cost = System.nanoTime() - start;
            log.info("撮合完成: symbol={}, orderId={}, cost={}μs",
                    symbol, order.getOrderId(), cost / 1000);
        }
    }
}
```

### 2.3 手动创建 Span（更灵活的场景）

```java
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

@Service
public class FundService {

    public FreezeResult freeze(Long userId, String asset, BigDecimal amount) {
        // 创建自定义 Span
        ActiveSpan.tag("freeze_user_id", String.valueOf(userId));
        ActiveSpan.tag("freeze_asset", asset);
        ActiveSpan.tag("freeze_amount", amount.toPlainString());
        ActiveSpan.setOperationName("FundService#freeze");

        try {
            // 业务逻辑...
            FreezeResult result = doFreeze(userId, asset, amount);

            // 记录结果到 Span
            ActiveSpan.tag("freeze_result", result.isSuccess() ? "SUCCESS" : "FAILED");
            ActiveSpan.error(result.getErrorMsg());  // 记录错误

            return result;
        } catch (Exception e) {
            // 记录异常到 Span
            ActiveSpan.error(e);
            throw e;
        }
    }
}
```

---

## 3. SkyWalking 告警规则

```yaml
# skywalking/config/alarm-settings.yml

rules:
  # ── 接口响应时间告警 ──
  service_resp_time_rule:
    include-names:
      - order-service
      - matching-service
    threshold: 3000                # 3 秒
    op: ">"
    period: 3                      # 最近 3 分钟
    count: 2                       # 连续 2 次超过阈值
    silence-period: 5              # 静默 5 分钟
    message: "服务 {{name}} 响应时间 > 3s"

  # ── 接口错误率告警 ──
  service_error_rate_rule:
    include-names:
      - order-service
      - matching-service
      - fund-service
    threshold: 20                  # 错误率 > 20%
    op: ">"
    period: 3
    count: 2
    silence-period: 5
    message: "服务 {{name}} 错误率 > 20%"

  # ── 自定义 Span 告警 ──
  order_place_latency_rule:
    include-names:
      - order-service
    threshold: 5000                # 下单 > 5 秒
    op: ">"
    period: 3
    count: 3
    message: "下单接口 {{name}} 连续 3 次响应 > 5s"

# 告警通知
webhooks:
  - http://alertmanager:9093/api/alerts
```
