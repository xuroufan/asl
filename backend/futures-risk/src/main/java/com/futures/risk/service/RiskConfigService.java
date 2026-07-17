package com.futures.risk.service;

import com.futures.common.exception.BizException;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 风控配置管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskConfigService {

    private final RiskConfigMapper riskConfigMapper;
    private final MarginCalculator marginCalculator;

    /**
     * 获取指定品种的风控配置
     */
    @Cacheable(value = "risk:config", key = "#symbol")
    public RiskConfigEntity getConfig(String symbol) {
        String key = symbol.toUpperCase();
        RiskConfigEntity config = riskConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RiskConfigEntity>()
                        .eq(RiskConfigEntity::getSymbol, key));
        if (config == null) {
            config = RiskConfigEntity.createDefault(key);
        }
        return config;
    }

    /**
     * 获取所有品种风控配置
     */
    public List<RiskConfigEntity> getAllConfigs() {
        return riskConfigMapper.selectList(null);
    }

    /**
     * 更新风控配置
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "risk:config", key = "#config.symbol")
    public void updateConfig(RiskConfigEntity config) {
        if (config.getId() == null) {
            // 新增
            riskConfigMapper.insert(config);
        } else {
            // 更新
            riskConfigMapper.updateById(config);
        }

        // 清空保证金计算器缓存
        marginCalculator.evictCache(config.getSymbol());

        log.info("风控配置已更新: symbol={}, marginRate={}, positionLimit={}",
                config.getSymbol(), config.getMarginRate(), config.getPositionLimit());
    }

    /**
     * 获取持仓限额
     */
    public int getPositionLimit(String symbol) {
        RiskConfigEntity config = getConfig(symbol);
        return config.getPositionLimit() != null ? config.getPositionLimit() : 1000;
    }

    /**
     * 获取最大单笔手数
     */
    public int getMaxOrderVolume(String symbol) {
        RiskConfigEntity config = getConfig(symbol);
        return config.getMaxOrderVolume() != null ? config.getMaxOrderVolume() : 100;
    }
}
