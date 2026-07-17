package com.futures.fund;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.futures.common.exception.BizException;
import com.futures.fund.cache.IdempotentCache;
import com.futures.fund.entity.FundAccountEntity;
import com.futures.fund.entity.FundFlowEntity;
import com.futures.fund.entity.WithdrawRecordEntity;
import com.futures.fund.mapper.FundAccountMapper;
import com.futures.fund.mapper.FundFlowMapper;
import com.futures.fund.mapper.WithdrawRecordMapper;
import com.futures.fund.service.FundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 资金管理服务单元测试。
 * <p>验证保证金计算、冻结/解冻、乐观锁重试、出金流程和资金流水记录的准确性。</p>
 */
class FundServiceTest {

    // 手动创建 mocks，避免 MockitoExtension 对 non-interface class (RedisUtil) 的限制
    private final FundAccountMapper fundAccountMapper = mock(FundAccountMapper.class);
    private final FundFlowMapper fundFlowMapper = mock(FundFlowMapper.class);
    private final WithdrawRecordMapper withdrawRecordMapper = mock(WithdrawRecordMapper.class);
    private final IdempotentCache idempotentCache = mock(IdempotentCache.class);

    private FundService fundService;

    private final Long userId = 1L;
    private final String symbolES = "ES";
    private final String symbolGC = "GC";

    @BeforeEach
    void setUp() {
        fundService = new FundService(fundAccountMapper, fundFlowMapper, withdrawRecordMapper, idempotentCache);
    }

    // ==================== 保证金计算测试 ====================

    @Test
    void testCalcMargin_ES() {
        // ES 合约：价格4500, 手数2, 乘数50, 保证金率5%
        // 预期: 4500 * 50 * 2 * 0.05 = 22500
        BigDecimal margin = fundService.calcMargin(symbolES, 2, new BigDecimal("4500"));
        assertEquals(0, new BigDecimal("22500.00").compareTo(margin),
                "ES 保证金计算错误: " + margin);
    }

    @Test
    void testCalcMargin_GC() {
        BigDecimal margin = fundService.calcMargin(symbolGC, 3, new BigDecimal("2000"));
        assertEquals(0, new BigDecimal("48000.00").compareTo(margin),
                "GC 保证金计算错误: " + margin);
    }

    @Test
    void testCalcMargin_UnknownSymbol() {
        BigDecimal margin = fundService.calcMargin("UNKNOWN", 1, new BigDecimal("1000"));
        assertEquals(0, new BigDecimal("1000.00").compareTo(margin),
                "未知合约保证金计算错误: " + margin);
    }

    @Test
    void testCalcMargin_ZeroVolume() {
        BigDecimal margin = fundService.calcMargin(symbolES, 0, new BigDecimal("4500"));
        assertEquals(0, BigDecimal.ZERO.compareTo(margin), "零手数保证金应为0");
    }

    // ==================== 入金测试 ====================

    @Test
    void testDeposit_shouldIncreaseBalance() {
        FundAccountEntity account = createDefaultAccount();
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);
        when(fundAccountMapper.updateById(any())).thenReturn(1);

        fundService.deposit(userId, new BigDecimal("100000"));

