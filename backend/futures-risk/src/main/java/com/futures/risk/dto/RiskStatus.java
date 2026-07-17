package com.futures.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户风险状态。
 * <p>包含风险度、权益、持仓明细等实时风控信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskStatus {

    /** 用户ID */
    private Long userId;

    /** 总权益 */
    private BigDecimal equity;

    /** 可用资金 */
    private BigDecimal availableFunds;

    /** 已占用保证金（frozen + margin） */
    private BigDecimal usedMargin;

    /** 已冻结保证金（未成交订单占用） */
    private BigDecimal frozenMargin;

    /** 已成交占用保证金 */
    private BigDecimal occupiedMargin;

    /** 浮动盈亏 */
    private BigDecimal floatPnl;

    /** 风险度（百分比） */
    private BigDecimal riskRatio;

    /** 风险等级：SAFE/ATTENTION/WARNING/DANGER/CRITICAL */
    private String riskLevel;

    /** 持仓数量 */
    private int positionCount;

    /** 持仓明细（按品种） */
    private List<PositionRisk> positions;

    /**
     * 单品种持仓风险。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionRisk {
        /** 合约代码 */
        private String symbol;

        /** 持仓手数 */
        private int volume;

        /** 方向：LONG/SHORT */
        private String direction;

        /** 开仓均价 */
        private BigDecimal avgPrice;

        /** 最新价 */
        private BigDecimal lastPrice;

        /** 浮动盈亏 */
        private BigDecimal unrealizedPnl;

        /** 占用保证金 */
        private BigDecimal marginUsed;
    }
}
