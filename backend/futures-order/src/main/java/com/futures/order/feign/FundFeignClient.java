package com.futures.order.feign;

import com.futures.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 资金服务 Feign 客户端
 */
@FeignClient(name = "futures-fund", url = "http://localhost:8084", path = "/api/v1/fund", fallbackFactory = FundFeignFallback.class)
public interface FundFeignClient {

    /**
     * 冻结保证金（下单时）
     *
     * @param userId    用户ID
     * @param symbol    合约代码
     * @param volume    手数
     * @param price     价格（用于计算保证金）
     * @return 冻结结果（包含 success, availableAfter, frozenAfter, message）
     */
    @PostMapping("/freeze")
    Result<Map<String, Object>> freezeMargin(@RequestParam("userId") Long userId,
                                             @RequestParam("symbol") String symbol,
                                             @RequestParam("volume") Integer volume,
                                             @RequestParam("price") BigDecimal price);

    /**
     * 解冻保证金（撤单/成交时）
     */
    @PostMapping("/unfreeze")
    Result<Void> unfreezeMargin(@RequestParam("userId") Long userId,
                                @RequestParam("symbol") String symbol,
                                @RequestParam("volume") Integer volume);

    /**
     * 查询可用资金
     */
    @GetMapping("/available")
    Result<BigDecimal> getAvailableFunds(@RequestParam("userId") Long userId);

    /**
     * 查询保证金占用
     */
    @GetMapping("/margin")
    Result<BigDecimal> getUsedMargin(@RequestParam("userId") Long userId);
}
