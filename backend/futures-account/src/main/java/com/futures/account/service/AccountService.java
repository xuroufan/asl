package com.futures.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.futures.account.entity.AccountEntity;
import com.futures.account.mapper.AccountMapper;
import com.futures.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;

    /** 获取账户总览 */
    public Map<String, Object> getAccountOverview(Long userId) {
        AccountEntity acc = getAccount(userId);

        BigDecimal riskRatio = acc.getEquityWithLoan().compareTo(BigDecimal.ZERO) > 0
                ? acc.getInitialMargin().divide(acc.getEquityWithLoan(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cashBalance", acc.getCashBalance());
        result.put("equityWithLoan", acc.getEquityWithLoan());
        result.put("initialMargin", acc.getInitialMargin());
        result.put("maintenanceMargin", acc.getMaintenanceMargin());
        result.put("availableFunds", acc.getAvailableFunds());
        result.put("dailyPnl", acc.getDailyPnl());
        result.put("dailyLossLimit", acc.getDailyLossLimit());
        result.put("totalPnl", acc.getTotalPnl());
        result.put("riskRatio", riskRatio);
        result.put("marginCall", riskRatio.compareTo(BigDecimal.ZERO) > 0 && riskRatio.compareTo(new BigDecimal("150")) < 0);
        return result;
    }

    public AccountEntity getAccount(Long userId) {
        AccountEntity acc = accountMapper.selectOne(
                new LambdaQueryWrapper<AccountEntity>().eq(AccountEntity::getUserId, userId));
        if (acc == null) throw BizException.notFound("账户不存在");
        return acc;
    }
}
