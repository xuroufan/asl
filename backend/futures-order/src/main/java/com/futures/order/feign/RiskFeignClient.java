package com.futures.order.feign;

import com.futures.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 风控服务 Feign 客户端
 */
@FeignClient(name = "futures-risk", url = "http://localhost:8085", path = "/api/v1/risk", fallbackFactory = RiskFeignFallback.class)
public interface RiskFeignClient {

    /**
     * 验仓：检查用户是否超过持仓限额
     *
     * @param userId 用户ID
     * @param symbol 合约代码
     * @param volume 欲开仓手数
     * @return 验仓结果
     */
    @PostMapping("/check-position-limit")
    Result<Void> checkPositionLimit(@RequestParam("userId") Long userId,
                                    @RequestParam("symbol") String symbol,
                                    @RequestParam("volume") Integer volume);

    /**
     * 验资：检查用户可用资金是否充足
     *
     * @param userId    用户ID
     * @param symbol    合约代码
     * @param volume    手数
     * @param price     价格
     * @return 验资结果
     */
    @PostMapping("/check-margin")
    Result<Void> checkMargin(@RequestParam("userId") Long userId,
                             @RequestParam("symbol") String symbol,
                             @RequestParam("volume") Integer volume,
                             @RequestParam("price") java.math.BigDecimal price);

    /**
     * 查询用户当前风险度
     */
    @GetMapping("/risk-ratio")
    Result<java.math.BigDecimal> getRiskRatio(@RequestParam("userId") Long userId);

    /**
     * 检查日内亏损限额
     */
    @GetMapping("/check-daily-loss")
    Result<Void> checkDailyLossLimit(@RequestParam("userId") Long userId);
}
