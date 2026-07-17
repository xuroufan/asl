package com.futures.fund.service;

import com.futures.common.exception.BizException;
import com.futures.common.util.RedisUtil;
import com.futures.fund.entity.FundAccountEntity;
import com.futures.fund.entity.FundFlowEntity;
import com.futures.fund.mapper.FundAccountMapper;
import com.futures.fund.mapper.FundFlowMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Seata TCC 分布式事务接口实现。
 * <p>
 * 下单流程 TCC：
 * <ul>
 *   <li>Try：冻结保证金（available → frozen）</li>
 *   <li>Confirm：成交后将 frozen 转为 margin</li>
 *   <li>Cancel：撤单后解冻保证金（frozen → available）</li>
 * </ul>
 * </p>
 */
@Slf4j
@LocalTCC
@Service
@RequiredArgsConstructor
public class FundTccService {

    private final FundAccountMapper fundAccountMapper;
    private final FundFlowMapper fundFlowMapper;
    private final RedisUtil redisUtil;

    /** 幂等键 TTL：24 小时 */
    private static final long IDEMPOTENT_TTL_HOURS = 24;

    // ==================== TCC: 冻结保证金（Try） ====================

    /**
     * 冻结保证金（Try 阶段）。
     *
     * @param context Seata 事务上下文
     * @param userId  用户 ID
     * @param orderId 订单 ID（业务幂等键）
     * @param symbol  合约代码
     * @param margin  保证金金额
     * @return true 表示 Try 成功
     */
    @TwoPhaseBusinessAction(
            name = "fundTccAction",
            commitMethod = "confirmFreeze",
            rollbackMethod = "cancelFreeze"
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean tryFreeze(BusinessActionContext context,
                             @BusinessActionContextParameter(paramName = "userId") Long userId,
                             @BusinessActionContextParameter(paramName = "orderId") String orderId,
                             @BusinessActionContextParameter(paramName = "symbol") String symbol,
                             @BusinessActionContextParameter(paramName = "margin") BigDecimal margin) {
        // 幂等性校验
        String idempotentKey = "fund:tcc:try:" + userId + ":" + orderId;
        if (Boolean.TRUE.equals(redisUtil.hasKey(idempotentKey))) {
            log.info("TCC Try 幂等命中，跳过 userId={}, orderId={}", userId, orderId);
            return true;
        }

        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));
        if (account == null) {
            throw BizException.notFound("资金账户不存在 userId=" + userId);
        }
        if (Integer.valueOf(1).equals(account.getStatus())) {
            throw BizException.badRequest("资金账户已冻结");
        }
        if (account.getAvailable().compareTo(margin) < 0) {
            throw BizException.insufficientFunds(
                    "可用资金不足，需要 " + margin + "，当前可用 " + account.getAvailable());
        }

        // available → frozen
        account.setAvailable(account.getAvailable().subtract(margin));
        account.setFrozen(account.getFrozen().add(margin));
        fundAccountMapper.updateById(account);

        // 记录流水
        recordFlow(userId, "TCC_TRY_FREEZE", margin.negate(), orderId, symbol, "TCC Try 冻结保证金");

        // 设置幂等键
        redisUtil.set(idempotentKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);

