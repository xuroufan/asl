package com.futures.fund.service;

import com.futures.fund.entity.FundAccountEntity;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 资金管理服务接口
 */
public interface FundService {

    /**
     * 冻结保证金（下单Try阶段）
     */
    boolean freeze(String userId, BigDecimal amount, String orderId);

    /**
     * 解冻保证金（撤单/下单Cancel阶段）
     */
    boolean unfreeze(String userId, BigDecimal amount, String orderId);

    /**
     * 扣减冻结资金（成交Confirm阶段）
     */
    boolean deduct(String userId, BigDecimal amount, String orderId);

    /**
     * 入金
     */
    boolean deposit(String userId, BigDecimal amount);

    /**
     * 查询资金概览
     */
    FundAccountEntity getBalanceOverview(String userId);

    /**
     * 获取可用资金
     */
    BigDecimal getAvailable(String userId);

    /**
     * 获取占用保证金
     */
    BigDecimal getUsedMargin(String userId);

    /**
     * 出金申请（冻结资金待审批）
     */
    boolean withdrawRequest(String userId, BigDecimal amount, String withdrawId);

    /**
     * 出金审批通过
     */
    boolean withdrawApprove(String userId, BigDecimal amount, String withdrawId);

    /**
     * 出金审批拒绝（解冻）
     */
    boolean withdrawReject(String userId, BigDecimal amount, String withdrawId);
}
