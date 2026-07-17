package com.futures.common.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓变动事件。
 * 撮合引擎成交后发送给账户/风控服务更新持仓信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionChangedEvent {

    /** 用户ID */
    private String userId;

    /** 合约代码 */
    private String symbol;

    /** 持仓方向 LONG / SHORT */
    private String direction;

    /** 变动数量（正数增加，负数减少） */
    private int changeVolume;

    /** 变动后总持仓量 */
    private int totalVolume;

    /** 变动后持仓均价 */
    private BigDecimal averagePrice;

    /** 本次成交价格 */
    private BigDecimal matchPrice;

    /** 关联订单ID */
    private String orderId;

    /** 变动类型 OPEN / CLOSE / LIQUIDATION */
    private String changeType;

    /** 变动时间 */
    private LocalDateTime changedAt;
}
