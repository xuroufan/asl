package com.futures.market.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/** 五档盘口 */
@Data
public class MarketDepth {
    private String symbol;
    private List<Level> bids; // 买盘
    private List<Level> asks; // 卖盘

    @Data
    public static class Level {
        private BigDecimal price;
        private BigDecimal quantity;
        private int position;
    }
}
