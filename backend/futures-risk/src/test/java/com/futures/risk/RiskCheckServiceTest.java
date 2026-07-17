package com.futures.risk;

import com.futures.common.exception.BizException;
import com.futures.risk.entity.PositionLimitConfigEntity;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.PositionLimitConfigMapper;
import com.futures.risk.mapper.RiskAlertMapper;
import com.futures.risk.mapper.RiskConfigMapper;
import com.futures.risk.service.MarginCalculator;
import com.futures.risk.service.RiskCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RiskCheckService 单元测试。
 * <p>使用 Mockito 仅 mock 接口类（MyBatis-Plus Mapper），
 * 其他具体服务类使用真实实例。</p>
 */
class RiskCheckServiceTest {

    private PositionLimitConfigMapper positionLimitConfigMapper;
    private RiskAlertMapper riskAlertMapper;
    private RiskConfigMapper riskConfigMapper;
    private MarginCalculator marginCalculator;
    private RiskCheckService riskCheckService;

    @BeforeEach
    void setUp() {
        positionLimitConfigMapper = mock(PositionLimitConfigMapper.class);
        riskAlertMapper = mock(RiskAlertMapper.class);
        riskConfigMapper = mock(RiskConfigMapper.class);
        marginCalculator = new MarginCalculator(riskConfigMapper);
        riskCheckService = new RiskCheckService(positionLimitConfigMapper, riskAlertMapper, marginCalculator);
    }

    // ==================== 验仓 ====================

    @Test
    void testCheckPositionLimit_Pass() {
        when(positionLimitConfigMapper.selectOne(any())).thenReturn(null);
        assertDoesNotThrow(() -> riskCheckService.checkPositionLimit(1L, "ES", 10));
    }

    @Test
    void testCheckPositionLimit_ExceedsMaxOrder() {
        PositionLimitConfigEntity config = new PositionLimitConfigEntity();
        config.setMaxOrderVolume(5);
        config.setMaxPositionVolume(100);
        config.setEnabled(true);
        when(positionLimitConfigMapper.selectOne(any())).thenReturn(config);

        BizException ex = assertThrows(BizException.class,
                () -> riskCheckService.checkPositionLimit(1L, "ES", 10));
        assertTrue(ex.getMessage().contains("超限"), "应提示超限");
    }

    @Test
    void testCheckPositionLimit_ZeroVolume() {
        assertThrows(BizException.class,
                () -> riskCheckService.checkPositionLimit(1L, "ES", 0));
    }

    // ==================== 验资 ====================

    @Test
    void testCheckMarginSufficiency_Pass() {
        // ES 1手, 价格 4000, 默认保证金率 5%, 合约乘数 50
        // 需保证金 = 4000 * 50 * 1 * 0.05 = 10000
        // Mock DB config to return null (use default)
        when(riskConfigMapper.selectOne(any())).thenReturn(null);

        // 可用 50000 > 10000
        assertDoesNotThrow(() ->
                riskCheckService.checkMarginSufficiency(new BigDecimal("50000"), "ES", 1, new BigDecimal("4000"), BigDecimal.ZERO));
    }

    @Test
    void testCheckMarginSufficiency_Insufficient() {
        when(riskConfigMapper.selectOne(any())).thenReturn(null);

        // 可用 5000 < 10000
        assertThrows(BizException.class,
                () -> riskCheckService.checkMarginSufficiency(new BigDecimal("5000"), "ES", 1, new BigDecimal("4000"), BigDecimal.ZERO));
    }

    // ==================== 日内亏损 ====================

    @Test
    void testCheckDailyLossLimit_Pass() {
        assertDoesNotThrow(() ->
                riskCheckService.checkDailyLossLimit(new BigDecimal("-5000"), new BigDecimal("20000")));
    }

    @Test
    void testCheckDailyLossLimit_Breached() {
        assertThrows(BizException.class,
                () -> riskCheckService.checkDailyLossLimit(new BigDecimal("-25000"), new BigDecimal("20000")));
    }

    @Test
    void testCheckDailyLossLimit_Profit() {
        assertDoesNotThrow(() ->
                riskCheckService.checkDailyLossLimit(new BigDecimal("5000"), new BigDecimal("20000")));
    }

    // ==================== 验价 ====================

    @Test
    void testCheckPrice_Valid() {
        assertDoesNotThrow(() -> riskCheckService.checkPrice("ES", new BigDecimal("4000")));
    }

    @Test
    void testCheckPrice_Zero() {
        assertThrows(BizException.class, () -> riskCheckService.checkPrice("ES", BigDecimal.ZERO));
    }

    @Test
    void testCheckPrice_Negative() {
        assertThrows(BizException.class, () -> riskCheckService.checkPrice("ES", new BigDecimal("-100")));
    }

    // ==================== 前置风控校验 ====================

    @Test
    void testPreTradeCheck_Pass() {
        when(positionLimitConfigMapper.selectOne(any())).thenReturn(null);
        assertDoesNotThrow(() ->
                riskCheckService.preTradeCheck(1L, "ES", "BUY", 10, new BigDecimal("4000")));
    }

    @Test
    void testPreTradeCheck_BadPrice() {
        assertThrows(BizException.class,
                () -> riskCheckService.preTradeCheck(1L, "ES", "BUY", 10, BigDecimal.ZERO));
    }

    @Test
    void testPreTradeCheck_ZeroVolume() {
        assertDoesNotThrow(() -> {
            try {
                riskCheckService.preTradeCheck(1L, "ES", "BUY", 0, new BigDecimal("4000"));
                fail("应抛异常");
            } catch (BizException e) {
                assertTrue(e.getMessage().contains("大于 0"));
            }
        });
    }

    // ==================== 盘中风控 ====================

    @Test
    void testCheckIntradayRisk_Safe() {
        when(riskConfigMapper.selectOne(any())).thenReturn(null);
        boolean result = riskCheckService.checkIntradayRisk(1L, new BigDecimal("50.00"), "ES");
        assertFalse(result, "风险度 50% 应安全");
    }

    @Test
    void testCheckIntradayRisk_Warning() {
        when(riskConfigMapper.selectOne(any())).thenReturn(null);
        boolean result = riskCheckService.checkIntradayRisk(1L, new BigDecimal("85.00"), "ES");
        assertFalse(result, "风险度 85% 应仅触发预警");
    }

    @Test
    void testCheckIntradayRisk_NoNewPosition() {
        when(riskConfigMapper.selectOne(any())).thenReturn(null);
        assertThrows(BizException.class,
                () -> riskCheckService.checkIntradayRisk(1L, new BigDecimal("105.00"), "ES"));
    }
}
