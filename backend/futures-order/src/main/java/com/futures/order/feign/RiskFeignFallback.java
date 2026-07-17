package com.futures.order.feign;

import com.futures.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * 风控服务 Feign 降级处理
 */
@Slf4j
@Component
public class RiskFeignFallback implements FallbackFactory<RiskFeignClient> {

    @Override
    public RiskFeignClient create(Throwable cause) {
        log.error("风控服务不可用: {}", cause.getMessage());
        return new RiskFeignClient() {
            @Override public Result<Void> checkPositionLimit(Long userId, String symbol, Integer volume) {
                return Result.error("风控服务不可用");
            }
            @Override public Result<Void> checkMargin(Long userId, String symbol, Integer volume, BigDecimal price) {
                return Result.error("风控服务不可用");
            }
            @Override public Result<BigDecimal> getRiskRatio(Long userId) {
                return Result.error("风控服务不可用");
            }
            @Override public Result<Void> checkDailyLossLimit(Long userId) {
                return Result.error("风控服务不可用");
            }
        };
    }
}
