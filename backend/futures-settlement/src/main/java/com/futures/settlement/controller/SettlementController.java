package com.futures.settlement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.common.result.Result;
import com.futures.settlement.entity.DailySettlementEntity;
import com.futures.settlement.entity.MarginCallEntity;
import com.futures.settlement.entity.ReconciliationEntity;
import com.futures.settlement.entity.ReconciliationDiffEntity;
import com.futures.settlement.entity.RegulatoryReportEntity;
import com.futures.settlement.service.ReconciliationService;
import com.futures.settlement.service.ReportGenerator;
import com.futures.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 清结算控制器。
 * <p>提供日终结算、结算历史查询、追保管理、对账、监管报表等REST API。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final ReconciliationService reconciliationService;
    private final ReportGenerator reportGenerator;

    // ==================== 日终结算 ====================

    /**
     * 执行日终结算。
     */
    @PostMapping("/daily")
    public Result<DailySettlementEntity> executeDailySettlement(
            @RequestParam Long userId,
            @RequestParam BigDecimal beginEquity,
            @RequestParam BigDecimal realizedPnl,
            @RequestParam BigDecimal unrealizedPnl,
            @RequestParam(required = false, defaultValue = "0") BigDecimal fee,
            @RequestParam(required = false, defaultValue = "0") BigDecimal netDeposit) {
        DailySettlementEntity result = settlementService.executeDailySettlement(
                userId, beginEquity, realizedPnl, unrealizedPnl, fee, netDeposit);
        return Result.success(result);
    }

    /**
     * 批量结算所有用户。
     */
    @PostMapping("/batch")
    public Result<Map<String, Object>> batchSettlement() {
        Map<String, Object> result = settlementService.batchSettleAllUsers(LocalDate.now());
        return Result.success(result);
    }

    /**
     * 查询结算历史（分页）。
     */
    @GetMapping("/history")
    public Result<Page<DailySettlementEntity>> getSettlementHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        Page<DailySettlementEntity> result = settlementService.getSettlementHistory(userId, start, end, page, size);
        return Result.success(result);
    }

    /**
     * 获取最近一次结算。
     */
    @GetMapping("/latest")
    public Result<DailySettlementEntity> getLatestSettlement(@RequestParam Long userId) {
        DailySettlementEntity result = settlementService.getLatestSettlement(userId);
        return result != null ? Result.success(result) : Result.success(null);
    }

    // ==================== 结算单 ====================

    /**
     * 生成结算单文本。
     */
    @GetMapping("/statement")
    public Result<String> generateStatement(
            @RequestParam Long userId,
            @RequestParam Long settlementId) {
        String statement = settlementService.generateSettlementStatement(userId, settlementId);
        return Result.success(statement);
    }

    // ==================== 追保管理 ====================

    /**
     * 检查并处理追保。
     */
    @PostMapping("/margin-call")
    public Result<MarginCallEntity> processMarginCall(
            @RequestParam Long userId,
            @RequestParam BigDecimal currentEquity,
            @RequestParam BigDecimal currentMargin,
            @RequestParam(required = false, defaultValue = "0") BigDecimal settlementPrice) {
        MarginCallEntity result = settlementService.processMarginCall(
                userId, currentEquity, currentMargin, settlementPrice);
        return Result.success(result);
    }

    /**
     * 批量检查追保。
     */
    @PostMapping("/margin-call/batch")
    public Result<Map<String, Object>> batchCheckMarginCalls() {
        Map<String, Object> result = settlementService.batchCheckMarginCalls();
        return Result.success(result);
    }

    /**
     * 查询用户追保记录。
     */
    @GetMapping("/margin-call/history")
    public Result<List<MarginCallEntity>> getMarginCallHistory(@RequestParam Long userId) {
        List<MarginCallEntity> result = settlementService.getMarginCallHistory(userId);
        return Result.success(result);
    }

    // ==================== 对账管理 ====================

    /**
     * 执行交易所对账。
     */
    @PostMapping("/reconciliation/exchange")
    public Result<ReconciliationEntity> reconcileExchange(@RequestParam String date) {
        ReconciliationEntity result = reconciliationService.reconcileWithExchange(LocalDate.parse(date));
        return Result.success(result);
    }

    /**
     * 执行银行对账。
     */
    @PostMapping("/reconciliation/bank")
    public Result<ReconciliationEntity> reconcileBank(@RequestParam String date) {
        ReconciliationEntity result = reconciliationService.reconcileWithBank(LocalDate.parse(date));
        return Result.success(result);
    }

    /**
     * 执行完整对账。
     */
    @PostMapping("/reconciliation/full")
    public Result<List<ReconciliationEntity>> runFullReconciliation(@RequestParam String date) {
        List<ReconciliationEntity> result = reconciliationService.runFullReconciliation(LocalDate.parse(date));
        return Result.success(result);
    }

    /**
     * 查询对账历史。
     */
    @GetMapping("/reconciliation/history")
    public Result<Page<ReconciliationEntity>> getReconciliationHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        Page<ReconciliationEntity> result = reconciliationService.getReconciliationHistory(start, end, page, size);
        return Result.success(result);
    }

    /**
     * 查询对账差异明细。
     */
    @GetMapping("/reconciliation/diffs")
    public Result<List<ReconciliationDiffEntity>> getReconciliationDiffs(
            @RequestParam Long reconciliationId) {
        List<ReconciliationDiffEntity> result = reconciliationService.getReconciliationDiffs(reconciliationId);
        return Result.success(result);
    }

    // ==================== 监管报表 ====================

    /**
     * 生成日报表。
     */
    @PostMapping("/report/daily")
    public Result<RegulatoryReportEntity> generateDailyReport(@RequestParam String date) throws Exception {
        RegulatoryReportEntity result = reportGenerator.generateDailyReport(LocalDate.parse(date));
        return Result.success(result);
    }

    /**
     * 生成月报表。
     */
    @PostMapping("/report/monthly")
    public Result<RegulatoryReportEntity> generateMonthlyReport(@RequestParam String yearMonth) throws Exception {
        RegulatoryReportEntity result = reportGenerator.generateMonthlyReport(yearMonth);
        return Result.success(result);
    }

    /**
     * 查询报表历史。
     */
    @GetMapping("/report/history")
    public Result<List<RegulatoryReportEntity>> getReportHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String reportType) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        List<RegulatoryReportEntity> result = reportGenerator.getReportHistory(start, end, reportType);
        return Result.success(result);
    }
}
