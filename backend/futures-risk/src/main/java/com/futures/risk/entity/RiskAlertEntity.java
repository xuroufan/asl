package com.futures.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 风控预警记录 — t_risk_alert 表
 */
@Data
@TableName("t_risk_alert")
public class RiskAlertEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private String userId;

    /** 预警级别 INFO / WARN / CRITICAL */
    private String level;

    /** 预警类型 */
    private String alertType;

    /** 当前风险度 */
    private java.math.BigDecimal riskRatio;

    /** 预警消息 */
    private String message;

    /** 处理状态 0=未处理 1=已处理 2=误报 */
    private Integer handleStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
