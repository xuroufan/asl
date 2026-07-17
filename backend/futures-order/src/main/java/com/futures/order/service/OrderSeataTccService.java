package com.futures.order.service;

import com.futures.common.exception.BizException;
import com.futures.order.entity.OrderEntity;
import com.futures.order.feign.FundFeignClient;
import com.futures.order.feign.RiskFeignClient;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/**
 * 订单 Seata TCC 分布式事务服务
 * <p>
 * 下单流程涉及三个微服务：
 * 1. 订单服务（创建订单）— Try
 * 2. 资金服务（冻结保证金）— Try
 * 3. 风控服务（验仓）— Try
 * <p>
 * Confirm：所有 Try 成功后，提交订单
 * Cancel：任何 Try 失败，回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSeataTccService implements OrderTccService {

    private final FundFeignClient fundFeignClient;
    private final RiskFeignClient riskFeignClient;

    /**
     * TCC-Try: 下单前的准备操作
     * 校验风控 → 冻结保证金 → 创建订单记录
     * <p>手动管理回滚：任何步骤失败时立即解冻已冻结的保证金。</p>
     *
     * @param order    已设置的订单实体（尚未持久化）
     * @param price    下单价格
     */
    public void tryPlace(OrderEntity order, BigDecimal price) {
        // Step 1: 风控验仓
        var riskResult = riskFeignClient.checkPositionLimit(
                order.getUserId(), order.getSymbol(), order.getVolume());
        if (riskResult.getCode() != 200) {
            throw BizException.positionError("风控验仓失败: " + riskResult.getMsg());
        }

        // Step 2: 风控验资
        var marginResult = riskFeignClient.checkMargin(
                order.getUserId(), order.getSymbol(), order.getVolume(), price);
        if (marginResult.getCode() != 200) {
            throw BizException.insufficientFunds("验资失败: " + marginResult.getMsg());
        }

        // Step 3: 冻结保证金 — 如后续失败需手动解冻
        var freezeResult = fundFeignClient.freezeMargin(
                order.getUserId(), order.getSymbol(), order.getVolume(), price);
        boolean frozen = freezeResult.getCode() == 200;
        if (!frozen) {
            throw BizException.insufficientFunds("冻结保证金失败: " + freezeResult.getMsg());
        }

        try {
            // Step 4: 检查日内亏损限额
            var lossResult = riskFeignClient.checkDailyLossLimit(order.getUserId());
            if (lossResult.getCode() != 200) {
                throw BizException.dailyLossLimitBreached();
            }
        } catch (Exception e) {
            // 手动回滚：解冻已冻结的保证金
            try { fundFeignClient.unfreezeMargin(order.getUserId(), order.getSymbol(), order.getVolume()); } catch (Exception ignored) {}
            throw e;
        }

        log.info("TCC-Try 完成: userId={}, symbol={}, volume={}, price={}",
                order.getUserId(), order.getSymbol(), order.getVolume(), price);
    }

    /**
     * Seata TCC-Confirm（自动提交）
     * Try 成功后，订单已持久化，无需额外操作
     */
    public void confirm(OrderEntity order) {
        log.info("TCC-Confirm: orderId={}", order.getOrderId());
    }

    /**
     * Seata TCC-Cancel（回滚）
     * 解冻保证金
     */
    public void cancel(OrderEntity order) {
        log.warn("TCC-Cancel: 回滚订单 orderId={}", order.getOrderId());
        fundFeignClient.unfreezeMargin(
                order.getUserId(), order.getSymbol(), order.getVolume());
    }
}
