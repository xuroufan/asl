package com.futures.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.common.exception.BizException;
import com.futures.risk.entity.LiquidationRecordEntity;
import com.futures.risk.entity.RiskAlertEntity;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskAlertMapper;
import com.futures.risk.mapper.RiskConfigMapper;
import com.futures.risk.mapper.LiquidationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控引擎服务（编排层）。
 * <p>作为风控模块的统一入口，将请求委派给各个专业化服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskCheckService riskCheckService;
    private final RiskMonitorService riskMonitorService;
    private final MarginCalculator marginCalculator;
    private final LiquidationService liquidationService;
    private final RiskConfigMapper riskConfigMapper;
    private final RiskAlertMapper riskAlertMapper;
    private final LiquidationRecordMapper liquidationRecordMapper;

    // ==================== 前置风控校验 ====================

    /** 验仓：检查用户是否超过持仓限额。 */
    public void checkPositionLimit(Long userId, String symbol, Integer volume) {
        RiskConfigEntity config = riskConfigMapper.selectOne(
                new LambdaQueryWrapper<RiskConfigEntity>()
                        .eq(RiskConfigEntity::getSymbol, symbol.toUpperCase()));
        if (config == null) {
            config = RiskConfigEntity.createDefault(symbol);
        }
        if (volume > config.getPositionLimit()) {
            throw BizException.badRequest(
                    "持仓超限: symbol=" + symbol + ", limit=" + config.getPositionLimit() + ", request=" + volume);
        }
        log.debug("验仓通过: userId={}, symbol={}, volume={}", userId, symbol, volume);
    }

    /** 验资前置检查（日志记录）。 */
    public void checkMargin(Long userId, String symbol, Integer volume, BigDecimal price) {
        log.debug("验资前置检查 userId={}, symbol={}, volume={}, price={}", userId, symbol, volume, price);
    }

    /** 计算风险度（百分比）。 */
    public BigDecimal calcRiskRatio(BigDecimal usedMargin, BigDecimal totalEquity) {
        if (totalEquity == null || totalEquity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return usedMargin.multiply(new BigDecimal("100"))
                .divide(totalEquity, 2, RoundingMode.HALF_UP);
    }

    /** 检查日内亏损限额。 */
    public void checkDailyLossLimit(BigDecimal dailyPnl, BigDecimal dailyLossLimit) {
        if (dailyPnl == null || dailyLossLimit == null) return;
        if (dailyPnl.compareTo(dailyLossLimit) < 0) {
            log.warn("日内亏损超限: pnl={}, limit={}", dailyPnl, dailyLossLimit);
        }
    }

    // ==================== 综合风控校验 ====================

    /** 前置风控综合校验。 */
    public void preTradeCheck(Long userId, String symbol, String side, int volume, BigDecimal price) {
        RiskCheckService.PreCheckRequest request = new RiskCheckService.PreCheckRequest();
        request.setUserId(String.valueOf(userId));
        request.setSymbol(symbol);
        request.setDirection(side);
        request.setVolume(volume);
        request.setPrice(price);
        RiskCheckService.RiskCheckResult result = riskCheckService.preCheck(request);
        if (!result.isPassed()) {
            throw BizException.badRequest("前置风控校验失败: " + result.getReason());
        }
        log.debug("前置风控校验通过: userId={}, symbol={}", userId, symbol);
    }

    /** 验资（含保证金计算）。 */
    public void checkMarginSufficiency(BigDecimal availableFunds, String symbol, int volume, BigDecimal price, BigDecimal volatility) {
        BigDecimal required = marginCalculator.calcMargin(symbol, volume, price);
        if (availableFunds.compareTo(required) < 0) {
            throw BizException.insufficientFunds(
                    "资金不足: available=" + availableFunds + ", required=" + required);
        }
    }

    /** 盘中风控检查。 */
    public boolean checkIntradayRisk(Long userId, BigDecimal riskRatio, String symbol) {
        if (riskRatio == null) return true;
        if (riskRatio.compareTo(new BigDecimal("100")) >= 0) {
            log.warn("盘中风控触发: userId={}, riskRatio={}%, symbol={}", userId, riskRatio, symbol);
            return false;
        }
        return true;
    }

    // ==================== 强平 ====================

    /** 执行强平。 */
    public void executeLiquidation(Long userId, BigDecimal currentRatio, BigDecimal equity) {
        String reason = String.format("风险度 %.2f%% 触发强平", currentRatio);
        liquidationService.executeLiquidation(String.valueOf(userId), reason);
    }

    // ==================== 风险状态 ====================

    /** 获取用户当前风险度。 */
    public BigDecimal getCurrentRiskRatio(Long userId) {
        RiskMonitorService.RiskStatus status = riskMonitorService.calculateRisk(String.valueOf(userId));
        return status.getRiskRatio();
    }

    /** 获取用户风控状态总览。 */
    public Map<String, Object> getUserRiskStatus(Long userId) {
        RiskMonitorService.RiskStatus status = riskMonitorService.calculateRisk(String.valueOf(userId));
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("totalEquity", status.getTotalEquity());
        result.put("usedMargin", status.getUsedMargin());
        result.put("riskRatio", status.getRiskRatio());
        result.put("positionCount", status.getPositionCount());
        return result;
    }

    /** 查询预警记录（分页）。 */
    public IPage<RiskAlertEntity> getRiskAlerts(Long userId, String alertType, int page, int size) {
        LambdaQueryWrapper<RiskAlertEntity> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(RiskAlertEntity::getUserId, String.valueOf(userId));
        }
        if (alertType != null && !alertType.isEmpty()) {
            wrapper.eq(RiskAlertEntity::getAlertType, alertType);
        }
        wrapper.orderByDesc(RiskAlertEntity::getCreatedAt);
        return riskAlertMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /** 查询强平记录（分页）。 */
    public IPage<LiquidationRecordEntity> getLiquidationRecords(Long userId, int page, int size) {
        LambdaQueryWrapper<LiquidationRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(LiquidationRecordEntity::getUserId, String.valueOf(userId));
        }
        wrapper.orderByDesc(LiquidationRecordEntity::getCreatedAt);
        return liquidationRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    // ==================== 风控配置管理 ====================

    /** 查询所有风控配置。 */
    public List<RiskConfigEntity> listRiskConfigs() {
        return riskConfigMapper.selectList(null);
    }

    /** 更新风控配置（管理员）。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateRiskConfig(RiskConfigEntity config) {
        RiskConfigEntity existing = riskConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw BizException.notFound("风控配置不存在 id=" + config.getId());
        }
        riskConfigMapper.updateById(config);
        log.info("风控配置已更新: id={}, symbol={}", config.getId(), config.getSymbol());
    }

    /** 刷新MarginCalculator本地缓存。 */
    public void refreshConfigCache() {
        log.info("风控配置缓存已刷新（下次查询自动重新加载）");
    }

    // ==================== 保证金计算 ====================

    /** 计算初始保证金。 */
    public BigDecimal calcInitialMargin(String symbol, int volume, BigDecimal price, BigDecimal volatility) {
        return marginCalculator.calcMargin(symbol, volume, price);
    }

    /** 获取保证金率。 */
    public BigDecimal getMarginRate(String symbol) {
        return marginCalculator.getMarginRate(symbol);
    }
}
