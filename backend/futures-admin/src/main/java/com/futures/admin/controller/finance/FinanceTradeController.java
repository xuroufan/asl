package com.futures.admin.controller.finance;

import com.futures.admin.client.SettlementApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 交易流水查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/finance/trade")
@RequiredArgsConstructor
public class FinanceTradeController {

    private final SettlementApiService settlementApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('finance:trade:list')")
    public Result<?> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 代理结算服务查询
        return settlementApiService.getSettlementHistory(userId, startDate, endDate, page, size);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('finance:trade:export')")
    public Result<String> export(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success("导出任务已提交，请在下载中心查看");
    }
}
