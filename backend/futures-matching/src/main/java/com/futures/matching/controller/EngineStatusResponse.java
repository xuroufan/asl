package com.futures.matching.controller;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 撮合引擎状态响应 DTO。
 * <p>
 * REST 接口 {@code GET /api/v1/matching/status} 的响应体。</p>
 */
@Data
public class EngineStatusResponse {

    /** 当前活跃的品种列表 */
    private List<String> activeSymbols;

    /** 各品种的订单簿概要 */
    private List<SymbolStatus> symbolStatuses;

    @Data
    public static class SymbolStatus {
        /** 合约代码 */
        private String symbol;
        /** 买盘价格级别数 */
        private int bidLevelCount;
        /** 卖盘价格级别数 */
        private int askLevelCount;
        /** 订单总数 */
        private int orderCount;
        /** 最优买价 */
        private String bestBid;
        /** 最优卖价 */
        private String bestAsk;
        /** 最新成交价 */
        private String lastPrice;
    }
}
