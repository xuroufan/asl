package com.futures.order.feign;

import com.futures.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 资金服务 Feign 降级处理
 */
@Slf4j
@Component
public class FundFeignFallback implements FallbackFactory<FundFeignClient> {

    @Override
    public FundFeignClient create(Throwable cause) {
        log.error("资金服务不可用: {}", cause.getMessage());
        return new FundFeignClient() {
            @Override
            public Result<Map<String, Object>> freezeMargin(Long userId, String symbol, Integer volume, BigDecimal price) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("success", false);
                fallback.put("message", "资金服务不可用，无法冻结保证金");
                return Result.error("资金服务不可用，无法冻结保证金");
            }

            @Override
            public Result<Void> unfreezeMargin(Long userId, String symbol, Integer volume) {
                return Result.error("资金服务不可用，无法解冻保证金");
            }

            @Override
            public Result<BigDecimal> getAvailableFunds(Long userId) {
                return Result.error("资金服务不可用");
            }

            @Override
            public Result<BigDecimal> getUsedMargin(Long userId) {
                return Result.error("资金服务不可用");
            }
        };
    }
}
