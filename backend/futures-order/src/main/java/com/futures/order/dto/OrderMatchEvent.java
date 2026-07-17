package com.futures.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 撮合成交事件 — MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMatchEvent {

    /** 订单号 */
    private String orderId;

    /** 本次成交数量 */
    private Integer matchVolume;

    /** 本次成交价格 */
    private BigDecimal matchPrice;

    /** 累计已成交数量 */
    private Integer totalFilledVolume;

    /** 成交均价 */
    private BigDecimal avgPrice;

    /** 成交后订单状态 */
    private String newStatus;     // PARTIAL / FILLED

    /** 成交时间 */
    private LocalDateTime matchedAt;
}
