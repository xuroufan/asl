package com.futures.risk.service;

import com.futures.common.exception.BizException;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskConfigMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 前置风控校验服务
 * <p>
 * 下单前校验：用户权限、验资、验仓
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskCheckService {

    private final RiskConfigMapper riskConfigMapper;

    /** 单笔最大下单手数 */
    private static final int MAX_ORDER_VOLUME = 9999;

    /**
     * 前置校验
     *
     * @param request 校验请求
     * @return 校验结果
     */
    public RiskCheckResult preCheck(PreCheckRequest request) {
        // 基础参数校验
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return RiskCheckResult.failed("价格必须大于0");
        }
        if (request.getVolume() == null || request.getVolume() <= 0) {
            return RiskCheckResult.failed("手数必须为正整数");
        }
        if (request.getVolume() > MAX_ORDER_VOLUME) {
            return RiskCheckResult.failed("单笔手数超过上限: " + MAX_ORDER_VOLUME);
        }

        String symbol = request.getSymbol().toUpperCase();

        // 1. 校验用户是否有该品种交易权限
        // TODO: 对接账户服务查询用户交易权限
        // 简化版：所有用户都有所有品种权限

        // 2. 获取风控配置
        RiskConfigEntity config = riskConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RiskConfigEntity>()
                        .eq(RiskConfigEntity::getSymbol, symbol));

        if (config == null) {
            config = RiskConfigEntity.createDefault(symbol);
        }

        // 检查最大手数限制
        if (request.getVolume() > config.getMaxOrderVolume()) {
            return RiskCheckResult.failed(
                    "单笔手数超过品种限额: max=" + config.getMaxOrderVolume());
        }

        // 3. 验资：计算所需保证金
        // 保证金 = volume × price × contractMultiplier × marginRate
        BigDecimal marginRate = config.getMarginRate();
        int multiplier = config.getContractMultiplier() != null ? config.getContractMultiplier() : 1;

        BigDecimal requiredMargin = BigDecimal.valueOf(request.getVolume())
                .multiply(request.getPrice())
                .multiply(BigDecimal.valueOf(multiplier))
                .multiply(marginRate)
                .setScale(4, BigDecimal.ROUND_HALF_UP);

        // TODO: 实际从资金服务获取可用资金
        // 简化版：假设可用资金充足
        log.info("验资: symbol={}, volume={}, price={}, requiredMargin={}",
                symbol, request.getVolume(), request.getPrice(), requiredMargin);

        // 4. 验仓：当前持仓 + 新开仓 <= 持仓限额
        // TODO: 实际从账户服务获取当前持仓
        // 简化版：假设持仓未超限

        log.info("前置风控校验通过: userId={}, symbol={}, direction={}, volume={}",
                request.getUserId(), symbol, request.getDirection(), request.getVolume());

        return RiskCheckResult.passed(requiredMargin);
    }

    // ==================== DTO ====================

    @Data
    public static class PreCheckRequest {
        private String userId;
        private String symbol;
        private String direction;   // BUY / SELL
        private Integer volume;
        private BigDecimal price;
    }

    @Data
    public static class RiskCheckResult {
        private boolean passed;
        private String reason;
        private BigDecimal requiredMargin;

        public static RiskCheckResult passed(BigDecimal margin) {
            RiskCheckResult r = new RiskCheckResult();
            r.passed = true;
            r.requiredMargin = margin;
            return r;
        }

        public static RiskCheckResult failed(String reason) {
            RiskCheckResult r = new RiskCheckResult();
            r.passed = false;
            r.reason = reason;
            return r;
        }
    }
}
