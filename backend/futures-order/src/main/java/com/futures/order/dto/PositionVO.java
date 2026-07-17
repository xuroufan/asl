package com.futures.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 持仓视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionVO {
    private String symbol;
    private String direction;  // "BUY" or "SELL"
    private int volume;
    private BigDecimal avgPrice;
    private BigDecimal currentPrice;
    private BigDecimal unrealizedPnl;
    private BigDecimal margin;
    private BigDecimal profitRate;
}
