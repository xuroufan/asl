package com.futures.common.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单成交事件。
 * 撮合引擎撮合成功后发送给订单/账户/风控服务。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMatchedEvent {

    /** 订单ID */
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 合约代码 */
    private String symbol;

    /** 买卖方向 BUY / SELL */
    private String direction;

    /** 成交价格 */
    private BigDecimal price;

    /** 成交数量 */
    private int volume;

    /** 成交金额 */
    private BigDecimal totalAmount;

    /** 手续费 */
    private BigDecimal fee;

    /** 本次成交ID（撮合引擎生成） */
    private String matchId;

    /** 对手订单ID */
    private String counterPartyOrderId;

    /** 成交前状态 PARTIAL / PENDING */
    private String previousStatus;

    /** 成交后状态 FILLED / PARTIAL */
    private String newStatus;

    /** 该订单累计成交量 */
    private int totalFilledVolume;

    /** 该订单累计成交均价 */
    private BigDecimal averagePrice;

    /** 成交时间 */
    private LocalDateTime matchedAt;
}
