package com.futures.settlement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.common.exception.BizException;
import com.futures.settlement.entity.DailySettlementEntity;
import com.futures.settlement.entity.MarginCallEntity;
import com.futures.settlement.entity.SettlementOrderEntity;
import com.futures.settlement.mapper.DailySettlementMapper;
import com.futures.settlement.mapper.MarginCallMapper;
import com.futures.settlement.mapper.SettlementOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 清结算服务。
 * <p>负责日终结算、PnL核算、追保管理、结算单生成等。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final DailySettlementMapper dailySettlementMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final MarginCallMapper marginCallMapper;

    /** 默认维持保证金率（相对于初始保证金） */
    private static final BigDecimal MAINTENANCE_MARGIN_RATIO = new BigDecimal("0.8");

    /**
     * 执行单个用户日终结算。
     */
    @Transactional(rollbackFor = Exception.class)
    public DailySettlementEntity executeDailySettlement(Long userId, BigDecimal beginEquity,
                                                         BigDecimal realizedPnl, BigDecimal unrealizedPnl,
                                                         BigDecimal fee, BigDecimal netDeposit) {
        LocalDate today = LocalDate.now();

        DailySettlementEntity existing = dailySettlementMapper.selectOne(
                new LambdaQueryWrapper<DailySettlementEntity>()
                        .eq(DailySettlementEntity::getUserId, userId)
                        .eq(DailySettlementEntity::getSettlementDate, today));
        if (existing != null) {
            throw BizException.badRequest("今日已结算，userId=" + userId + ", date=" + today);
        }

        BigDecimal totalPnl = realizedPnl.add(unrealizedPnl);
        BigDecimal endEquity = beginEquity.add(totalPnl).subtract(fee).add(netDeposit);

        DailySettlementEntity settlement = new DailySettlementEntity();
        settlement.setUserId(userId);
        settlement.setSettlementDate(today);
        settlement.setBeginEquity(beginEquity);
        settlement.setEndEquity(endEquity);
        settlement.setRealizedPnl(realizedPnl);
        settlement.setUnrealizedPnl(unrealizedPnl);
        settlement.setTotalPnl(totalPnl);
        settlement.setFee(fee);
        settlement.setNetDeposit(netDeposit);
        settlement.setStatus("COMPLETED");
        settlement.setSettledTime(LocalDateTime.now());
        dailySettlementMapper.insert(settlement);

        log.info("日终结算完成 userId={}, beginEquity={}, endEquity={}, totalPnl={}",
                userId, beginEquity, endEquity, totalPnl);
        return settlement;
    }

    /**
     * 批量结算所有用户。
     * <p>遍历所有活跃用户执行日终结算。实际部署时从账户服务获取用户列表。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchSettleAllUsers(LocalDate settlementDate) {
        log.info("===== 开始批量结算，日期={} =====", settlementDate);
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;

        // 模拟用户列表：实际从账户服务获取
        List<Long> userIds = List.of(1L, 2L, 3L);
        for (Long userId : userIds) {
            try {
                BigDecimal beginEquity = new BigDecimal("500000");
                BigDecimal realizedPnl = BigDecimal.ZERO;
                BigDecimal unrealizedPnl = BigDecimal.ZERO;
                BigDecimal fee = BigDecimal.ZERO;
                BigDecimal netDeposit = BigDecimal.ZERO;

                executeDailySettlement(userId, beginEquity, realizedPnl, unrealizedPnl, fee, netDeposit);
                successCount++;
            } catch (Exception e) {
                log.error("用户 {} 结算失败", userId, e);
                errorCount++;
            }
        }

        result.put("settlementDate", settlementDate.toString());
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        log.info("===== 批量结算完成：成功{}，失败{} =====", successCount, errorCount);
        return result;
    }

    /**
     * 检查并生成追保通知。
     * <p>如果用户权益低于维持保证金，生成追保通知。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public MarginCallEntity processMarginCall(Long userId, BigDecimal currentEquity,
                                               BigDecimal currentMargin, BigDecimal settlementPrice) {
        // 维持保证金 = 当前保证金 * 0.8
        BigDecimal maintenanceMargin = currentMargin.multiply(MAINTENANCE_MARGIN_RATIO)
                .setScale(2, RoundingMode.HALF_UP);

        if (currentEquity.compareTo(maintenanceMargin) >= 0) {
            log.info("用户 {} 权益充足，无需追保", userId);
            return null;
        }

        BigDecimal deficit = maintenanceMargin.subtract(currentEquity)
                .setScale(2, RoundingMode.HALF_UP);

        MarginCallEntity marginCall = new MarginCallEntity();
        marginCall.setUserId(userId);
        marginCall.setMarginCallType("MAINTENANCE");
        marginCall.setRequiredAmount(deficit);
        marginCall.setCurrentMargin(currentMargin);
        marginCall.setCurrentEquity(currentEquity);
        marginCall.setStatus("PENDING");
        marginCall.setRemarks(String.format("维持保证金=%s, 当前权益=%s, 差额=%s",
                maintenanceMargin, currentEquity, deficit));
        marginCallMapper.insert(marginCall);

        log.warn("用户 {} 触发追保：需追加 {}，维持保证金={}，当前权益={}",
                userId, deficit, maintenanceMargin, currentEquity);
        return marginCall;
    }

    /**
     * 批量检查所有用户的追保需求。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCheckMarginCalls() {
        log.info("===== 开始批量追保检查 =====");
        Map<String, Object> result = new HashMap<>();
        int marginCallCount = 0;

        // 模拟：实际从账户/持仓服务获取
        List<Long> userIds = List.of(1L, 2L, 3L);
        for (Long userId : userIds) {
            try {
                // 模拟数据
                BigDecimal equity = new BigDecimal("400000");
                BigDecimal margin = new BigDecimal("500000");
                MarginCallEntity mc = processMarginCall(userId, equity, margin, BigDecimal.ZERO);
                if (mc != null) {
                    marginCallCount++;
                }
            } catch (Exception e) {
                log.error("用户 {} 追保检查失败", userId, e);
            }
        }

        result.put("marginCallCount", marginCallCount);
        log.info("===== 批量追保检查完成：{}个用户需追保 =====", marginCallCount);
        return result;
    }

    /**
     * 生成结算单数据。
     * <p>为用户生成格式化的结算单文本。</p>
     */
    public String generateSettlementStatement(Long userId, Long settlementId) {
        DailySettlementEntity settlement = dailySettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw BizException.notFound("结算记录不存在");
        }

        List<SettlementOrderEntity> orders = settlementOrderMapper.selectList(
                new LambdaQueryWrapper<SettlementOrderEntity>()
                        .eq(SettlementOrderEntity::getSettlementId, settlementId));

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("        期货交易结算单\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append("用户ID：").append(userId).append("\n");
        sb.append("结算日期：").append(settlement.getSettlementDate()).append("\n");
        sb.append("结算状态：").append(settlement.getStatus()).append("\n\n");

        sb.append("── 资金概览 ──\n");
        sb.append("期初权益：").append(settlement.getBeginEquity()).append("\n");
        sb.append("期末权益：").append(settlement.getEndEquity()).append("\n");
        sb.append("总盈亏：").append(settlement.getTotalPnl()).append("\n");
        sb.append("手续费：").append(settlement.getFee()).append("\n\n");

        if (!orders.isEmpty()) {
            sb.append("── 成交明细 ──\n");
            sb.append(String.format("%-8s %-6s %-10s %-10s %-6s %-10s\n",
                    "合约", "方向", "开仓价", "平仓价", "手数", "盈亏"));
            for (SettlementOrderEntity o : orders) {
                sb.append(String.format("%-8s %-6s %-10s %-10s %-6d %-10s\n",
                        o.getSymbol(), o.getDirection(),
                        o.getOpenPrice(), o.getClosePrice(),
                        o.getVolume(), o.getPnl()));
            }
        }

        sb.append("\n── 保证金情况 ──\n");
        if (settlement.getOpeningMargin() != null) {
            sb.append("期初保证金：").append(settlement.getOpeningMargin()).append("\n");
        }
        if (settlement.getClosingMargin() != null) {
            sb.append("期末保证金：").append(settlement.getClosingMargin()).append("\n");
        }
        if (settlement.getMarginCallAmount() != null
                && settlement.getMarginCallAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("⚠ 追保金额：").append(settlement.getMarginCallAmount()).append("\n");
        }

        sb.append("\n═══════════════════════════════════════\n");
        sb.append("生成时间：").append(LocalDateTime.now()).append("\n");

        return sb.toString();
    }

    /**
     * 记录结算订单明细。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordSettlementOrder(Long settlementId, Long userId, Long orderId,
                                       String symbol, String direction,
                                       BigDecimal openPrice, BigDecimal closePrice,
                                       Integer volume, BigDecimal fee) {
        BigDecimal pnl = direction.equalsIgnoreCase("BUY")
                ? closePrice.subtract(openPrice).multiply(BigDecimal.valueOf(volume))
                : openPrice.subtract(closePrice).multiply(BigDecimal.valueOf(volume));

        SettlementOrderEntity orderDetail = new SettlementOrderEntity();
        orderDetail.setSettlementId(settlementId);
        orderDetail.setUserId(userId);
        orderDetail.setOrderId(orderId);
        orderDetail.setSymbol(symbol);
        orderDetail.setDirection(direction);
        orderDetail.setOpenPrice(openPrice);
        orderDetail.setClosePrice(closePrice);
        orderDetail.setVolume(volume);
        orderDetail.setPnl(pnl);
        orderDetail.setFee(fee);
        settlementOrderMapper.insert(orderDetail);
    }

    /**
     * 查询用户的结算历史（分页）。
     */
    public Page<DailySettlementEntity> getSettlementHistory(Long userId, LocalDate startDate,
                                                             LocalDate endDate, int page, int size) {
        LambdaQueryWrapper<DailySettlementEntity> wrapper = new LambdaQueryWrapper<DailySettlementEntity>()
                .eq(userId != null, DailySettlementEntity::getUserId, userId)
                .ge(startDate != null, DailySettlementEntity::getSettlementDate, startDate)
                .le(endDate != null, DailySettlementEntity::getSettlementDate, endDate)
                .orderByDesc(DailySettlementEntity::getSettlementDate);
        return dailySettlementMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 获取最近一次结算数据。
     */
    public DailySettlementEntity getLatestSettlement(Long userId) {
        LambdaQueryWrapper<DailySettlementEntity> wrapper = new LambdaQueryWrapper<DailySettlementEntity>()
                .eq(DailySettlementEntity::getUserId, userId)
                .orderByDesc(DailySettlementEntity::getSettlementDate)
                .last("LIMIT 1");
        return dailySettlementMapper.selectOne(wrapper);
    }

    /**
     * 查询用户的追保记录。
     */
    public List<MarginCallEntity> getMarginCallHistory(Long userId) {
        return marginCallMapper.selectList(
                new LambdaQueryWrapper<MarginCallEntity>()
                        .eq(MarginCallEntity::getUserId, userId)
                        .orderByDesc(MarginCallEntity::getCreateTime));
    }
}
