package com.futures.fund.aspect;

import com.futures.common.exception.BizException;
import com.futures.fund.entity.FundFlowEntity;
import com.futures.fund.mapper.FundFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AOP 切面：自动记录资金操作流水。
 * <p>
 * 拦截 FundService 的所有核心资金操作方法（freezeMargin / unfreezeMargin / deduct / add / deposit），
 * 在方法执行失败时自动记录异常流水。
 * </p>
 *
 * <p>
 * 注意：成功时的流水已在 FundService 内部通过 updateWithRetry 的 flowAction 参数显式记录，
 * 此切面仅补充记录异常场景，确保每次关键操作都有流水痕迹。
 * </p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FundFlowAspect {

    private final FundFlowMapper fundFlowMapper;

    /** 拦截所有资金操作方法 */
    @Pointcut("execution(* com.futures.fund.service.FundService.freeze*(..)) " +
              "|| execution(* com.futures.fund.service.FundService.unfreeze*(..)) " +
              "|| execution(* com.futures.fund.service.FundService.deduct(..)) " +
              "|| execution(* com.futures.fund.service.FundService.add(..)) " +
              "|| execution(* com.futures.fund.service.FundService.deposit(..))")
    public void fundOperationPointcut() {}

    /**
     * 环绕通知：记录异常流水。
     */
    @Around("fundOperationPointcut()")
    public Object recordExceptionFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        try {
            return joinPoint.proceed();
        } catch (BizException e) {
            // 业务异常：记录异常流水（非技术故障）
            log.warn("资金操作业务异常: method={}, args={}, code={}, msg={}",
                    methodName, args, e.getCode(), e.getMessage());

            // 尝试提取 userId（第一个参数通常是 userId）
            if (args.length > 0 && args[0] instanceof Long userId) {
                recordAuditFlow(userId, methodName + "_FAILED",
                        e.getMessage(), args);
            }

            throw e;
        } catch (Exception e) {
            // 系统异常：记录错误流水
            log.error("资金操作系统异常: method={}, args={}, error={}",
                    methodName, args, e.getMessage());

            if (args.length > 0 && args[0] instanceof Long userId) {
                recordAuditFlow(userId, methodName + "_ERROR",
                        "系统异常: " + e.getMessage(), args);
            }

            throw e;
        }
    }

    /**
     * 记录审计流水。
     */
    private void recordAuditFlow(Long userId, String flowType,
                                  String remark, Object[] args) {
        try {
            FundFlowEntity flow = new FundFlowEntity();
            flow.setUserId(String.valueOf(userId));
            flow.setFlowType(99);
            flow.setAmount(BigDecimal.ZERO);
            flow.setBeforeBalance(BigDecimal.ZERO);
            flow.setAfterBalance(BigDecimal.ZERO);
            flow.setDescription("[AUTO] " + remark);
            fundFlowMapper.insert(flow);
        } catch (Exception e) {
            log.error("记录审计流水失败: userId={}, flowType={}", userId, flowType, e);
        }
    }
}
