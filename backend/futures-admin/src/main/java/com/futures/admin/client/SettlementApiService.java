package com.futures.admin.client;

import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 清结算服务 API 客户端 — 封装对 futures-settlement 微服务的 REST 调用。
 * <p>所有 API 从 futures-settlement 的 SettlementController 代理而来。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementApiService {

    private final RestTemplate restTemplate;

    @Value("${settlement.service.url:http://localhost:8087}")
    private String settlementServiceUrl;

    // ==================== 结算历史 ====================

    public Result<?> getSettlementHistory(Long userId, String startDate, String endDate, int page, int size) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(settlementServiceUrl + "/api/v1/settlement/history")
                    .queryParam("page", page).queryParam("size", size);
            if (userId != null) builder.queryParam("userId", userId);
            if (startDate != null) builder.queryParam("startDate", startDate);
            if (endDate != null) builder.queryParam("endDate", endDate);
            String url = builder.build().toUriString();
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<?>>() {}).getBody();
        } catch (Exception e) {
            log.warn("获取结算历史失败: {}", e.getMessage());
            return getMockSettlementHistory(page);
        }
    }

    // ==================== 对账管理 ====================

    public Result<?> getReconciliationHistory(String startDate, String endDate, int page, int size) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(settlementServiceUrl + "/api/v1/settlement/reconciliation/history")
                    .queryParam("page", page).queryParam("size", size);
            if (startDate != null) builder.queryParam("startDate", startDate);
            if (endDate != null) builder.queryParam("endDate", endDate);
            String url = builder.build().toUriString();
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<?>>() {}).getBody();
        } catch (Exception e) {
            log.warn("获取对账历史失败: {}", e.getMessage());
            return getMockReconciliationHistory(page);
        }
    }

    public Result<?> getReconciliationDiffs(Long reconciliationId) {
        try {
            String url = settlementServiceUrl + "/api/v1/settlement/reconciliation/diffs?reconciliationId=" + reconciliationId;
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<?>>() {}).getBody();
        } catch (Exception e) {
            log.warn("获取对账差异失败: {}", e.getMessage());
            return getMockReconciliationDiffs();
        }
    }

    // ==================== 报表 ====================

    public Result<?> getReportHistory(String startDate, String endDate, String reportType) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(settlementServiceUrl + "/api/v1/settlement/report/history");
            if (startDate != null) builder.queryParam("startDate", startDate);
            if (endDate != null) builder.queryParam("endDate", endDate);
            if (reportType != null) builder.queryParam("reportType", reportType);
            String url = builder.build().toUriString();
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<?>>() {}).getBody();
        } catch (Exception e) {
            log.warn("获取报表历史失败: {}", e.getMessage());
            return getMockReportHistory();
        }
    }

    // ==================== 模拟数据（降级） ====================

    private Result<?> getMockSettlementHistory(int page) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", (long) i);
            r.put("userId", 1000L + i);
            r.put("username", "user_" + String.format("%04d", i));
            r.put("settlementDate", LocalDate.now().minusDays(i % 7).toString());
            r.put("openingEquity", 100000 + i * 1000);
            r.put("closingEquity", 102000 + i * 500);
            r.put("dailyProfit", 2000 + i * 100);
            r.put("fee", 150 + i * 10);
            r.put("margin", 30000 + i * 1000);
            r.put("status", i % 5 == 0 ? "PENDING" : "SETTLED");
            r.put("createTime", LocalDate.now().minusDays(i % 7).atStartOfDay().toString());
            records.add(r);
        }
        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("records", records);
        pageResult.put("total", 150);
        pageResult.put("page", page);
        pageResult.put("size", 20);
        return Result.success(pageResult);
    }

    private Result<?> getMockReconciliationHistory(int page) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", (long) i);
            r.put("reconciliationDate", LocalDate.now().minusDays(i).toString());
            r.put("type", i % 2 == 0 ? "EXCHANGE" : "BANK");
            r.put("status", i == 1 ? "IN_PROGRESS" : "COMPLETED");
            r.put("ourTotal", 1256800.00 + i * 100);
            r.put("theirTotal", 1256800.00 + i * 100 + (i % 3 == 0 ? 50 : 0));
            r.put("diffCount", i % 3 == 0 ? 2 : 0);
            r.put("resolvedCount", i % 3 == 0 ? 1 : 0);
            r.put("createTime", LocalDate.now().minusDays(i).atStartOfDay().toString());
            records.add(r);
        }
        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("records", records);
        pageResult.put("total", 50);
        pageResult.put("page", page);
        pageResult.put("size", 20);
        return Result.success(pageResult);
    }

    private Result<?> getMockReconciliationDiffs() {
        List<Map<String, Object>> diffs = new ArrayList<>();
        Map<String, Object> d1 = new LinkedHashMap<>();
        d1.put("id", 1); d1.put("reconciliationId", 1); d1.put("diffType", "MISMATCH");
        d1.put("ourRecordId", "TRADE20240315001"); d1.put("theirRecordId", "EXT20240315001");
        d1.put("ourAmount", 15230.50); d1.put("theirAmount", 15230.00);
        d1.put("amountDiff", 0.50); d1.put("status", "PENDING");
        d1.put("notes", "手续费差异"); d1.put("createTime", "2024-03-15T16:30:00");
        diffs.add(d1);
        Map<String, Object> d2 = new LinkedHashMap<>();
        d2.put("id", 2); d2.put("reconciliationId", 1); d2.put("diffType", "MISSING");
        d2.put("ourRecordId", "TRADE20240315002"); d2.put("theirRecordId", "");
        d2.put("ourAmount", 5000.00); d2.put("theirAmount", 0.00);
        d2.put("amountDiff", 5000.00); d2.put("status", "PENDING");
        d2.put("notes", "我方有记录，对方缺"); d2.put("createTime", "2024-03-15T16:30:00");
        diffs.add(d2);
        return Result.success(diffs);
    }

    private Result<?> getMockReportHistory() {
        List<Map<String, Object>> reports = new ArrayList<>();
        reports.add(Map.of("id", 1, "reportType", "DAILY", "reportName", "日报表-20240315",
                "reportDate", "2024-03-15", "status", "PUBLISHED",
                "createTime", "2024-03-15T18:00:00"));
        reports.add(Map.of("id", 2, "reportType", "DAILY", "reportName", "日报表-20240314",
                "reportDate", "2024-03-14", "status", "PUBLISHED",
                "createTime", "2024-03-14T18:00:00"));
        reports.add(Map.of("id", 3, "reportType", "MONTHLY", "reportName", "月报表-202403",
                "reportDate", "2024-03-31", "status", "AUDITING",
                "createTime", "2024-03-31T18:00:00"));
        return Result.success(reports);
    }

    /** 获取财务统计看板数据（聚合数据，含降级） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFinanceDashboardData() {
        try {
            // 尝试获取真实数据
            var historyResult = getSettlementHistory(null, null, null, 1, 100);
            if (historyResult != null && historyResult.getData() instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) historyResult.getData();
                List<Map<String, Object>> records = (List<Map<String, Object>>) data.getOrDefault("records", List.of());

                BigDecimal totalFee = BigDecimal.ZERO;
                BigDecimal totalProfit = BigDecimal.ZERO;
                for (Map<String, Object> r : records) {
                    totalFee = totalFee.add(new BigDecimal(r.getOrDefault("fee", "0").toString()));
                    totalProfit = totalProfit.add(new BigDecimal(r.getOrDefault("dailyProfit", "0").toString()));
                }

                Map<String, Object> dashboard = new LinkedHashMap<>();
                dashboard.put("totalSettlements", records.size());
                dashboard.put("totalFee", totalFee.doubleValue());
                dashboard.put("totalProfit", totalProfit.doubleValue());
                dashboard.put("pendingSettlements", records.stream().filter(r -> "PENDING".equals(r.get("status"))).count());
                return dashboard;
            }
        } catch (Exception e) {
            log.warn("获取财务看板数据失败: {}", e.getMessage());
        }
        return getMockDashboardData();
    }

    private Map<String, Object> getMockDashboardData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayDeposit", 1250000.00);
        data.put("todayWithdraw", 380000.00);
        data.put("monthDeposit", 56800000.00);
        data.put("monthWithdraw", 15200000.00);
        data.put("totalFee", 456000.00);
        data.put("totalProfit", 3860000.00);
        data.put("pendingReconciliations", 3);
        data.put("pendingReports", 2);
        data.put("totalAccounts", 1580);
        data.put("activeAccounts", 423);

        // 出入金趋势（近7天）
        data.put("depositTrend", List.of(
                Map.of("date", "03-09", "deposit", 185000, "withdraw", 52000),
                Map.of("date", "03-10", "deposit", 220000, "withdraw", 48000),
                Map.of("date", "03-11", "deposit", 168000, "withdraw", 65000),
                Map.of("date", "03-12", "deposit", 310000, "withdraw", 38000),
                Map.of("date", "03-13", "deposit", 256000, "withdraw", 72000),
                Map.of("date", "03-14", "deposit", 198000, "withdraw", 55000),
                Map.of("date", "03-15", "deposit", 1250000, "withdraw", 380000)
        ));

        // 手续费收入趋势
        data.put("feeTrend", List.of(
                Map.of("date", "03-09", "fee", 18500),
                Map.of("date", "03-10", "fee", 22000),
                Map.of("date", "03-11", "fee", 19800),
                Map.of("date", "03-12", "fee", 26500),
                Map.of("date", "03-13", "fee", 23800),
                Map.of("date", "03-14", "fee", 21500),
                Map.of("date", "03-15", "fee", 28900)
        ));

        return data;
    }
}
