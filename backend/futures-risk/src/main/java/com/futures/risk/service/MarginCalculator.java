package com.futures.risk.service;

import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保证金计算器 — 支持SPAN算法简化版
 * <p>
 * 根据不同品种的保证金率动态计算占用保证金
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarginCalculator {

    private final RiskConfigMapper riskConfigMapper;

    /** 本地缓存：symbol → 保证金率 */
    private final Map<String, BigDecimal> marginRateCache = new ConcurrentHashMap<>();

    /**
     * 计算占用保证金
     *
     * @param symbol 合约代码
     * @param volume 持仓手数
     * @param price  当前价格
     * @return 占用保证金
     */
    public BigDecimal calcMargin(String symbol, int volume, BigDecimal price) {
        if (symbol == null || price == null || volume <= 0) {
            return BigDecimal.ZERO;
        }

        RiskConfigEntity config = getConfig(symbol);
        if (config == null) {
            log.warn("未找到品种配置，使用默认保证金率: symbol={}", symbol);
            return BigDecimal.valueOf(volume).multiply(price).multiply(new BigDecimal("0.10"))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal marginRate = config.getMarginRate();
        int multiplier = config.getContractMultiplier() != null ? config.getContractMultiplier() : 1;

        return BigDecimal.valueOf(volume)
                .multiply(price)
                .multiply(BigDecimal.valueOf(multiplier))
                .multiply(marginRate)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 获取保证金率
     */
    public BigDecimal getMarginRate(String symbol) {
        RiskConfigEntity config = getConfig(symbol);
        if (config == null) {
            return new BigDecimal("0.10");
        }
        return config.getMarginRate();
    }

    /**
     * 获取合约乘数
     */
    public int getContractMultiplier(String symbol) {
        RiskConfigEntity config = getConfig(symbol);
        if (config == null) {
            return 1;
        }
        return config.getContractMultiplier() != null ? config.getContractMultiplier() : 1;
    }

    /**
     * 获取风控配置（带本地缓存）
     */
    private RiskConfigEntity getConfig(String symbol) {
        String key = symbol.toUpperCase();
        // 先查缓存
        if (marginRateCache.containsKey(key)) {
            // 从数据库重新查询看是否有变更
        }
        RiskConfigEntity config = riskConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RiskConfigEntity>()
                        .eq(RiskConfigEntity::getSymbol, key));
        if (config != null) {
            marginRateCache.put(key, config.getMarginRate());
        }
        return config;
    }

    /**
     * 清除缓存（配置变更时调用）
     */
    public void evictCache(String symbol) {
        marginRateCache.remove(symbol.toUpperCase());
    }
}
