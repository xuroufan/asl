package com.futures.matching.disruptor;

import com.futures.matching.engine.MatchedOrder;
import com.futures.matching.engine.MatchingEngine;
import com.futures.matching.engine.OrderBookManager;
import com.futures.matching.model.MatchResult;
import com.futures.matching.model.Order;
import com.futures.matching.mq.MatchEventProducer;
import com.futures.matching.persistence.EventJournal;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Disruptor 配置 — 无锁队列接收订单输入
 * <p>
 * 支持多品种路由：通过 {@link OrderBookManager} 管理每个 Symbol 的撮合引擎。
 * RingBuffer 大小：2^17 = 131072，可抗大量订单突发。
 */
@Slf4j
@Component
public class DisruptorConfig {

    private static final int RING_BUFFER_SIZE = 131072; // 2^17
    private static final long SYNC_TIMEOUT_MS = 3000;

    @Getter
    private Disruptor<OrderEvent> disruptor;

    @Getter
    private RingBuffer<OrderEvent> ringBuffer;

    private final OrderBookManager orderBookManager;
    private final MatchEventProducer matchEventProducer;
    private final EventJournal eventJournal;

    /** 同步等待结果 */
    private final ConcurrentHashMap<String, CompletableFuture<MatchedOrder>> syncFutures = new ConcurrentHashMap<>();

    public DisruptorConfig(OrderBookManager orderBookManager,
                           MatchEventProducer matchEventProducer,
                           EventJournal eventJournal) {
        this.orderBookManager = orderBookManager;
        this.matchEventProducer = matchEventProducer;
        this.eventJournal = eventJournal;
    }

    @PostConstruct
    public void start() {
        disruptor = new Disruptor<>(
                OrderEvent::new,
                RING_BUFFER_SIZE,
                DaemonThreadFactory.INSTANCE,
                com.lmax.disruptor.dsl.ProducerType.SINGLE,
                new com.lmax.disruptor.SleepingWaitStrategy()
        );

        disruptor.handleEventsWith(new OrderEventHandler(
                orderBookManager, matchEventProducer, eventJournal, syncFutures));
        ringBuffer = disruptor.start();

        log.info("Disruptor 启动成功: ringBufferSize={}, waitStrategy=BusySpin",
                RING_BUFFER_SIZE);
    }

    @PreDestroy
    public void shutdown() {
        if (disruptor != null) {
            disruptor.shutdown();
            log.info("Disruptor 已关闭");
        }
    }

    // ==================== 发布方法 ====================

    /**
     * 异步发布订单到 Disruptor RingBuffer（不等待结果）。
     */
    public void publishOrder(Order order) {
        ringBuffer.publishEvent((event, sequence) -> {
            event.setOrder(order);
            event.setSymbol(order.getSymbol());
            event.setCancelOperation(false);
            event.setCancelOrderId(null);
            event.setSyncMode(false);
            event.setSyncResult(null);
        });
    }

    /**
     * 同步发布订单 — 等待撮合结果。
     */
    public MatchedOrder publishOrderSync(Order order) {
        CompletableFuture<MatchedOrder> future = new CompletableFuture<>();
        syncFutures.put(order.getOrderId(), future);

        ringBuffer.publishEvent((event, sequence) -> {
            event.setOrder(order);
            event.setSymbol(order.getSymbol());
            event.setCancelOperation(false);
            event.setCancelOrderId(null);
            event.setSyncMode(true);
            event.setSyncResult(null);
        });

        try {
            return future.get(SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("同步撮合超时: orderId={}, timeout={}ms",
                    order.getOrderId(), SYNC_TIMEOUT_MS);
            syncFutures.remove(order.getOrderId());
            return null;
        }
    }

    /**
     * 发布撤单到 Disruptor RingBuffer。
     */
    public void publishCancel(String symbol, String orderId) {
        ringBuffer.publishEvent((event, sequence) -> {
            event.setSymbol(symbol);
            event.setOrder(null);
            event.setCancelOperation(true);
            event.setCancelOrderId(orderId);
            event.setSyncMode(false);
            event.setSyncResult(null);
        });
    }

    // ==================== 状态查询 ====================

    /** 获取所有活跃的合约代码 */
    public Set<String> getActiveSymbols() {
        return orderBookManager.getSymbols();
    }

    /** 获取订单簿管理器 */
    public OrderBookManager getOrderBookManager() {
        return orderBookManager;
    }
}
