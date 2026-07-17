package com.futures.risk;

import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskConfigMapper;
import com.futures.risk.service.MarginCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MarginCalculator 单元测试。
 * <p>验证初始保证金计算、风险度计算、品种级配置查找、波动率加成等逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
class MarginCalculatorTest {

    @Mock
    private RiskConfigMapper riskConfigMapper;

    private MarginCalculator marginCalculator;

    @BeforeEach
    void setUp() {
        marginCalculator = new MarginCalculator(riskConfigMapper);
    }

    @Test
    void testCalcInitialMargin_DefaultRate() {
        // ES 默认保证金率 5%，合约乘数 50
        BigDecimal margin = marginCalculator.calcInitialMargin("ES", 1, new BigDecimal("4000"), BigDecimal.ZERO);
        // 4000 * 50 * 1 * 0.05 = 10000
        assertEquals(0, new BigDecimal("10000.00").compareTo(margin), "ES 1手保证金应为 10000");
    }

    @Test
    void testCalcInitialMargin_GC() {
        // GC 默认保证金率 8%，合约乘数 100
        BigDecimal margin = marginCalculator.calcInitialMargin("GC", 2, new BigDecimal("2000"), BigDecimal.ZERO);
        // 2000 * 100 * 2 * 0.08 = 32000
        assertEquals(0, new BigDecimal("32000.00").compareTo(margin), "GC 2手保证金应为 32000");
    }

    @Test
    void testCalcInitialMargin_WithVolatilitySurcharge() {
        // ES 1手，价格 4000，波动率加成 0.2
        BigDecimal margin = marginCalculator.calcInitialMargin("ES", 1, new BigDecimal("4000"), new BigDecimal("0.2"));
        // 4000 * 50 * 1 * (0.05 + 0.2*0.01) = 4000 * 50 * 1 * 0.052 = 10400
        assertEquals(0, new BigDecimal("10400.00").compareTo(margin), "含波动率加成保证金应为 10400");
    }

    @Test
    void testCalcRiskRatio_Normal() {
        BigDecimal ratio = marginCalculator.calcRiskRatio(new BigDecimal("100000"), new BigDecimal("500000"));
        assertEquals(0, new BigDecimal("20.00").compareTo(ratio), "风险度应为 20%");
    }

    @Test
    void testCalcRiskRatio_HighRisk() {
        BigDecimal ratio = marginCalculator.calcRiskRatio(new BigDecimal("400000"), new BigDecimal("500000"));
        assertEquals(0, new BigDecimal("80.00").compareTo(ratio), "风险度应为 80%");
    }

    @Test
    void testCalcRiskRatio_ZeroEquity() {
        BigDecimal ratio = marginCalculator.calcRiskRatio(new BigDecimal("100000"), BigDecimal.ZERO);
        assertEquals(0, new BigDecimal("999.99").compareTo(ratio), "零权益应返回 999.99");
    }

    @Test
    void testCalcMaintenanceMargin() {
        BigDecimal maint = marginCalculator.calcMaintenanceMargin("ES", 1, new BigDecimal("4000"), BigDecimal.ZERO);
        // 10000 * 0.8 = 8000
        assertEquals(0, new BigDecimal("8000.00").compareTo(maint), "维持保证金应为 8000");
    }

    @Test
    void testGetMarginRate_Default() {
        BigDecimal rate = marginCalculator.getMarginRate("ES");
        assertEquals(0, new BigDecimal("0.05").compareTo(rate), "ES 默认保证金率应为 0.05");
    }

    @Test
    void testGetContractMultiplier_Default() {
        BigDecimal multiplier = marginCalculator.getContractMultiplier("ES");
        assertEquals(0, new BigDecimal("50").compareTo(multiplier), "ES 默认合约乘数应为 50");
    }

    @Test
    void testGetMarginRate_FromConfig() {
        RiskConfigEntity config = new RiskConfigEntity();
        config.setSymbol("ES");
        config.setMarginRate(new BigDecimal("0.03"));
        config.setContractMultiplier(new BigDecimal("60"));
        config.setPositionLimit(200);
        config.setWarningRatio(new BigDecimal("75.00"));
        config.setLiquidationRatio(new BigDecimal("110.00"));
        config.setEnabled(true);

        when(riskConfigMapper.selectOne(any())).thenReturn(config);

        // 触发缓存加载
        BigDecimal rate = marginCalculator.getMarginRate("ES");
        assertEquals(0, new BigDecimal("0.03").compareTo(rate), "数据库配置的保证金率应为 0.03");

        BigDecimal multiplier = marginCalculator.getContractMultiplier("ES");
        assertEquals(0, new BigDecimal("60").compareTo(multiplier), "数据库配置的合约乘数应为 60");

        int limit = marginCalculator.getPositionLimit("ES");
        assertEquals(200, limit, "数据库配置的持仓限额应为 200");

        BigDecimal warn = marginCalculator.getWarningRatio("ES");
        assertEquals(0, new BigDecimal("75.00").compareTo(warn), "数据库配置的预警阈值应为 75%");

        BigDecimal liq = marginCalculator.getLiquidationRatio("ES");
        assertEquals(0, new BigDecimal("110.00").compareTo(liq), "数据库配置的强平阈值应为 110%");
    }

    @Test
    void testRefreshConfigCache() {
        marginCalculator.refreshConfigCache();
        // 验证缓存已清空（调用后重新从数据库加载默认值）
        BigDecimal rate = marginCalculator.getMarginRate("ES");
        assertEquals(0, new BigDecimal("0.05").compareTo(rate), "刷新后应使用默认值");
    }

    @Test
    void testCalcInitialMargin_VolumeMultiplier() {
        // ES 5手
        BigDecimal margin = marginCalculator.calcInitialMargin("ES", 5, new BigDecimal("4000"), BigDecimal.ZERO);
        // 4000 * 50 * 5 * 0.05 = 50000
        assertEquals(0, new BigDecimal("50000.00").compareTo(margin), "ES 5手保证金应为 50000");
    }
}