        log.info("TCC Try 冻结成功 userId={}, orderId={}, margin={}", userId, orderId, margin);
        return true;
    }

    // ==================== TCC: 确认冻结（Confirm） ====================

    /**
     * 确认冻结（Confirm 阶段）。
     * <p>将 frozen 转为 margin，完成资金扣减。</p>
     *
     * @param context Seata 事务上下文
     * @return true 表示 Confirm 成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmFreeze(BusinessActionContext context) {
        Long userId = (Long) context.getActionContext("userId");
        String orderId = (String) context.getActionContext("orderId");
        String symbol = (String) context.getActionContext("symbol");
        BigDecimal margin = (BigDecimal) context.getActionContext("margin");

        String idempotentKey = "fund:tcc:confirm:" + userId + ":" + orderId;
        if (Boolean.TRUE.equals(redisUtil.hasKey(idempotentKey))) {
            log.info("TCC Confirm 幂等命中，跳过 userId={}, orderId={}", userId, orderId);
            return true;
        }

        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));
        if (account == null) {
            log.error("TCC Confirm 账户不存在 userId={}", userId);
            return false;
        }

        // frozen → margin
        BigDecimal actualFrozen = margin.min(account.getFrozen());
        account.setFrozen(account.getFrozen().subtract(actualFrozen));
        account.setMargin(account.getMargin().add(actualFrozen));
        fundAccountMapper.updateById(account);

        // 记录流水
        recordFlow(userId, "TCC_CONFIRM", actualFrozen.negate(), orderId, symbol, "TCC Confirm 成交确认");

        redisUtil.set(idempotentKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);

        log.info("TCC Confirm 成功 userId={}, orderId={}, margin={}", userId, orderId, actualFrozen);
        return true;
    }

    // ==================== TCC: 回滚冻结（Cancel） ====================

    /**
     * 取消冻结（Cancel 阶段）。
     * <p>将 frozen 释放回 available。</p>
     *
     * @param context Seata 事务上下文
     * @return true 表示 Cancel 成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelFreeze(BusinessActionContext context) {
        Long userId = (Long) context.getActionContext("userId");
        String orderId = (String) context.getActionContext("orderId");
        String symbol = (String) context.getActionContext("symbol");
        BigDecimal margin = (BigDecimal) context.getActionContext("margin");

        String idempotentKey = "fund:tcc:cancel:" + userId + ":" + orderId;
        if (Boolean.TRUE.equals(redisUtil.hasKey(idempotentKey))) {
            log.info("TCC Cancel 幂等命中，跳过 userId={}, orderId={}", userId, orderId);
            return true;
        }

        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));
        if (account == null) {
            log.error("TCC Cancel 账户不存在 userId={}", userId);
            return false;
        }

        // frozen → available
        BigDecimal actualUnfreeze = margin.min(account.getFrozen());
        account.setFrozen(account.getFrozen().subtract(actualUnfreeze));
        account.setAvailable(account.getAvailable().add(actualUnfreeze));
        fundAccountMapper.updateById(account);

        // 记录流水
        recordFlow(userId, "TCC_CANCEL", actualUnfreeze, orderId, symbol, "TCC Cancel 回滚解冻");

        redisUtil.set(idempotentKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);

        log.info("TCC Cancel 成功 userId={}, orderId={}, margin={}", userId, orderId, actualUnfreeze);
        return true;
    }

    // ==================== 空回滚与悬挂处理 ====================

    /**
     * 空回滚检查：Try 阶段失败后 Cancel 被调用，此时尚未执行 Try。
     * 通过幂等键判断：如果 Try 未执行（幂等键不存在），则 Cancel 直接返回 true。
     * 如果 Try 已执行但幂等键存在，正常回滚。
     */
    public boolean isTryExecuted(String userId, String orderId) {
        return Boolean.TRUE.equals(redisUtil.hasKey("fund:tcc:try:" + userId + ":" + orderId));
    }

    // ==================== 私有方法 ====================

    private void recordFlow(Long userId, String flowType, BigDecimal amount,
                            String orderId, String symbol, String remark) {
        FundFlowEntity flow = new FundFlowEntity();
        flow.setUserId(String.valueOf(userId));
        flow.setFlowType(toFlowTypeCode(flowType));
        flow.setAmount(amount);
        flow.setOrderId(orderId);
        flow.setDescription(remark + (symbol != null ? " [" + symbol + "]" : ""));
        // 查询当前余额
        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, String.valueOf(userId)));
        if (account != null) {
            flow.setBeforeBalance(account.getBalance());
            flow.setAfterBalance(account.getBalance());
        }
        fundFlowMapper.insert(flow);
    }

    /**
     * 将字符串流水类型映射为整数编码
     */
    private int toFlowTypeCode(String flowType) {
        switch (flowType) {
            case "TCC_TRY_FREEZE": return 10;
            case "TCC_CONFIRM": return 11;
            case "TCC_CANCEL": return 12;
            default: return 99;
        }
    }
}
