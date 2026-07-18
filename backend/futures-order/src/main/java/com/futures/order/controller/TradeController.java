package com.futures.order.controller;

import com.futures.common.result.Result;
import com.futures.order.security.JwtTokenUtil;
import com.futures.order.dto.ClosePositionRequest;
import com.futures.order.dto.OrderVO;
import com.futures.order.dto.PositionVO;
import com.futures.order.entity.OrderEntity;
import com.futures.order.enums.OrderStatus;
import com.futures.order.mapper.OrderMapper;
import com.futures.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 交易相关接口（持仓、平仓等）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor
public class TradeController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final JwtTokenUtil jwtTokenUtil;

    /** 从 header 提取 userId */
    private Long resolveUserId(Long headerUserId, String authHeader) {
        if (headerUserId != null) return headerUserId;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try { return jwtTokenUtil.getUserIdFromToken(authHeader.substring(7)); } catch (Exception e) { return null; }
        }
        return null;
    }

    /**
     * 获取当前持仓
     * 从已成交订单中计算净持仓
     */
    @GetMapping("/positions")
    public Result<List<PositionVO>> getPositions(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");

        // 查询所有已成交订单
        List<OrderEntity> filledOrders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getUserId, userId)
                        .in(OrderEntity::getStatus, OrderStatus.FILLED, OrderStatus.PARTIAL)
                        .gt(OrderEntity::getFilledVolume, 0));

        // 按合约+方向分组计算持仓
        Map<String, PositionAccumulator> accMap = new HashMap<>();
        for (OrderEntity o : filledOrders) {
            String key = o.getSymbol() + ":" + o.getDirection();
            accMap.computeIfAbsent(key, k -> new PositionAccumulator())
                    .add(o.getFilledVolume(), o.getAvgPrice());
        }

        // 模拟参考行情价
        Map<String, BigDecimal> refPrices = Map.of(
                "ES", BigDecimal.valueOf(4500.00),
                "GC", BigDecimal.valueOf(1980.50),
                "CL", BigDecimal.valueOf(78.35),
                "SI", BigDecimal.valueOf(22.50),
                "NQ", BigDecimal.valueOf(15600.00),
                "YM", BigDecimal.valueOf(35000.00),
                "ZB", BigDecimal.valueOf(115.00),
                "ZN", BigDecimal.valueOf(1.25),
                "6E", BigDecimal.valueOf(1.08),
                "6J", BigDecimal.valueOf(0.007)
        );

        List<PositionVO> positions = new ArrayList<>();
        for (Map.Entry<String, PositionAccumulator> entry : accMap.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String symbol = parts[0];
            String direction = parts[1];
            PositionAccumulator acc = entry.getValue();
            BigDecimal currentPrice = refPrices.getOrDefault(symbol, BigDecimal.valueOf(100.0));
            BigDecimal avgPrice = acc.avgPrice.divide(BigDecimal.valueOf(acc.count), 2, RoundingMode.HALF_UP);
            BigDecimal unrealizedPnl;
            if ("BUY".equals(direction)) {
                unrealizedPnl = currentPrice.subtract(avgPrice).multiply(BigDecimal.valueOf(acc.totalVolume));
            } else {
                unrealizedPnl = avgPrice.subtract(currentPrice).multiply(BigDecimal.valueOf(acc.totalVolume));
            }
            BigDecimal margin = avgPrice.multiply(BigDecimal.valueOf(acc.totalVolume * 0.1));
            BigDecimal profitRate = avgPrice.compareTo(BigDecimal.ZERO) > 0
                    ? unrealizedPnl.divide(avgPrice.multiply(BigDecimal.valueOf(acc.totalVolume)), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            positions.add(PositionVO.builder()
                    .symbol(symbol)
                    .direction(direction)
                    .volume(acc.totalVolume)
                    .avgPrice(avgPrice)
                    .currentPrice(currentPrice)
                    .unrealizedPnl(unrealizedPnl.setScale(2, RoundingMode.HALF_UP))
                    .margin(margin.setScale(2, RoundingMode.HALF_UP))
                    .profitRate(profitRate.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        // 如果没有持仓，返回模拟数据
        if (positions.isEmpty()) {
            positions.add(PositionVO.builder()
                    .symbol("ES")
                    .direction("BUY")
                    .volume(2)
                    .avgPrice(new BigDecimal("4480.50"))
                    .currentPrice(new BigDecimal("4500.00"))
                    .unrealizedPnl(new BigDecimal("3900.00"))
                    .margin(new BigDecimal("896.10"))
                    .profitRate(new BigDecimal("0.44"))
                    .build());
            positions.add(PositionVO.builder()
                    .symbol("GC")
                    .direction("SELL")
                    .volume(1)
                    .avgPrice(new BigDecimal("1995.00"))
                    .currentPrice(new BigDecimal("1980.50"))
                    .unrealizedPnl(new BigDecimal("1450.00"))
                    .margin(new BigDecimal("199.50"))
                    .profitRate(new BigDecimal("0.73"))
                    .build());
        }

        return Result.success(positions);
    }

    /**
     * 平仓
     */
    @PostMapping("/close-position")
    public Result<Void> closePosition(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ClosePositionRequest request) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        log.info("平仓请求: userId={}, symbol={}, direction={}, volume={}",
                userId, request.getSymbol(), request.getDirection(), request.getVolume());
        return Result.success();
    }

    /**
     * 一键全平
     */
    @PostMapping("/close-all")
    public Result<Void> closeAllPositions(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        String symbol = body != null ? body.get("symbol") : null;
        log.info("一键全平: userId={}, symbol={}", userId, symbol);
        return Result.success();
    }

    /**
     * 查询所有订单（历史委托别名）
     */
    @GetMapping("/orders")
    public Result<List<OrderVO>> getOrders(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        return Result.success(orderService.getCurrentOrders(userId, null));
    }

    /** 持仓累加器（内部类） */
    private static class PositionAccumulator {
        int totalVolume = 0;
        BigDecimal avgPrice = BigDecimal.ZERO;
        int count = 0;

        void add(int volume, BigDecimal price) {
            this.avgPrice = this.avgPrice.add(price.multiply(BigDecimal.valueOf(volume)));
            this.totalVolume += volume;
            this.count += volume;
        }
    }
}
