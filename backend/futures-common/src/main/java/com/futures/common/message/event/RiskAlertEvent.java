package com.futures.common.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控预警事件。
 * 风控引擎检测到风险时发送给通知服务推送用户。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAlertEvent {

    /** 用户ID */
    private String userId;

    /** 预警级别 INFO / WARN / CRITICAL */
    private String level;

    /** 预警类型 MARGIN / POSITION / LIQUIDATION / LOSS */
    private String alertType;

    /** 当前风险度 */
    private BigDecimal riskRatio;

    /** 预警阈值 */
    private BigDecimal threshold;

    /** 预警消息 */
    private String message;

    /** 关联合约 */
    private String symbol;

    /** 是否需要强平 */
    private boolean requireLiquidation;

    /** 预警时间 */
    private LocalDateTime alertedAt;
}
