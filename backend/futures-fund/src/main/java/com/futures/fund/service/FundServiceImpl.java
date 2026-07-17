package com.futures.fund.service;

import com.futures.common.exception.BizException;
import com.futures.common.util.RedisUtil;
import com.futures.fund.entity.FundAccountEntity;
import com.futures.fund.mapper.FundAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 资金管理服务实现 — 乐观锁 + 业务幂等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundServiceImpl implements FundService {

    /** 乐观锁重试最大次数 */
    private static final int MAX_RETRIES = 3;

    private final FundAccountMapper fundAccountMapper;
    private final FundFlowRecorder flowRecorder;
    private final RedisUtil redisUtil;

    // ==================== 冻结保证金（下单Try） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freeze(String userId, BigDecimal amount, String orderId) {
        // 幂等：检查订单是否已处理
        if (isIdempotent(orderId)) {
            log.info("幂等命中，跳过冻结: orderId={}", orderId);
            return true;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("冻结金额必须大于0");
        }

        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));

        if (account == null) {
            throw BizException.notFound("资金账户不存在: " + userId);
        }

        // 检查账户是否冻结
        if (account.getStatus() != null && account.getStatus() == 1) {
            throw BizException.forbidden("账户已冻结");
        }

        // 检查可用资金是否充足
        if (account.getAvailable().compareTo(amount) < 0) {
            throw BizException.insufficientFunds(
                    "可用资金不足，当前可用: " + account.getAvailable() + "，需要: " + amount);
        }

        // 乐观锁重试
        int retries = 0;
        int updated = 0;
        while (retries < MAX_RETRIES && updated == 0) {
            // 重新读取最新版本
            account = fundAccountMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                            .eq(FundAccountEntity::getUserId, userId));
            updated = fundAccountMapper.freezeByVersion(userId, amount, account.getVersion());
            retries++;
        }

        if (updated == 0) {
            log.error("冻结失败-乐观锁重试耗尽: userId={}, amount={}, retries={}",
                    userId, amount, retries);
            throw BizException.badRequest("系统繁忙，冻结失败");
        }

        log.info("冻结成功: userId={}, amount={}, retries={}", userId, amount, retries);

        // 记录流水
        flowRecorder.record(userId, orderId, 2, amount,
                account.getAvailable(), account.getAvailable().subtract(amount),
                account.getFrozen(), account.getFrozen().add(amount),
                "冻结保证金 - 订单: " + orderId);

        return true;
    }

    // ==================== 解冻保证金（撤单Cancel） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreeze(String userId, BigDecimal amount, String orderId) {
        if (isIdempotent(orderId + "_UNFREEZE")) {
            return true;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("解冻金额必须大于0");
        }

        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));

        if (account == null) {
            throw BizException.notFound("资金账户不存在: " + userId);
        }

        if (account.getFrozen().compareTo(amount) < 0) {
            log.warn("解冻金额超过冻结金额: frozen={}, amount={}", account.getFrozen(), amount);
            // 生产环境中这里应该触发告警
        }

        int retries = 0;
        int updated = 0;
        while (retries < MAX_RETRIES && updated == 0) {
            account = fundAccountMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                            .eq(FundAccountEntity::getUserId, userId));
            updated = fundAccountMapper.unfreezeByVersion(userId, amount, account.getVersion());
            retries++;
        }

        if (updated == 0) {
            throw BizException.badRequest("系统繁忙，解冻失败");
        }

        log.info("解冻成功: userId={}, amount={}", userId, amount);

        flowRecorder.record(userId, orderId, 3, amount,
                account.getAvailable(), account.getAvailable().add(amount),
                account.getFrozen(), account.getFrozen().subtract(amount),
                "解冻保证金 - 订单: " + orderId);

        return true;
    }

    // ==================== 扣减冻结资金（成交Confirm） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deduct(String userId, BigDecimal amount, String orderId) {
        if (isIdempotent(orderId + "_DEDUCT")) {
            return true;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("扣款金额必须大于0");
        }

        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));

        if (account == null) {
            throw BizException.notFound("资金账户不存在: " + userId);
        }

        if (account.getFrozen().compareTo(amount) < 0) {
            throw BizException.marginError(
                    "冻结资金不足，无法扣款: frozen=" + account.getFrozen() + ", amount=" + amount);
        }

        int retries = 0;
        int updated = 0;
        while (retries < MAX_RETRIES && updated == 0) {
            account = fundAccountMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                            .eq(FundAccountEntity::getUserId, userId));
            updated = fundAccountMapper.deductByVersion(userId, amount, account.getVersion());
            retries++;
        }

        if (updated == 0) {
            throw BizException.badRequest("系统繁忙，扣款失败");
        }

        log.info("扣款成功: userId={}, amount={}", userId, amount);

        flowRecorder.record(userId, orderId, 4, amount,
                account.getBalance(), account.getBalance().subtract(amount),
                account.getFrozen(), account.getFrozen().subtract(amount),
                "扣款 - 订单: " + orderId);

        return true;
    }

    // ==================== 入金 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deposit(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("入金金额必须大于0");
        }

        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));

        if (account == null) {
            // 自动创建账户
            account = FundAccountEntity.create(userId);
            fundAccountMapper.insert(account);
            account = fundAccountMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                            .eq(FundAccountEntity::getUserId, userId));
        }

        int retries = 0;
        int updated = 0;
        while (retries < MAX_RETRIES && updated == 0) {
            account = fundAccountMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                            .eq(FundAccountEntity::getUserId, userId));
            updated = fundAccountMapper.depositByVersion(userId, amount, account.getVersion());
            retries++;
        }

        if (updated == 0) {
            throw BizException.badRequest("系统繁忙，入金失败");
        }

        log.info("入金成功: userId={}, amount={}", userId, amount);

        flowRecorder.record(userId, null, 0, amount,
                account.getBalance(), account.getBalance().add(amount),
                account.getAvailable(), account.getAvailable().add(amount),
                "入金");

        return true;
    }

    // ==================== 出金 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawRequest(String userId, BigDecimal amount, String withdrawId) {
        if (isIdempotent(withdrawId)) {
            return true;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("出金金额必须大于0");
        }

        // 出金申请先冻结可用资金
        return freeze(userId, amount, withdrawId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawApprove(String userId, BigDecimal amount, String withdrawId) {
        // 审批通过：从冻结中扣减
        return deduct(userId, amount, withdrawId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawReject(String userId, BigDecimal amount, String withdrawId) {
        // 审批拒绝：解冻
        return unfreeze(userId, amount, withdrawId);
    }

    // ==================== 查询 ====================

    @Override
    public FundAccountEntity getBalanceOverview(String userId) {
        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));
        if (account == null) {
            throw BizException.notFound("资金账户不存在: " + userId);
        }
        return account;
    }

    @Override
    public BigDecimal getAvailable(String userId) {
        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));
        return account != null ? account.getAvailable() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getUsedMargin(String userId) {
        FundAccountEntity account = fundAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundAccountEntity>()
                        .eq(FundAccountEntity::getUserId, userId));
        return account != null ? account.getMargin() : BigDecimal.ZERO;
    }

    // ==================== 幂等性 ====================

    private boolean isIdempotent(String key) {
        if (key == null || key.isBlank()) return false;
        String redisKey = "fund:idempotent:" + key;
        // 如果Redis中存在，说明已处理
        Boolean exists = redisUtil.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }
        // 标记已处理（TTL=1小时）
        redisUtil.set(redisKey, "1", 3600, java.util.concurrent.TimeUnit.SECONDS);
        return false;
    }
}
