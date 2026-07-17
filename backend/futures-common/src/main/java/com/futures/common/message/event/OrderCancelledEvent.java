package com.futures.common.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单取消事件。
 * 用户撤单或系统自动撤单时发送给资金服务解冻保证金。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

    /** 订单ID */
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 合约代码 */
    private String symbol;

    /** 待解冻保证金金额 */
    private String frozenAmount;

    /** 未成交数量 */
    private int remainingVolume;

    /** 取消原因 USER_CANCEL / TIMEOUT / SYSTEM / LIQUIDATION */
    private String cancelReason;

    /** 取消时间 */
    private LocalDateTime cancelledAt;
}
