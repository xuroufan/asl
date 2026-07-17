package com.futures.matching.engine;

import com.futures.matching.model.MatchResult;
import com.futures.matching.model.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 订单撮合结果 — 聚合单个订单的所有成交信息。
 * <p>
 * 由 {@link MatchingEngine#processOrder(Order)} 返回，
 * 封装了原始订单 + 所有逐笔成交记录 + 自动计算的汇总信息。
 * </p>
 */
@Getter
public class MatchedOrder {

    /** 原始订单（主动单） */
    private final Order order;

    /** 所有逐笔成交记录（可能为空） */
    private final List<MatchResult> trades;

    /** 总成交数量 */
    private final int totalFilledVolume;

    /** 成交均价（BigDecimal） */
    private final BigDecimal avgPrice;

    /** 是否有成交 */
    private final boolean hasTrades;

    /** 最终状态描述：FILLED / PARTIAL / PENDING / CANCELLED / REJECTED */
    private final String finalStatus;

    public MatchedOrder(Order order, List<MatchResult> trades) {
        this.order = order;
        this.trades = trades != null ? Collections.unmodifiableList(new ArrayList<>(trades)) : Collections.emptyList();
        this.hasTrades = !this.trades.isEmpty();

        // 计算总成交量和均价
        int filled = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (MatchResult t : this.trades) {
            filled += t.getVolume();
            totalValue = totalValue.add(t.getPriceAsBigDecimal().multiply(BigDecimal.valueOf(t.getVolume())));
        }
        this.totalFilledVolume = filled;

        if (filled > 0) {
            this.avgPrice = totalValue.divide(BigDecimal.valueOf(filled), 4, RoundingMode.HALF_UP);
        } else {
            this.avgPrice = BigDecimal.ZERO;
        }

        // 确定最终状态
        if (filled == 0) {
            this.finalStatus = "PENDING";
        } else if (filled >= order.getVolume()) {
            this.finalStatus = "FILLED";
        } else {
            this.finalStatus = "PARTIAL";
        }
    }

    /** 剩余未成交数量 */
    public int getRemainingVolume() {
        return order.getVolume() - totalFilledVolume;
    }

    /** 是否全部成交 */
    public boolean isFullyFilled() {
        return totalFilledVolume >= order.getVolume();
    }

    /** 订单ID（主动单） */
    public String getOrderId() {
        return order.getOrderId();
    }

    /** 合约代码 */
    public String getSymbol() {
        return order.getSymbol();
    }

    // ==================== Trade 兼容视图 ====================

    /**
     * 成交记录（兼容旧模型 MatchResult.Trade 的字段）。
     */
    @Getter
    public static class Trade {
        private final String buyOrderId;
        private final String sellOrderId;
        private final long buyerId;
        private final long sellerId;
        private final BigDecimal price;
        private final int volume;
        private final long tradedAtNanos;

        public Trade(MatchResult mr) {
            this.buyOrderId = null;
            this.sellOrderId = null;
            this.buyerId = 0;
            this.sellerId = 0;
            this.price = mr.getPriceAsBigDecimal();
            this.volume = mr.getVolume();
            this.tradedAtNanos = mr.getTimestamp();
        }
    }

    /** 返回所有成交记录（MatchResult 格式，供事件日志使用） */
    public List<MatchResult> getTrades() {
        return trades != null ? trades : Collections.emptyList();
    }

    /** 返回成交记录 Trade 视图（供 MQ/REST 使用） */
    public List<Trade> getTradeInfo() {
        List<Trade> result = new ArrayList<>(trades.size());
        for (MatchResult t : trades) {
            result.add(new Trade(t));
        }
        return result;
    }
}
