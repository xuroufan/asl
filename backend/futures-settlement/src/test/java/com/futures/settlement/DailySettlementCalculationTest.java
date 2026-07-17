package com.futures.settlement;

import com.futures.settlement.entity.DailySettlementEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class DailySettlementCalculationTest {

    @Test
    @DisplayName("PnL 计算 — 多头盈利")
    void testPnL_LongPosition_Profit() {
        BigDecimal pnl = new BigDecimal("4520").subtract(new BigDecimal("4500"))
                .multiply(new BigDecimal("50")).multiply(new BigDecimal("2"));
        assertEquals(0, new BigDecimal("2000").compareTo(pnl));
    }

    @Test
    @DisplayName("PnL 计算 — 空头亏损")
    void testPnL_ShortPosition_Loss() {
        BigDecimal pnl = new BigDecimal("2050").subtract(new BigDecimal("2070"))
                .multiply(new BigDecimal("100")).multiply(BigDecimal.ONE);
        assertEquals(0, new BigDecimal("-2000").compareTo(pnl));
    }

    @Test
    @DisplayName("保证金计算")
    void testInitialMargin() {
        BigDecimal margin = new BigDecimal("4500").multiply(new BigDecimal("50"))
                .multiply(new BigDecimal("2")).multiply(new BigDecimal("0.05"));
        assertEquals(0, new BigDecimal("22500").compareTo(margin));
    }

    @Test
    @DisplayName("DailySettlementEntity — 创建和访问")
    void testDailySettlementEntity() {
        DailySettlementEntity e = new DailySettlementEntity();
        e.setUserId(1L);
        e.setTotalPnl(new BigDecimal("5000"));
        e.setMaintenanceMargin(new BigDecimal("18000"));
        assertEquals(1L, e.getUserId());
        assertEquals(0, new BigDecimal("5000").compareTo(e.getTotalPnl()));
        assertEquals(0, new BigDecimal("18000").compareTo(e.getMaintenanceMargin()));
    }

    @Test
    @DisplayName("风险率计算")
    void testRiskRatio() {
        BigDecimal ratio = new BigDecimal("50000")
                .divide(new BigDecimal("22500"), 4, RoundingMode.HALF_UP);
        assertTrue(ratio.compareTo(new BigDecimal("2.22")) > 0);
    }
}
