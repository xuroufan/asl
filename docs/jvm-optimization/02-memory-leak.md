# 2. 内存泄漏排查与修复

## 2.1 ThreadLocal 未清理

### 风险位置扫描

| 文件 | 风险等级 | 说明 |
|------|---------|------|
| `futures-common` 中的日志追踪 | 🔴 | MDC `put()` 后未 `clear()` |
| `futures-matching` 的 Disruptor 事件处理器 | 🟡 | 单线程设计，无风险 |
| `futures-order` 的 MyBatis Plus 分页 | 🟡 | `PageHelper` 自动清理 |
| `futures-gateway` 的 Spring Cloud Gateway 过滤器 | 🟠 | 需确认 ThreadLocal 在 filter 后清理 |

### 修复代码 - MDC ThreadLocal 泄漏

```java
// 🔴 风险代码：futures-common 日志链路追踪
// 当前代码
import org.slf4j.MDC;

@Aspect
@Component
public class LogTraceAspect {
    @Around("@annotation(Traceable)")
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {
        try {
            MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
            MDC.put("userId", getCurrentUserId());
            return pjp.proceed();
        } finally {
            // 🔴 缺少 MDC.clear()
            // MDC.clear(); ← 缺失！
        }
    }
}

// ✅ 修复后
@Around("@annotation(Traceable)")
public Object trace(ProceedingJoinPoint pjp) throws Throwable {
    try {
        MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
        MDC.put("userId", getCurrentUserId());
        return pjp.proceed();
    } finally {
        MDC.clear();  // ✅ 必须清理
    }
}
```

### 通用 ThreadLocal 清理工具

```java
/**
 * ThreadLocal 工具类 - 确保在使用后自动清理
 */
public final class ThreadLocalUtils {

    private ThreadLocalUtils() {}

    /**
     * 带自动清理的 ThreadLocal 包装
     */
    public static <T> T executeWithCleanup(ThreadLocal<T> threadLocal, Supplier<T> action) {
        try {
            return action.get();
        } finally {
            threadLocal.remove();
        }
    }

    /**
     * 容器环境下的 ThreadLocal 兜底清理
     * 通过 Spring 的 RequestContextListener 或过滤器实现
     */
    @WebFilter(urlPatterns = "/*")
    @Component
    public static class ThreadLocalCleanupFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            try {
                chain.doFilter(request, response);
            } finally {
                // 清理所有已知的 ThreadLocal
                RequestContextHolder.resetRequestAttributes();
                LocaleContextHolder.resetLocaleContext();
                TransactionSynchronizationManager.clear();
                MDC.clear();
            }
        }
    }
}
```

## 2.2 静态集合持有对象

### 风险位置

| 位置 | 风险等级 | 说明 |
|------|---------|------|
| **`MatchingEngine.stopOrders`** `ConcurrentSkipListMap<Long, List<Order>>` | 🔴 | 止损单停留在内存中可能永不触发，需要TTL兜底 |
| **`OrderBookManager.engines`** `ConcurrentHashMap<String, MatchingEngine>` | 🟠 | Symbol 数量有限（通常 < 100），风险低 |
| **`OrderBook` 中的价格队列** | 🟠 | 订单簿深度合理范围内，但需监控 |

### 修复 - MatchingEngine 止损单 TTL ⭐ 关键修复

```java
// 🔴 当前代码：止损单永不清理，可能造成内存泄漏
// MatchingEngine.java 第32行
private final ConcurrentSkipListMap<Long, List<Order>> stopOrders;

public List<MatchResult> handleStopOrder(Order order) {
    long triggerPrice = order.getPrice();
    stopOrders.compute(triggerPrice, (k, list) -> {
        if (list == null) list = new ArrayList<>();
        list.add(order);
        return list;
    });
    return Collections.emptyList();
}

// ✅ 修复：添加定时清理 + TTL 兜底策略
public class MatchingEngine {
    // ... 原有代码 ...
    
    // 新增：止损单 TTL 配置（默认 7 天）
    private static final long STOP_ORDER_TTL_MS = 7 * 24 * 60 * 60 * 1000L;

    public List<MatchResult> handleStopOrder(Order order) {
        long triggerPrice = order.getPrice();
        stopOrders.compute(triggerPrice, (k, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(order);
            return list;
        });
        // 记录时间戳用于 TTL 清理
        stopOrderTimestamps.put(order.getOrderId(), System.currentTimeMillis());
        return Collections.emptyList();
    }

    /**
     * 定期清理超时止损单（由 ScheduledExecutor 每分钟执行）
     */
    public void purgeExpiredStopOrders() {
        long now = System.currentTimeMillis();
        List<Long> emptyPrices = new ArrayList<>();
        
        for (Map.Entry<Long, List<Order>> entry : stopOrders.entrySet()) {
            List<Order> orders = entry.getValue();
            orders.removeIf(order -> {
                Long ts = stopOrderTimestamps.get(order.getOrderId());
                return ts != null && (now - ts) > STOP_ORDER_TTL_MS;
            });
            if (orders.isEmpty()) {
                emptyPrices.add(entry.getKey());
            }
        }
        emptyPrices.forEach(stopOrders::remove);
    }
}
```

