package com.futures.risk;

import com.futures.risk.dto.RiskStatus;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskAlertMapper;
import com.futures.risk.mapper.RiskConfigMapper;
import com.futures.risk.service.MarginCalculator;
import com.futures.risk.service.RiskCheckService;
import com.futures.risk.service.RiskMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RiskMonitorService.calculateRisk() 单元测试。
 */
class RiskMonitorServiceTest {

    private RiskAlertMapper riskAlertMapper;
    private RiskConfigMapper riskConfigMapper;
    private MarginCalculator marginCalculator;
    private RiskCheckService riskCheckService;
    private RiskMonitorService riskMonitorService;

    @BeforeEach
    void setUp() {
        riskAlertMapper = mock(RiskAlertMapper.class);
        riskConfigMapper = mock(RiskConfigMapper.class);
        riskCheckService = mock(RiskCheckService.class);
        marginCalculator = new MarginCalculator(riskConfigMapper);
        riskMonitorService = new RiskMonitorService(riskAlertMapper, riskCheckService, marginCalculator);
    }

    @Test
    void testCalculateRisk_NoData() {
        RiskStatus status = riskMonitorService.calculateRisk(1L);

        assertNotNull(status, "不应返回 null");
        assertEquals(1L, status.getUserId());
        assertEquals(BigDecimal.ZERO, status.getEquity(), "无数据时权益应为 0");
        assertEquals(BigDecimal.ZERO, status.getUsedMargin(), "无数据时保证金应为 0");
        assertEquals(0, status.getPositionCount(), "无数据时持仓应为 0");
        assertEquals("SAFE", status.getRiskLevel(), "无数据时等级应为 SAFE");
    }

    @Test
    void testCalculateRisk_WithFundSnapshot() {
        // 更新资金快照
        riskMonitorService.updateFundSnapshot(1L,
                new BigDecimal("100000"),  // usedMargin
                new BigDecimal("500000"),  // equityWithLoan
                new BigDecimal("400000")); // availableFunds

        RiskStatus status = riskMonitorService.calculateRisk(1L);

        assertNotNull(status);
        assertEquals(1L, status.getUserId());
        assertEquals(new BigDecimal("500000"), status.getEquity(), "权益应等于 equityWithLoan");
        assertEquals(new BigDecimal("100000"), status.getUsedMargin(), "占用保证金应为 100000");
        assertEquals(new BigDecimal("400000"), status.getAvailableFunds(), "可用资金应为 400000");
        assertEquals(0, status.getPositionCount(), "无持仓时持仓数应为 0");
        // 风险度 = 100000 / 500000 * 100 = 20.00%
        assertEquals(0, new BigDecimal("20.00").compareTo(status.getRiskRatio()),
                "风险度应为 20%");
    }

    @Test
    void testCalcRiskRatio_WithPositions() {
        // 更新资金快照
        riskMonitorService.updateFundSnapshot(1L,
                new BigDecimal("500000"),  // usedMargin
                new BigDecimal("1000000"), // equityWithLoan
                new BigDecimal("500000")); // availableFunds

        // 更新持仓快照
        riskMonitorService.updatePositionSnapshot(1L, "ES", 10,
                new BigDecimal("4000"), new BigDecimal("4200"));

        RiskStatus status = riskMonitorService.calculateRisk(1L);

        assertNotNull(status);
        assertEquals(1, status.getPositionCount(), "应有 1 个持仓");
        assertEquals("ES", status.getPositions().get(0).getSymbol());
        assertEquals(10, status.getPositions().get(0).getVolume());
        // 风险度 = 500000 / 1000000 * 100 = 50.00%
        assertEquals(0, new BigDecimal("50.00").compareTo(status.getRiskRatio()),
                "风险度应为 50%");
    }

    @Test
    void testCalculateRisk_WithMultiplePositions() {
        // 更新资金快照：高杠杆场景
        riskMonitorService.updateFundSnapshot(1L,
                new BigDecimal("900000"),  // usedMargin
                new BigDecimal("1000000"), // equityWithLoan
                new BigDecimal("100000")); // availableFunds

        // 更新多个持仓
        riskMonitorService.updatePositionSnapshot(1L, "ES", 5,
                new BigDecimal("4000"), new BigDecimal("3800"));
        riskMonitorService.updatePositionSnapshot(1L, "GC", 3,
                new BigDecimal("2000"), new BigDecimal("1900"));

        RiskStatus status = riskMonitorService.calculateRisk(1L);

        assertNotNull(status);
        assertEquals(2, status.getPositionCount(), "应有 2 个持仓");
        // 风险度 = 900000 / 1000000 * 100 = 90.00% → WARNING
        assertEquals("WARNING", status.getRiskLevel(), "风险度 90% 应为 WARNING");
    }

    @Test
    void testGetCurrentRiskRatio_NoData() {
        BigDecimal ratio = riskMonitorService.getCurrentRiskRatio(1L);
        assertEquals(BigDecimal.ZERO, ratio, "无数据时风险度应为 0");
    }

    @Test
    void testGetCurrentRiskRatio_HasData() {
        riskMonitorService.updateFundSnapshot(1L,
                new BigDecimal("300000"),
                new BigDecimal("500000"),
                new BigDecimal("200000"));

        BigDecimal ratio = riskMonitorService.getCurrentRiskRatio(1L);
        // 300000 / 500000 * 100 = 60.00
        assertEquals(0, new BigDecimal("60.00").compareTo(ratio),
                "风险度应为 60%");
    }

    @Test
    void testUpdateFundSnapshotFull() {
        riskMonitorService.updateFundSnapshotFull(1L,
                new BigDecimal("200000"),
                new BigDecimal("800000"),
                new BigDecimal("500000"),
                new BigDecimal("100000"));

        RiskStatus status = riskMonitorService.calculateRisk(1L);

        assertEquals(new BigDecimal("100000"), status.getFrozenMargin(), "冻结保证金应为 100000");
        assertEquals(0, new BigDecimal("25.00").compareTo(status.getRiskRatio()),
                "风险度应为 25%");
    }
}
