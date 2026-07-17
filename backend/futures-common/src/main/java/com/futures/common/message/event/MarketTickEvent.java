package com.futures.common.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行情Tick事件。
 * 通过Kafka高吞吐分发，行情服务 → 风控引擎/WebSocket终端。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketTickEvent {

    /** 合约代码 */
    private String symbol;

    /** 交易所 */
    private String exchange;

    /** 最新成交价 */
    private BigDecimal lastPrice;

    /** 买一价 */
    private BigDecimal bidPrice;

    /** 买一量 */
    private long bidVolume;

    /** 卖一价 */
    private BigDecimal askPrice;

    /** 卖一量 */
    private long askVolume;

    /** 当日开盘价 */
    private BigDecimal openPrice;

    /** 当日最高价 */
    private BigDecimal highPrice;

    /** 当日最低价 */
    private BigDecimal lowPrice;

    /** 上一交易日收盘价 */
    private BigDecimal preClosePrice;

    /** 成交量 */
    private long volume;

    /** 成交额 */
    private BigDecimal turnover;

    /** 持仓量 */
    private BigDecimal openInterest;

    /** 涨跌幅 */
    private BigDecimal changePercent;

    /** 行情时间 */
    private LocalDateTime timestamp;
}
