package com.futures.matching.service;

import com.futures.matching.controller.*;
import com.futures.matching.disruptor.DisruptorConfig;
import com.futures.matching.engine.MatchedOrder;
import com.futures.matching.engine.OrderBook;
import com.futures.matching.engine.OrderBookManager;
import com.futures.matching.model.Order;
import com.futures.matching.model.OrderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 撮合服务 — HTTP 与 Disruptor 之间的桥接层。
 * <p>
 * 负责将 REST 请求转换为撮合引擎指令，通过 Disruptor 异步处理。
 * </p>
 */
@Slf4j
@Service
public class MatchingService {

    private final DisruptorConfig disruptorConfig;
    private final OrderBookManager orderBookManager;

    public MatchingService(DisruptorConfig disruptorConfig, OrderBookManager orderBookManager) {
        this.disruptorConfig = disruptorConfig;
        this.orderBookManager = orderBookManager;
    }

    /**
     * 下单处理（同步模式 — 等待撮合结果）。
     */
    public MatchedOrder placeOrder(MatchingRequest request) {
        validateRequest(request);

        Order.Direction dir;
        try {
            dir = Order.Direction.valueOf(request.getDirection().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的买卖方向: " + request.getDirection() + "，仅支持 BUY/SELL");
        }

        Order.OrderType type;
        try {
            type = Order.OrderType.valueOf(request.getOrderType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的订单类型: " + request.getOrderType());
        }

        // 构建引擎内部订单
        // 注意：userId 需要转为 String，MatchingController 中也需要处理
        Order order = Order.builder()
                .orderId("ORD-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .userId(String.valueOf(request.getUserId()))
                .direction(dir)
                .type(type)
                .price(request.getPrice() != null ? request.getPrice().longValue() : 0)
                .volume(request.getVolume())
                .filledVolume(0)
                .timestamp(System.nanoTime())
                .build();

        // 同步撮合
        MatchedOrder result = disruptorConfig.publishOrderSync(order);
        return result;
    }

    /**
     * 撤单处理。
     */
    public boolean cancelOrder(CancelRequest request) {
        if (request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new IllegalArgumentException("合约代码不能为空");
        }
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("订单 ID 不能为空");
        }

        // 通过 Disruptor 异步撤单
        disruptorConfig.publishCancel(request.getSymbol(), request.getOrderId());
        return true;
    }

    /**
     * 查询五档盘口深度。
     */
    public DepthResponse getDepth(String symbol) {
        OrderBook orderBook = orderBookManager.getOrderBook(symbol);
        if (orderBook == null) return null;

        DepthResponse resp = new DepthResponse();
        resp.setSymbol(symbol);
        resp.setBestBidPrice(orderBook.getBestBidPrice());
        resp.setBestAskPrice(orderBook.getBestAskPrice());

        // 买盘（前5档）：engine/OrderBook 的 bids() 返回从高到低排序的 entry
        List<DepthResponse.DepthLevel> bidLevels = new ArrayList<>(5);
        int count = 0;
        for (Map.Entry<Long, Deque<Order>> entry : orderBook.bids()) {
            if (count++ >= 5) break;
            long totalVolume = 0;
            for (Order o : entry.getValue()) {
                totalVolume += o.remainingVolume();
            }
            DepthResponse.DepthLevel level = new DepthResponse.DepthLevel();
            level.setPrice(java.math.BigDecimal.valueOf(entry.getKey()));
            level.setVolume(totalVolume);
            level.setOrderCount(entry.getValue().size());
            bidLevels.add(level);
        }
        resp.setBids(bidLevels);

        // 卖盘（前5档）
        List<DepthResponse.DepthLevel> askLevels = new ArrayList<>(5);
        count = 0;
        for (Map.Entry<Long, Deque<Order>> entry : orderBook.asks()) {
            if (count++ >= 5) break;
            long totalVolume = 0;
            for (Order o : entry.getValue()) {
                totalVolume += o.remainingVolume();
            }
            DepthResponse.DepthLevel level = new DepthResponse.DepthLevel();
            level.setPrice(java.math.BigDecimal.valueOf(entry.getKey()));
            level.setVolume(totalVolume);
            level.setOrderCount(entry.getValue().size());
            askLevels.add(level);
        }
        resp.setAsks(askLevels);

        return resp;
    }

    /**
     * 查询引擎状态。
     */
    public EngineStatusResponse getEngineStatus() {
        EngineStatusResponse resp = new EngineStatusResponse();
        resp.setActiveSymbols(new ArrayList<>(disruptorConfig.getActiveSymbols()));

        List<EngineStatusResponse.SymbolStatus> statuses = new ArrayList<>();
        for (String symbol : disruptorConfig.getActiveSymbols()) {
            OrderBook book = orderBookManager.getOrderBook(symbol);
            if (book == null) continue;

            EngineStatusResponse.SymbolStatus ss = new EngineStatusResponse.SymbolStatus();
            ss.setSymbol(symbol);
            ss.setBidLevelCount(book.getBidLevelCount());
            ss.setAskLevelCount(book.getAskLevelCount());
            ss.setOrderCount(book.getTotalOrderCount());
            ss.setBestBid(book.getBestBidPrice() != null ? String.valueOf(book.getBestBidPrice()) : "N/A");
            ss.setBestAsk(book.getBestAskPrice() != null ? String.valueOf(book.getBestAskPrice()) : "N/A");
            statuses.add(ss);
        }
        resp.setSymbolStatuses(statuses);

        return resp;
    }

    // ==================== 私有方法 ====================

    private void validateRequest(MatchingRequest request) {
        if (request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new IllegalArgumentException("合约代码不能为空");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("用户 ID 无效");
        }
        if (request.getDirection() == null || request.getDirection().isBlank()) {
            throw new IllegalArgumentException("买卖方向不能为空");
        }
        if (request.getOrderType() == null || request.getOrderType().isBlank()) {
            throw new IllegalArgumentException("订单类型不能为空");
        }
        if (request.getVolume() == null || request.getVolume() <= 0) {
            throw new IllegalArgumentException("手数必须为正整数");
        }
    }
}
