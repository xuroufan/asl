package com.futures.risk.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控预警事件（MQ 消息）。
 * <p>当用户风险度达到预警/强平阈值时，发送此消息到 risk-alert Topic。
 * 通知服务消费后推送至终端（WebSocket）、App推送或短信。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAlertEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件ID（UUID） */
    private String eventId;

    /** 用户ID */
    private Long userId;

    /** 预警类型：MARGIN_WARN / NO_NEW_POSITION / FORCE_LIQUIDATION / DAILY_LOSS */
    private String alertType;

    /** 预警级别：INFO / WARN / CRITICAL */
    private String alertLevel;

    /** 触发时的合约代码（可为 null） */
    private String symbol;

    /** 风险度 */
    private BigDecimal riskRatio;

    /** 总权益 */
    private BigDecimal equity;

    /** 已占用保证金 */
    private BigDecimal usedMargin;

    /** 消息内容 */
    private String message;

    /** 触发时间 */
    private LocalDateTime timestamp;
}
