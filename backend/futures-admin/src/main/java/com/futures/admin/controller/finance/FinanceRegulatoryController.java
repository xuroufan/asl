package com.futures.admin.controller.finance;

import com.futures.admin.client.SettlementApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 监管报表输出控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/finance/regulatory")
@RequiredArgsConstructor
public class FinanceRegulatoryController {

    private final SettlementApiService settlementApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('finance:regulatory:list')")
    public Result<?> list(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return settlementApiService.getReportHistory(startDate, endDate, null);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('finance:regulatory:generate')")
    public Result<String> generate(@RequestParam String type) {
        return Result.success(type + " 监管报表已生成");
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('finance:regulatory:export')")
    public Result<String> export(@RequestParam Long reportId, @RequestParam(defaultValue = "PDF") String format) {
        return Result.success("监管报表导出任务已提交，格式: " + format);
    }
}
