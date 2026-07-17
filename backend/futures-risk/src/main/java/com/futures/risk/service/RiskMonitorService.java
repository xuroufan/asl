package com.futures.risk.service;

import com.futures.common.message.event.RiskAlertEvent;
import com.futures.risk.entity.LiquidationRecordEntity;
import com.futures.risk.entity.RiskAlertEntity;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.mapper.RiskAlertMapper;
import com.futures.risk.mapper.RiskConfigMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 风险度实时监控服务
 * <p>
 * 定时计算所有用户的实时风险度 + 事件驱动更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskMonitorService {

    private final RiskConfigMapper riskConfigMapper;
    private final RiskAlertMapper riskAlertMapper;
    private final LiquidationService liquidationService;
    private final MarginCalculator marginCalculator;

    /**
     * 定时任务：每分钟扫描所有用户风险度
     */
    @Scheduled(fixedRate = 60000)
    public void scheduledRiskScan() {
        log.debug("开始定时风险扫描...");

        // 获取所有活跃用户列表
        // TODO: 实际从账户服务获取所有有持仓的用户
        List<String> activeUsers = getActiveUsers();

        for (String userId : activeUsers) {
            try {
                RiskStatus status = calculateRisk(userId);
                checkAndAlert(status);
            } catch (Exception e) {
                log.error("风险扫描异常: userId={}", userId, e);
            }
        }
    }

    /**
     * 计算用户实时风险度
     *
     * @param userId 用户ID
     * @return 风险状态
     */
    public RiskStatus calculateRisk(String userId) {
        // 1. 从资金服务获取用户当前权益
        // TODO: 调用资金服务Feign接口
        BigDecimal totalEquity = new BigDecimal("1000000"); // 模拟数据

        // 2. 从账户服务获取用户持仓列表
        // TODO: 调用账户服务Feign接口
        List<PositionInfo> positions = getPositions(userId);

        // 3. 计算占用保证金
        BigDecimal usedMargin = BigDecimal.ZERO;
        for (PositionInfo pos : positions) {
            BigDecimal margin = marginCalculator.calcMargin(
                    pos.symbol, pos.volume, pos.currentPrice);
            usedMargin = usedMargin.add(margin);
        }

        // 4. 计算风险度
        BigDecimal riskRatio = BigDecimal.ZERO;
        if (totalEquity.compareTo(BigDecimal.ZERO) > 0) {
            riskRatio = usedMargin.multiply(new BigDecimal("100"))
                    .divide(totalEquity, 2, RoundingMode.HALF_UP);
        }

        RiskStatus status = new RiskStatus();
        status.setUserId(userId);
        status.setTotalEquity(totalEquity);
        status.setUsedMargin(usedMargin);
        status.setRiskRatio(riskRatio);
        status.setPositionCount(positions.size());

        log.info("实时风险度: userId={}, equity={}, margin={}, risk={}%",
                userId, totalEquity, usedMargin, riskRatio);

        return status;
    }

    /**
     * 检查风险度并触发预警
     */
    public void checkAndAlert(RiskStatus status) {
        BigDecimal riskRatio = status.getRiskRatio();

        // 获取风控阈值
        // 简化版使用默认阈值
        BigDecimal warningThreshold = new BigDecimal("80");
        BigDecimal forbidOpenThreshold = new BigDecimal("100");
        BigDecimal liquidationThreshold = new BigDecimal("120");

        String alertLevel = null;
        String alertMessage = null;

        if (riskRatio.compareTo(liquidationThreshold) >= 0) {
            alertLevel = "CRITICAL";
            alertMessage = String.format(
                    "风险度 %.2f%% 超过强平阈值 %.0f%%，触发自动强平",
                    riskRatio, liquidationThreshold);

            // 触发强平
            liquidationService.executeLiquidation(status.getUserId(), "风险度超过强平阈值: " + riskRatio + "%");

        } else if (riskRatio.compareTo(forbidOpenThreshold) >= 0) {
            alertLevel = "CRITICAL";
            alertMessage = String.format(
                    "风险度 %.2f%% 超过禁止开仓阈值 %.0f%%",
                    riskRatio, forbidOpenThreshold);

        } else if (riskRatio.compareTo(warningThreshold) >= 0) {
            alertLevel = "WARN";
            alertMessage = String.format(
                    "风险度 %.2f%% 超过预警阈值 %.0f%%",
                    riskRatio, warningThreshold);
        }

        if (alertLevel != null) {
            // 记录预警
            saveAlert(status.getUserId(), alertLevel, "MARGIN", riskRatio, alertMessage);

            // 构建预警事件
            RiskAlertEvent alertEvent = RiskAlertEvent.builder()
                    .userId(status.getUserId())
                    .level(alertLevel)
                    .alertType("MARGIN")
                    .riskRatio(riskRatio)
                    .threshold(alertLevel.equals("CRITICAL") ?
                            (riskRatio.compareTo(new BigDecimal("120")) >= 0 ?
                                    new BigDecimal("120") : new BigDecimal("100"))
                            : new BigDecimal("80"))
                    .message(alertMessage)
                    .requireLiquidation(riskRatio.compareTo(new BigDecimal("120")) >= 0)
                    .alertedAt(LocalDateTime.now())
                    .build();

            // TODO: 发送 MQ 消息
            log.warn("风控预警: level={}, userId={}, riskRatio={}%, message={}",
                    alertLevel, status.getUserId(), riskRatio, alertMessage);
        }
    }

    /**
     * 记录风控预警
     */
    private void saveAlert(String userId, String level, String type,
                           BigDecimal riskRatio, String message) {
        RiskAlertEntity alert = new RiskAlertEntity();
        alert.setUserId(userId);
        alert.setLevel(level);
        alert.setAlertType(type);
        alert.setRiskRatio(riskRatio);
        alert.setMessage(message);
        alert.setHandleStatus(0);
        riskAlertMapper.insert(alert);
    }

    // ==================== 模拟数据 / TODO ====================

    private List<String> getActiveUsers() {
        // TODO: 从账户服务获取
        return new ArrayList<>();
    }

    private List<PositionInfo> getPositions(String userId) {
        // TODO: 从账户服务Feign获取
        return new ArrayList<>();
    }

    // ==================== DTO ====================

    @Data
    public static class RiskStatus {
        private String userId;
        private BigDecimal totalEquity;
        private BigDecimal usedMargin;
        private BigDecimal riskRatio;
        private int positionCount;
    }

    @Data
    public static class PositionInfo {
        private String symbol;
        private String direction;
        private int volume;
        private BigDecimal currentPrice;
        private BigDecimal openPrice;
    }
}
