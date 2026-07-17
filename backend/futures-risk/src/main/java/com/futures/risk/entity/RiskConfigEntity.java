package com.futures.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控配置实体 — t_risk_config 表
 */
@Data
@TableName("t_risk_config")
public class RiskConfigEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 合约代码 */
    private String symbol;

    /** 保证金率（如 0.10 表示10%） */
    private BigDecimal marginRate;

    /** 持仓限额（手） */
    private Integer positionLimit;

    /** 单笔最大下单手数 */
    private Integer maxOrderVolume;

    /** 预警阈值（如 0.80 表示风险度80%） */
    private BigDecimal warningRatio;

    /** 禁止开仓阈值 */
    private BigDecimal forbidOpenRatio;

    /** 强平阈值（如 1.20 表示风险度120%） */
    private BigDecimal liquidationRatio;

    /** 合约乘数 */
    private Integer contractMultiplier;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    /** 默认配置 */
    public static RiskConfigEntity createDefault(String symbol) {
        RiskConfigEntity config = new RiskConfigEntity();
        config.setSymbol(symbol);
        config.setMarginRate(new BigDecimal("0.10"));
        config.setPositionLimit(1000);
        config.setMaxOrderVolume(100);
        config.setWarningRatio(new BigDecimal("0.80"));
        config.setForbidOpenRatio(new BigDecimal("1.00"));
        config.setLiquidationRatio(new BigDecimal("1.20"));
        config.setContractMultiplier(1);
        return config;
    }
}
