package com.futures.matching.disruptor;

import com.futures.matching.engine.MatchedOrder;
import com.futures.matching.engine.MatchingEngine;
import com.futures.matching.engine.OrderBookManager;
import com.futures.matching.model.Order;
import com.futures.matching.mq.MatchEventProducer;
import com.futures.matching.persistence.EventJournal;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disruptor 事件处理器 — 单线程处理所有订单事件
 * <p>
 * 根据事件中的 symbol 路由到对应的 {@link MatchingEngine} 处理。
 * 撮合完成后：写入事件日志 → 发送 MQ 消息 → 完成异步 Future。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final OrderBookManager orderBookManager;
    private final MatchEventProducer matchEventProducer;
    private final EventJournal eventJournal;
    private final ConcurrentHashMap<String, CompletableFuture<MatchedOrder>> syncFutures;

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.isCancelOperation()) {
            handleCancel(event);
        } else if (event.getOrder() != null) {
            handleNewOrder(event);
        }
    }

    private void handleCancel(OrderEvent event) {
        String symbol = event.getSymbol();
        String orderId = event.getCancelOrderId();

        if (symbol == null || orderId == null) {
            log.warn("撤单参数不完整: symbol={}, orderId={}", symbol, orderId);
            return;
        }

        MatchingEngine engine = orderBookManager.getEngine(symbol);
        if (engine == null) {
            log.warn("撤单失败，合约无撮合引擎: symbol={}, orderId={}", symbol, orderId);
            return;
        }

        boolean success = engine.cancelOrder(orderId);
        log.info("撤单: symbol={}, orderId={}, success={}", symbol, orderId, success);
    }

    private void handleNewOrder(OrderEvent event) {
        Order order = event.getOrder();
        String symbol = order.getSymbol();

        if (symbol == null) {
            log.warn("订单缺少合约代码: orderId={}", order.getOrderId());
            return;
        }

        // 获取或创建该合约的撮合引擎
        MatchingEngine engine = orderBookManager.getOrCreateEngine(symbol);

        // 执行撮合
        MatchedOrder result = engine.processOrder(order);

        // 写入事件日志
        if (result.isHasTrades()) {
            eventJournal.append(result.getTrades());
        }

        // 发送 MQ 消息
        matchEventProducer.publishMatchResult(result);

        // 完成同步 Future（如果有）
        if (event.isSyncMode() && syncFutures != null) {
            CompletableFuture<MatchedOrder> future = syncFutures.remove(order.getOrderId());
            if (future != null) {
                future.complete(result);
            }
        }

        log.debug("撮合完成: orderId={}, symbol={}, trades={}, status={}",
                order.getOrderId(), symbol, result.getTrades().size(), result.getFinalStatus());
    }
}