        verify(fundAccountMapper, times(1)).updateById(argThat(a -> {
            FundAccountEntity acct = (FundAccountEntity) a;
            return acct.getBalance().compareTo(new BigDecimal("600000")) == 0
                    && acct.getAvailable().compareTo(new BigDecimal("500000")) == 0;
        }));
        verify(fundFlowMapper, times(1)).insert(argThat(f -> {
            FundFlowEntity flow = (FundFlowEntity) f;
            return "DEPOSIT".equals(flow.getFlowType())
                    && flow.getAmount().compareTo(new BigDecimal("100000")) == 0;
        }));
    }

    // ==================== 冻结/扣款链路测试 ====================

    @Test
    void testFreezeDeductChain() {
        FundAccountEntity account = createDefaultAccount();
        account.setBalance(new BigDecimal("500000"));
        account.setFrozen(BigDecimal.ZERO);
        account.setMargin(BigDecimal.ZERO);
        account.setAvailable(new BigDecimal("500000"));

        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);
        when(fundAccountMapper.updateById(any())).thenReturn(1);

        // 冻结保证金
        fundService.freezeMargin(userId, symbolES, 2, new BigDecimal("4500"));

        verify(fundAccountMapper, times(1)).updateById(argThat(a -> {
            FundAccountEntity acct = (FundAccountEntity) a;
            return acct.getFrozen().compareTo(new BigDecimal("22500.00")) == 0
                    && acct.getAvailable().compareTo(new BigDecimal("477500.00")) == 0;
        }));
        verify(fundFlowMapper, times(1)).insert(argThat(f -> {
            FundFlowEntity flow = (FundFlowEntity) f;
            return "FREEZE".equals(flow.getFlowType())
                    && flow.getAmount().compareTo(new BigDecimal("-22500.00")) == 0;
        }));

        // 扣款：冻结转占用
        reset(fundAccountMapper, fundFlowMapper, idempotentCache);
        account.setFrozen(new BigDecimal("22500"));
        account.setMargin(BigDecimal.ZERO);
        when(idempotentCache.hasKey(anyString())).thenReturn(false);
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);
        when(fundAccountMapper.updateById(any())).thenReturn(1);

        fundService.deduct(userId, 1001L, new BigDecimal("22500"));

        verify(fundAccountMapper, times(1)).updateById(argThat(a -> {
            FundAccountEntity acct = (FundAccountEntity) a;
            return acct.getFrozen().compareTo(BigDecimal.ZERO) == 0
                    && acct.getMargin().compareTo(new BigDecimal("22500")) == 0;
        }));
        verify(fundFlowMapper, times(1)).insert(argThat(f -> {
            FundFlowEntity flow = (FundFlowEntity) f;
            return "DEDUCT".equals(flow.getFlowType());
        }));
        verify(idempotentCache, times(1)).set(anyString(), anyString(), anyLong());
    }

    // ==================== 乐观锁重试测试 ====================

    @Test
    void testOptimisticLockRetry_shouldSucceedOnSecondAttempt() {
        FundAccountEntity account = createDefaultAccount();
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account, account);
        when(fundAccountMapper.updateById(any()))
                .thenReturn(0)
                .thenReturn(1);

        fundService.deposit(userId, new BigDecimal("50000"));

        verify(fundAccountMapper, times(2)).updateById(any());
        verify(fundFlowMapper, times(1)).insert(any());
    }

    @Test
    void testOptimisticLockRetry_shouldThrowAfterMaxRetries() {
        FundAccountEntity account = createDefaultAccount();
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account, account, account, account);
        when(fundAccountMapper.updateById(any()))
                .thenReturn(0, 0, 0, 0);

        assertThrows(BizException.class, () -> fundService.deposit(userId, new BigDecimal("50000")));

        verify(fundAccountMapper, times(3)).updateById(any());
    }

    // ==================== 扣款幂等性测试 ====================

    @Test
    void testDeductIdempotency() {
        when(idempotentCache.hasKey(anyString())).thenReturn(true);

        fundService.deduct(userId, 1001L, new BigDecimal("10000"));

        verify(fundAccountMapper, never()).selectOne(any());
        verify(fundAccountMapper, never()).updateById(any());
        verify(fundFlowMapper, never()).insert(any());
    }

    // ==================== 出金流程测试 ====================

    @Test
    void testWithdrawRequest_shouldFreezeFunds() {
        FundAccountEntity account = createDefaultAccount();
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);
        when(fundAccountMapper.updateById(any())).thenReturn(1);
        when(withdrawRecordMapper.insert(any())).thenReturn(1);

        fundService.requestWithdraw(userId, new BigDecimal("100000"), "HK-123456");

        verify(fundAccountMapper, times(1)).updateById(argThat(a -> {
            FundAccountEntity acct = (FundAccountEntity) a;
            return acct.getAvailable().compareTo(new BigDecimal("300000")) == 0
                    && acct.getFrozen().compareTo(new BigDecimal("100000")) == 0;
        }));
        verify(withdrawRecordMapper, times(1)).insert(argThat(r -> {
            WithdrawRecordEntity rec = (WithdrawRecordEntity) r;
            return rec.getAmount().compareTo(new BigDecimal("100000")) == 0
                    && "HK-123456".equals(rec.getBankInfo());
        }));
    }

    @Test
    void testWithdrawRequest_insufficientFunds() {
        FundAccountEntity account = createDefaultAccount();
        account.setAvailable(new BigDecimal("50000"));
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);

        assertThrows(BizException.class,
                () -> fundService.requestWithdraw(userId, new BigDecimal("100000"), "HK-123456"));
    }

    @Test
    void testWithdrawApprove_shouldDeductBalance() {
        FundAccountEntity account = createDefaultAccount();
        account.setFrozen(new BigDecimal("100000"));
        account.setBalance(new BigDecimal("500000"));

        WithdrawRecordEntity record = new WithdrawRecordEntity();
        record.setId(1L);
        record.setUserId(userId);
        record.setAmount(new BigDecimal("100000"));
        record.setBankInfo("HK-123456");
        record.setStatus(0);

        when(withdrawRecordMapper.selectById(1L)).thenReturn(record);
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);
        when(fundAccountMapper.updateById(any())).thenReturn(1);

        fundService.approveWithdraw(1L, "admin");

        verify(fundAccountMapper, times(1)).updateById(argThat(a -> {
            FundAccountEntity acct = (FundAccountEntity) a;
            return acct.getBalance().compareTo(new BigDecimal("400000")) == 0
                    && acct.getFrozen().compareTo(BigDecimal.ZERO) == 0;
        }));
        verify(withdrawRecordMapper, times(1)).updateById(argThat(r -> {
            WithdrawRecordEntity rec = (WithdrawRecordEntity) r;
            return rec.getId() == 1L && rec.getStatus() == 1;
        }));
    }

    @Test
    void testWithdrawReject_shouldUnfreezeFunds() {
        FundAccountEntity account = createDefaultAccount();
        account.setFrozen(new BigDecimal("100000"));
        account.setAvailable(new BigDecimal("300000"));

        WithdrawRecordEntity record = new WithdrawRecordEntity();
        record.setId(2L);
        record.setUserId(userId);
        record.setAmount(new BigDecimal("100000"));
        record.setBankInfo("HK-123456");
        record.setStatus(0);

        when(withdrawRecordMapper.selectById(2L)).thenReturn(record);
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);
        when(fundAccountMapper.updateById(any())).thenReturn(1);

        fundService.rejectWithdraw(2L, "admin", "银行卡信息不匹配");

        verify(fundAccountMapper, times(1)).updateById(argThat(a -> {
            FundAccountEntity acct = (FundAccountEntity) a;
            return acct.getAvailable().compareTo(new BigDecimal("400000")) == 0
                    && acct.getFrozen().compareTo(BigDecimal.ZERO) == 0;
        }));
        verify(withdrawRecordMapper, times(1)).updateById(argThat(r -> {
            WithdrawRecordEntity rec = (WithdrawRecordEntity) r;
            return rec.getStatus() == 2 && "银行卡信息不匹配".equals(rec.getRemark());
        }));
    }

    // ==================== 资金流水准确性测试 ====================

    @Test
    void testFundFlowRecordsCorrectBeforeAfter() {
        FundAccountEntity account = createDefaultAccount();
        account.setBalance(new BigDecimal("500000"));

        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);
        when(fundAccountMapper.updateById(any())).thenReturn(1);

        fundService.deposit(userId, new BigDecimal("100000"));

        verify(fundFlowMapper, times(1)).insert(argThat(f -> {
            FundFlowEntity flow = (FundFlowEntity) f;
            return flow.getBalanceBefore().compareTo(new BigDecimal("500000")) == 0
                    && flow.getBalanceAfter().compareTo(new BigDecimal("600000")) == 0;
        }));
    }

    // ==================== 账户总览测试 ====================

    @Test
    void testGetBalanceOverview() {
        FundAccountEntity account = createDefaultAccount();
        account.setFloatProfit(new BigDecimal("10000"));
        account.setDailyPnl(new BigDecimal("5000"));
        account.setDailyLossLimit(new BigDecimal("20000"));
        account.setCurrency("HKD");

        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);

        Map<String, Object> overview = fundService.getBalanceOverview(userId);

        assertEquals(userId, overview.get("userId"));
        assertEquals(new BigDecimal("400000"), overview.get("available"));
        assertEquals(new BigDecimal("10000"), overview.get("floatProfit"));
        assertEquals(new BigDecimal("5000"), overview.get("dailyPnl"));
        assertEquals(new BigDecimal("20000"), overview.get("dailyLossLimit"));
        assertEquals("HKD", overview.get("currency"));
        // equity = balance(500000) + floatProfit(10000) = 510000
        assertEquals(new BigDecimal("510000"), overview.get("equity"));
        assertEquals(new BigDecimal("0.00"),
                ((BigDecimal) overview.get("marginRatio")).setScale(2, RoundingMode.HALF_UP));
    }

    // ==================== 账户冻结状态测试 ====================

    @Test
    void testOperationOnFrozenAccount_shouldThrow() {
        FundAccountEntity account = createDefaultAccount();
        account.setStatus(1);

        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);

        assertThrows(BizException.class,
                () -> fundService.freezeMargin(userId, symbolES, 1, new BigDecimal("4500")));
    }

    @Test
    void testFreezeInsufficientFunds_shouldThrow() {
        FundAccountEntity account = createDefaultAccount();
        account.setAvailable(new BigDecimal("1000"));

        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);

        assertThrows(BizException.class,
                () -> fundService.freezeMargin(userId, symbolES, 10, new BigDecimal("4500")));
    }

    @Test
    void testDeductInsufficientFrozen_shouldThrow() {
        FundAccountEntity account = createDefaultAccount();
        account.setFrozen(new BigDecimal("1000"));

        when(idempotentCache.hasKey(anyString())).thenReturn(false);
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);

        assertThrows(BizException.class,
                () -> fundService.deduct(userId, 2001L, new BigDecimal("99999")));
    }

    @Test
    void testGetAvailable() {
        FundAccountEntity account = createDefaultAccount();
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);

        BigDecimal available = fundService.getAvailable(userId);

        assertEquals(new BigDecimal("400000"), available);
    }

    @Test
    void testGetUsedMargin() {
        FundAccountEntity account = createDefaultAccount();
        account.setFrozen(new BigDecimal("50000"));
        account.setMargin(new BigDecimal("100000"));
        when(fundAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(account);

        BigDecimal usedMargin = fundService.getUsedMargin(userId);

        assertEquals(new BigDecimal("150000"), usedMargin);
    }

    // ==================== 私有辅助方法 ====================

    private FundAccountEntity createDefaultAccount() {
        FundAccountEntity account = new FundAccountEntity();
        account.setId(1L);
        account.setUserId(userId);
        account.setVersion(0);
        account.setBalance(new BigDecimal("500000"));
        account.setFrozen(BigDecimal.ZERO);
        account.setMargin(BigDecimal.ZERO);
        account.setAvailable(new BigDecimal("400000"));
        account.setFloatProfit(BigDecimal.ZERO);
        account.setCurrency("HKD");
        account.setStatus(0);
        account.setDailyPnl(BigDecimal.ZERO);
        account.setDailyLossLimit(new BigDecimal("20000"));
        return account;
    }
}
