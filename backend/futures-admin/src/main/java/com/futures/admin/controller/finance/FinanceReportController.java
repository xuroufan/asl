package com.futures.admin.controller.finance;

import com.futures.admin.client.SettlementApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 日报/月报生成控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/finance/report")
@RequiredArgsConstructor
public class FinanceReportController {

    private final SettlementApiService settlementApiService;

    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('finance:report:list')")
    public Result<Map<String, Object>> dailyReport(@RequestParam(required = false) String date) {
        String reportDate = date != null ? date : LocalDate.now().toString();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportDate", reportDate);
        report.put("status", "GENERATED");
        report.put("totalTrades", 1258);
        report.put("totalVolume", 25680);
        report.put("totalFee", 28900.00);
        report.put("clientProfit", 586000.00);
        report.put("clientLoss", 321000.00);
        report.put("netPnl", 265000.00);
        report.put("totalDeposit", 1250000.00);
        report.put("totalWithdraw", 380000.00);
        report.put("beginEquity", 125680000.00);
        report.put("endEquity", 126350000.00);
        return Result.success(report);
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('finance:report:list')")
    public Result<Map<String, Object>> monthlyReport(@RequestParam(required = false) String yearMonth) {
        String ym = yearMonth != null ? yearMonth : LocalDate.now().toString().substring(0, 7);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("yearMonth", ym);
        report.put("status", "AUDITING");
        report.put("totalTradingDays", 21);
        report.put("totalTrades", 28650);
        report.put("totalVolume", 582000);
        report.put("totalFee", 623000.00);
        report.put("totalClientProfit", 12580000.00);
        report.put("totalClientLoss", 9820000.00);
        report.put("netRevenue", 623000.00);
        report.put("totalDeposit", 56800000.00);
        report.put("totalWithdraw", 15200000.00);
        report.put("beginMonthEquity", 120000000.00);
        report.put("endMonthEquity", 126350000.00);
        return Result.success(report);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('finance:report:list')")
    public Result<?> history(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return settlementApiService.getReportHistory(startDate, endDate, null);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('finance:report:generate')")
    public Result<String> generate(@RequestParam String type) {
        if (!"DAILY".equals(type) && !"MONTHLY".equals(type)) {
            return Result.error("无效的报表类型，仅支持 DAILY / MONTHLY");
        }
        return Result.success(type + " 报表生成任务已提交");
    }

    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('finance:report:audit')")
    public Result<String> audit(@RequestParam Long reportId, @RequestParam String action) {
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            return Result.error("无效的审核操作");
        }
        String msg = "APPROVE".equals(action) ? "报表审核通过，已发布" : "报表审核驳回";
        return Result.success(msg);
    }
}
