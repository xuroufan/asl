package com.futures.matching.controller;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 五档盘口深度响应 DTO。
 * <p>
 * REST 接口 {@code GET /api/v1/matching/depth} 的响应体。</p>
 */
@Data
public class DepthResponse {

    /** 合约代码 */
    private String symbol;

    /** 买盘（从高到低排序） */
    private List<DepthLevel> bids;

    /** 卖盘（从低到高排序） */
    private List<DepthLevel> asks;

    /** 最优买价 */
    private Long bestBidPrice;

    /** 最优卖价 */
    private Long bestAskPrice;

    /** 盘口的单个价格级别 */
    @Data
    public static class DepthLevel {
        /** 价格 */
        private BigDecimal price;
        /** 该价格的总数量 */
        private long volume;
        /** 该价格的订单数量 */
        private int orderCount;
    }
}