## 2.3 大对象直接进入老年代

### 风险位置

| 文件 | 风险等级 | 说明 |
|------|---------|------|
| **`MatchingEngine.processOrder()` 中的 `new ArrayList<>()`** | 🟠 | 每次撮合创建多个ArrayList，对象频繁进入老年代 |
| `MatchingService` 中的深度复制 | 🟠 | 订单拷贝可能产生大对象 |

### 修复 - 对象池化减少 GC 压力

```java
// 🟠 风险代码：每次撮合创建大量临时对象
// MatchingEngine.java
public MatchedOrder processOrder(Order order) {
    List<MatchResult> results;  // 每次调用 new ArrayList
    switch (order.getType()) {
        case LIMIT:    results = matchLimitOrder(order);    break;  // ← 内部也 new ArrayList
        case MARKET:   results = matchMarketOrder(order);   break;  // ← 内部也 new ArrayList
        // ...
    }
}

// ✅ 优化：使用线程本地对象池复用 MatchResult 列表
// ✅ 修改：在 Disruptor 的事件处理器中预创建列表
public class OrderEventHandler implements EventHandler<OrderEvent> {
    private static final int INITIAL_RESULT_CAPACITY = 64;
    
    // 使用 ThreadLocal 复用列表（Disruptor 单线程，无竞态）
    private final ThreadLocal<List<MatchResult>> resultPool = 
        ThreadLocal.withInitial(() -> new ArrayList<>(INITIAL_RESULT_CAPACITY));
    
    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        List<MatchResult> results = resultPool.get();
        results.clear();
        // ... 撮合逻辑，将结果填入 results
        event.setResults(results);
    }
}
```

## 2.4 直接内存泄漏

### 扫描 Netty/文件通道使用

| 文件 | 风险等级 | 说明 |
|------|---------|------|
| `futures-gateway` (Spring Cloud Gateway) | 🟠 | 基于 Netty，需确认 ByteBuf 释放 |
| `futures-push` WebSocket 连接 | 🟠 | 长时间连接需监控直接内存 |
| NIO 文件操作 | 🟡 | 无大规模文件操作 |

### 直接内存监控 Bean ⭐ 关键新增

```java
/**
 * 直接内存监控 - 添加到 common 模块
 */
@Configuration
public class DirectMemoryMonitorConfig {

    @Bean
    public DirectMemoryMonitor directMemoryMonitor() {
        return new DirectMemoryMonitor();
    }

    public static class DirectMemoryMonitor {
        private static final Logger log = LoggerFactory.getLogger(DirectMemoryMonitor.class);
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        @PostConstruct
        public void start() {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // 获取直接内存使用量
                    long directMemory = ManagementFactory.getPlatformMXBean(
                        com.sun.management.BufferPoolMXBean.class
                    ).getMemoryUsed();
                    
                    log.info("Direct Memory: {} MB", directMemory / 1024 / 1024);
                } catch (Exception e) {
                    log.warn("Failed to monitor direct memory", e);
                }
            }, 1, 30, TimeUnit.SECONDS);
        }

        @PreDestroy
        public void stop() {
            scheduler.shutdown();
        }
    }
}
```

## 2.5 内存泄漏检测脚本

```bash
#!/bin/bash
# mem-leak-scan.sh - 基于 jmap 的内存泄漏快速诊断

PID=$1
if [ -z "$PID" ]; then
    echo "Usage: $0 <pid>"
    exit 1
fi

echo "=== 1. 类实例数 Top 20 ==="
jmap -histo:live $PID | head -30

echo ""
echo "=== 2. String 对象统计 ==="
jmap -histo:live $PID | grep -E "String|char\[\]" | head -10

echo ""
echo "=== 3. 直接内存使用 ==="
jcmd $PID VM.native_memory summary | grep -A 10 "Internal|Direct"

echo ""
echo "=== 4. GC 根引用 ==="
jcmd $PID GC.class_stats 2>/dev/null | head -20 || echo "GC.class_stats not available"
```
