package com.futures.risk;

import com.futures.common.exception.BizException;
import com.futures.risk.dto.PreCheckRequest;
import com.futures.risk.dto.RiskCheckResult;
import com.futures.risk.entity.PositionLimitConfigEntity;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.PositionLimitConfigMapper;
import com.futures.risk.mapper.RiskAlertMapper;
import com.futures.risk.mapper.RiskConfigMapper;
import com.futures.risk.service.MarginCalculator;
import com.futures.risk.service.RiskCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RiskCheckService.preCheck() 单元测试。
 * <p>验证返回值版的前置风控校验方法。</p>
 */
class RiskCheckServicePreCheckTest {

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

    @Test
    void testPreCheck_Passed() {
        when(positionLimitConfigMapper.selectOne(any())).thenReturn(null);

        PreCheckRequest request = PreCheckRequest.builder()
                .userId(1L)
                .symbol("ES")
                .direction("BUY")
                .volume(10)
                .price(new BigDecimal("4000"))
                .availableFunds(new BigDecimal("50000"))
                .build();

        RiskCheckResult result = riskCheckService.preCheck(request);

        assertTrue(result.isPassed(), "前置校验应通过");
        assertEquals(200, result.getCode(), "状态码应为 200");
        assertEquals("校验通过", result.getReason());
        assertNotNull(result.getRequiredMargin(), "应计算保证金");
        // ES 1手: 4000*50*1*0.05=10000, 10手: 100000
        assertEquals(0, new BigDecimal("100000.00").compareTo(result.getRequiredMargin()),
                "保证金应为 100000");
    }

    @Test
    void testPreCheck_Failed_ZeroPrice() {
        PreCheckRequest request = PreCheckRequest.builder()
                .userId(1L)
                .symbol("ES")
                .direction("BUY")
                .volume(10)
                .price(BigDecimal.ZERO)
                .build();

        RiskCheckResult result = riskCheckService.preCheck(request);

        assertFalse(result.isPassed(), "零价格应不通过");
        assertTrue(result.getReason().contains("大于 0"), "应提示价格错误");
    }

    @Test
    void testPreCheck_Failed_ExceedsMaxOrder() {
        PositionLimitConfigEntity config = new PositionLimitConfigEntity();
        config.setMaxOrderVolume(5);
        config.setMaxPositionVolume(100);
        config.setEnabled(true);
        when(positionLimitConfigMapper.selectOne(any())).thenReturn(config);

        PreCheckRequest request = PreCheckRequest.builder()
                .userId(1L)
                .symbol("ES")
                .direction("BUY")
                .volume(10)
                .price(new BigDecimal("4000"))
                .build();

        RiskCheckResult result = riskCheckService.preCheck(request);

        assertFalse(result.isPassed(), "超限额应不通过");
        assertTrue(result.getReason().contains("超限"), "应提示超限");
        assertEquals(4003, result.getCode(), "错误码应为 4003（持仓超限）");
    }

    @Test
    void testPreCheck_Failed_ZeroVolume() {
        PreCheckRequest request = PreCheckRequest.builder()
                .userId(1L)
                .symbol("ES")
                .direction("BUY")
                .volume(0)
                .price(new BigDecimal("4000"))
                .build();

        RiskCheckResult result = riskCheckService.preCheck(request);

        assertFalse(result.isPassed(), "零手数应不通过");
        assertTrue(result.getReason().contains("大于 0"), "应提示手数错误");
    }

    @Test
    void testPreCheck_NegativePrice() {
        PreCheckRequest request = PreCheckRequest.builder()
                .userId(1L)
                .symbol("ES")
                .direction("BUY")
                .volume(10)
                .price(new BigDecimal("-100"))
                .build();

        RiskCheckResult result = riskCheckService.preCheck(request);

        assertFalse(result.isPassed(), "负价格应不通过");
    }
}
