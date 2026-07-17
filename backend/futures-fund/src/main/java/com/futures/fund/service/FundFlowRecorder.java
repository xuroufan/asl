package com.futures.fund.service;

import com.futures.fund.entity.FundFlowEntity;
import com.futures.fund.mapper.FundFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水记录器
 * <p>
 * 使用独立事务记录资金流水，确保流水不因主事务回滚而丢失
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundFlowRecorder {

    private final FundFlowMapper fundFlowMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(String userId, String orderId, int flowType,
                       BigDecimal amount,
                       BigDecimal beforeBalance, BigDecimal afterBalance,
                       BigDecimal beforeFrozen, BigDecimal afterFrozen,
                       String description) {
        FundFlowEntity flow = new FundFlowEntity();
        flow.setUserId(userId);
        flow.setOrderId(orderId);
        flow.setFlowType(flowType);
        flow.setAmount(amount);
        flow.setBeforeBalance(beforeBalance);
        flow.setAfterBalance(afterBalance);
        flow.setBeforeAvailable(null); // 由调用者设置
        flow.setAfterAvailable(null);
        flow.setBeforeFrozen(beforeFrozen);
        flow.setAfterFrozen(afterFrozen);
        flow.setDescription(description);
        flow.setCreatedAt(LocalDateTime.now());

        fundFlowMapper.insert(flow);
        log.debug("资金流水已记录: userId={}, type={}, amount={}", userId, flowType, amount);
    }
}
