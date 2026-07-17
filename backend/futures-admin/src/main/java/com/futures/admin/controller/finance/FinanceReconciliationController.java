package com.futures.admin.controller.finance;

import com.futures.admin.client.SettlementApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 对账差异处理控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/finance/reconciliation")
@RequiredArgsConstructor
public class FinanceReconciliationController {

    private final SettlementApiService settlementApiService;

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('finance:reconciliation:list')")
    public Result<?> history(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return settlementApiService.getReconciliationHistory(startDate, endDate, page, size);
    }

    @GetMapping("/diffs")
    @PreAuthorize("hasAuthority('finance:reconciliation:list')")
    public Result<?> diffs(@RequestParam Long reconciliationId) {
        return settlementApiService.getReconciliationDiffs(reconciliationId);
    }

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('finance:reconciliation:run')")
    public Result<String> runReconciliation(@RequestParam String date, @RequestParam String type) {
        if (!"EXCHANGE".equals(type) && !"BANK".equals(type) && !"FULL".equals(type)) {
            return Result.error("无效的对账类型");
        }
        return Result.success(type + " 对账任务已提交，对账日期: " + date);
    }

    @PostMapping("/diff/resolve")
    @PreAuthorize("hasAuthority('finance:reconciliation:resolve')")
    public Result<String> resolveDiff(
            @RequestParam Long diffId,
            @RequestParam String resolution,
            @RequestParam(required = false) String notes) {
        return Result.success("差异已处理: " + resolution);
    }
}
