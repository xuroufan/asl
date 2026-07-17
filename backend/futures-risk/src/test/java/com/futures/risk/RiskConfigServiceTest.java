package com.futures.risk;

import com.futures.common.exception.BizException;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskConfigMapper;
import com.futures.risk.service.MarginCalculator;
import com.futures.risk.service.RiskConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RiskConfigService 单元测试。
 * <p>验证风控配置的 CRUD 操作和缓存刷新。</p>
 */
class RiskConfigServiceTest {

    private RiskConfigMapper riskConfigMapper;
    private MarginCalculator marginCalculator;
    private RiskConfigService riskConfigService;

    @BeforeEach
    void setUp() {
        riskConfigMapper = mock(RiskConfigMapper.class);
        marginCalculator = new MarginCalculator(riskConfigMapper);
        riskConfigService = new RiskConfigService(riskConfigMapper, marginCalculator);
    }

    @Test
    void testListAll() {
        when(riskConfigMapper.selectList(any())).thenReturn(List.of(
                createConfig(1L, "ES", new BigDecimal("0.05"), 100),
                createConfig(2L, "GC", new BigDecimal("0.08"), 50)));

        List<RiskConfigEntity> configs = riskConfigService.listAll();

        assertEquals(2, configs.size());
    }

    @Test
    void testGetById_Found() {
        RiskConfigEntity config = createConfig(1L, "ES", new BigDecimal("0.05"), 100);
        when(riskConfigMapper.selectById(1L)).thenReturn(config);

        RiskConfigEntity result = riskConfigService.getById(1L);

        assertNotNull(result);
        assertEquals("ES", result.getSymbol());
        assertEquals(0, new BigDecimal("0.05").compareTo(result.getMarginRate()));
    }

    @Test
    void testGetById_NotFound() {
        when(riskConfigMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> riskConfigService.getById(999L));
    }

    @Test
    void testGetBySymbol() {
        RiskConfigEntity config = createConfig(1L, "ES", new BigDecimal("0.05"), 100);
        when(riskConfigMapper.selectOne(any())).thenReturn(config);

        RiskConfigEntity result = riskConfigService.getBySymbol("es");

        assertNotNull(result);
        assertEquals("ES", result.getSymbol());
    }

    @Test
    void testCreate() {
        RiskConfigEntity config = createConfig(null, "CL", new BigDecimal("0.10"), 200);
        when(riskConfigMapper.insert(any())).thenReturn(1);

        riskConfigService.create(config);

        verify(riskConfigMapper, times(1)).insert(any());
    }

    @Test
    void testUpdate() {
        RiskConfigEntity existing = createConfig(1L, "ES", new BigDecimal("0.05"), 100);
        when(riskConfigMapper.selectById(1L)).thenReturn(existing);
        when(riskConfigMapper.updateById(any())).thenReturn(1);

        RiskConfigEntity update = new RiskConfigEntity();
        update.setId(1L);
        update.setMarginRate(new BigDecimal("0.06"));

        riskConfigService.update(update);

        verify(riskConfigMapper, times(1)).updateById(any());
    }

    @Test
    void testDelete() {
        RiskConfigEntity existing = createConfig(1L, "ES", new BigDecimal("0.05"), 100);
        when(riskConfigMapper.selectById(1L)).thenReturn(existing);
        when(riskConfigMapper.deleteById(1L)).thenReturn(1);

        riskConfigService.delete(1L);

        verify(riskConfigMapper, times(1)).deleteById(1L);
    }

    @Test
    void testRefreshCache() {
        riskConfigService.refreshCache();
        // 验证缓存已清空：再次查询应调用 Mapper
        when(riskConfigMapper.selectOne(any())).thenReturn(null);

        BigDecimal rate = marginCalculator.getMarginRate("ES");
        assertEquals(0, new BigDecimal("0.05").compareTo(rate), "刷新后应回退到默认值");
    }

    private RiskConfigEntity createConfig(Long id, String symbol, BigDecimal marginRate, int positionLimit) {
        RiskConfigEntity config = new RiskConfigEntity();
        config.setId(id);
        config.setSymbol(symbol);
        config.setMarginRate(marginRate);
        config.setPositionLimit(positionLimit);
        config.setWarningRatio(new BigDecimal("80.00"));
        config.setLiquidationRatio(new BigDecimal("120.00"));
        config.setContractMultiplier(symbol.equals("ES") ? new BigDecimal("50") : new BigDecimal("100"));
        config.setEnabled(true);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
