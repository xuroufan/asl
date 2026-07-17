package com.futures.matching.controller;

import com.futures.common.result.Result;
import com.futures.matching.disruptor.DisruptorConfig;
import com.futures.matching.engine.MatchedOrder;
import com.futures.matching.engine.MatchingEngine;
import com.futures.matching.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 撮合引擎 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
@Tag(name = "撮合引擎", description = "订单撮合相关接口")
public class MatchingController {

    private final MatchingEngine matchingEngine;
    private final DisruptorConfig disruptorConfig;

    @PostMapping("/place")
    @Operation(summary = "提交订单撮合")
    public Result<MatchingResponse> placeOrder(@RequestBody PlaceRequest request) {
        Order order = Order.builder()
                .orderId(request.getOrderId() != null ? request.getOrderId()
                        : "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .userId(request.getUserId())
                .symbol(request.getSymbol() != null ? request.getSymbol() : "HSI")
                .direction(Order.Direction.valueOf(request.getDirection().toUpperCase()))
                .type(Order.OrderType.valueOf(request.getOrderType().toUpperCase()))
                .price(request.getPrice())
                .volume(request.getVolume())
                .filledVolume(0)
                .timestamp(System.nanoTime())
                .build();

        if (request.isSync()) {
            MatchedOrder result = matchingEngine.processOrder(order);
            return Result.success(MatchingResponse.from(result));
        }

        // 异步模式
        disruptorConfig.publishOrder(order);
        MatchingResponse resp = new MatchingResponse();
        resp.setOrderId(order.getOrderId());
        resp.setFinalStatus("PENDING");
        return Result.success(resp);
    }

    @PostMapping("/cancel")
    @Operation(summary = "撤单")
    public Result<String> cancelOrder(@RequestParam String symbol,
                                      @RequestParam String orderId) {
        disruptorConfig.publishCancel(symbol, orderId);
        return Result.success("撤单指令已发送: " + orderId);
    }

    @GetMapping("/price")
    @Operation(summary = "获取当前中间价")
    public Result<Long> getMidPrice() {
        return Result.success(matchingEngine.getCurrentMidPrice());
    }

    @GetMapping("/depth")
    @Operation(summary = "获取订单簿深度")
    public Result<DepthView> getDepth() {
        com.futures.matching.engine.OrderBook book = matchingEngine.getOrderBook();
        DepthView view = new DepthView();
        view.setBidLevels(book.getBidLevelCount());
        view.setAskLevels(book.getAskLevelCount());
        view.setTotalOrders(book.getTotalOrderCount());
        view.setBestBid(book.getBestBidPrice());
        view.setBestAsk(book.getBestAskPrice());
        return Result.success(view);
    }

    // ==================== DTO ====================

    @Data
    public static class PlaceRequest {
        private String orderId;
        private String userId;
        private String symbol;         // 合约代码
        private String direction;      // BUY / SELL
        private String orderType;      // LIMIT / MARKET / STOP / FOK / IOC
        private long price;
        private int volume;
        private boolean sync;          // true=同步模式，false=异步Disruptor
    }

    @Data
    public static class DepthView {
        private int bidLevels;
        private int askLevels;
        private int totalOrders;
        private Long bestBid;
        private Long bestAsk;
    }
}
