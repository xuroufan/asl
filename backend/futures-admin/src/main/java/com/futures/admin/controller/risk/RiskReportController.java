package com.futures.admin.controller.risk;

import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控报表控制器。
 * <p>提供风险日报、周报等报表生成和导出功能（当前为模拟数据）。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/risk/report")
@RequiredArgsConstructor
public class RiskReportController {

    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('risk:report:list')")
    public Result<Map<String, Object>> dailyReport(@RequestParam(required = false) String date) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportDate", date != null ? date : java.time.LocalDate.now().toString());
        report.put("totalAccounts", 1580);
        report.put("activeAccounts", 423);
        report.put("totalEquity", 125_680_000.00);
        report.put("totalMargin", 42_350_000.00);
        report.put("averageRiskRatio", 62.5);
        report.put("highRiskAccounts", 12);
        report.put("todayLiquidations", 2);
        report.put("todayAlerts", 5);
        report.put("riskDistribution", List.of(
                Map.of("range", "< 50%", "count", 210, "percentage", 49.6),
                Map.of("range", "50-80%", "count", 140, "percentage", 33.1),
                Map.of("range", "80-100%", "count", 55, "percentage", 13.0),
                Map.of("range", "> 100%", "count", 18, "percentage", 4.3)
        ));
        return Result.success(report);
    }

    @GetMapping("/weekly")
    @PreAuthorize("hasAuthority('risk:report:list')")
    public Result<Map<String, Object>> weeklyReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("startDate", java.time.LocalDate.now().minusDays(7).toString());
        report.put("endDate", java.time.LocalDate.now().toString());
        report.put("totalLiquidations", 8);
        report.put("totalAlerts", 35);
        report.put("avgRiskRatioTrend", List.of(58.2, 60.1, 59.5, 61.3, 63.0, 62.8, 62.5));
        report.put("highRiskTrend", List.of(8, 9, 10, 11, 10, 12, 12));
        report.put("liquidationBySymbol", List.of(
                Map.of("symbol", "HSI", "count", 3),
                Map.of("symbol", "ES", "count", 2),
                Map.of("symbol", "CL", "count", 2),
                Map.of("symbol", "GC", "count", 1)
        ));
        return Result.success(report);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('risk:report:export')")
    public Result<String> export(@RequestParam(defaultValue = "daily") String type) {
        return Result.success("报表导出任务已提交，请稍后在下载中心查看");
    }
}
