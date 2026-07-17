package com.futures.matching.controller;

import com.futures.matching.engine.MatchedOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 撮合结果响应 DTO。
 */
@Data
public class MatchingResponse {

    private String orderId;
    private String symbol;
    private String finalStatus;
    private boolean fullyFilled;
    private int totalFilledVolume;
    private int remainingVolume;
    private BigDecimal avgPrice;
    private List<TradeInfo> trades;

    public static MatchingResponse from(MatchedOrder result) {
        if (result == null) return null;

        MatchingResponse resp = new MatchingResponse();
        resp.setOrderId(result.getOrderId());
        resp.setSymbol(result.getSymbol());
        resp.setFinalStatus(result.getFinalStatus());
        resp.setFullyFilled(result.isFullyFilled());
        resp.setTotalFilledVolume(result.getTotalFilledVolume());
        resp.setRemainingVolume(result.getRemainingVolume());
        resp.setAvgPrice(result.getAvgPrice());

        if (result.isHasTrades()) {
            List<TradeInfo> tradeList = new ArrayList<>(result.getTradeInfo().size());
            for (MatchedOrder.Trade trade : result.getTradeInfo()) {
                TradeInfo info = new TradeInfo();
                info.setPrice(trade.getPrice());
                info.setVolume(trade.getVolume());
                info.setTradedAtNanos(trade.getTradedAtNanos());
                tradeList.add(info);
            }
            resp.setTrades(tradeList);
        }

        return resp;
    }

    @Data
    public static class TradeInfo {
        private BigDecimal price;
        private int volume;
        private long tradedAtNanos;
    }
}
