package com.futures.fund.service;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保证金计算器
 * <p>
 * 根据不同品种的保证金率计算开仓所需保证金。
 * 保证金 = volume × price × contractMultiplier × marginRate
 */
@Component
public class MarginCalculator {

    /** 各品种默认保证金率（模拟数据） */
    private static final Map<String, MarginConfig> DEFAULT_CONFIG = new ConcurrentHashMap<>();

    static {
        DEFAULT_CONFIG.put("ES", new MarginConfig(50, 0.10));    // S&P 500 E-mini
        DEFAULT_CONFIG.put("GC", new MarginConfig(100, 0.08));   // 黄金
        DEFAULT_CONFIG.put("CL", new MarginConfig(1000, 0.12));  // 原油
        DEFAULT_CONFIG.put("SI", new MarginConfig(5000, 0.10));  // 白银
        DEFAULT_CONFIG.put("NQ", new MarginConfig(20, 0.12));    // Nasdaq 100
        DEFAULT_CONFIG.put("YM", new MarginConfig(5, 0.10));     // Dow Jones
        DEFAULT_CONFIG.put("ZB", new MarginConfig(1000, 0.05));  // 长期国债
        DEFAULT_CONFIG.put("ZN", new MarginConfig(1000, 0.04));  // 中期国债
        DEFAULT_CONFIG.put("6E", new MarginConfig(125000, 0.05)); // 欧元
        DEFAULT_CONFIG.put("6J", new MarginConfig(125000, 0.06)); // 日元
    }

    /**
     * 计算开仓保证金
     *
     * @param symbol 合约代码
     * @param volume 手数
     * @param price  开仓价格
     * @return 所需保证金
     */
    public BigDecimal calcMargin(String symbol, int volume, BigDecimal price) {
        if (symbol == null || price == null || volume <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        MarginConfig config = DEFAULT_CONFIG.get(symbol.toUpperCase());
        if (config == null) {
            // 未知合约：使用默认保证金率15%
            config = new MarginConfig(1, 0.15);
        }

        BigDecimal margin = BigDecimal.valueOf(volume)
                .multiply(price)
                .multiply(BigDecimal.valueOf(config.contractMultiplier))
                .multiply(BigDecimal.valueOf(config.marginRate))
                .setScale(4, RoundingMode.HALF_UP);

        return margin;
    }

    /**
     * 获取品种的保证金率
     */
    public double getMarginRate(String symbol) {
        MarginConfig config = DEFAULT_CONFIG.get(symbol.toUpperCase());
        return config != null ? config.marginRate : 0.15;
    }

    /**
     * 获取品种的合约乘数
     */
    public int getContractMultiplier(String symbol) {
        MarginConfig config = DEFAULT_CONFIG.get(symbol.toUpperCase());
        return config != null ? config.contractMultiplier : 1;
    }

    // ==================== 内部类 ====================

    public static class MarginConfig {
        public final int contractMultiplier;
        public final double marginRate;

        public MarginConfig(int contractMultiplier, double marginRate) {
            this.contractMultiplier = contractMultiplier;
            this.marginRate = marginRate;
        }
    }
}
