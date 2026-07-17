package com.futures.market.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** K线数据 */
@Data
public class KlineEntity {
    private String symbol;
    private String interval;    // 1m, 5m, 15m, 30m, 1h, 1d, 1w
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private Long timestamp;
}
