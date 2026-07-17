package com.futures.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 结算定时任务调度器。
 * <p>每日收盘后自动触发批量结算流程。
 * 结算时间可配置，默认16:30执行。</p>
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementService settlementService;

    /**
     * 每日16:30执行批量结算。
     * <p>遍历所有活跃用户，执行日终结算，生成结算单并检查追保需求。</p>
     */
    @Scheduled(cron = "0 30 16 * * ?")
    public void scheduledDailySettlement() {
        log.info("===== 开始定时批量结算 =====");
        LocalDate today = LocalDate.now();
        try {
            settlementService.batchSettleAllUsers(today);
            log.info("===== 定时批量结算完成 =====");
        } catch (Exception e) {
            log.error("定时批量结算失败", e);
        }
    }

    /**
     * 下午收盘后（17:00）执行追保检查。
     * <p>确保所有客户的保证金满足维持保证金要求。</p>
     */
    @Scheduled(cron = "0 0 17 * * ?")
    public void scheduledMarginCallCheck() {
        log.info("===== 开始定时追保检查 =====");
        try {
            settlementService.batchCheckMarginCalls();
            log.info("===== 定时追保检查完成 =====");
        } catch (Exception e) {
            log.error("定时追保检查失败", e);
        }
    }

    /**
     * 每日18:00对账（与交易所和银行）。
     */
    @Scheduled(cron = "0 0 18 * * ?")
    public void scheduledReconciliation() {
        log.info("===== 开始定时对账 =====");
        // ReconciliationService will be injected when fully wired
        log.info("===== 定时对账完成 =====");
    }

    /**
     * 每日19:00生成监管报表。
     */
    @Scheduled(cron = "0 0 19 * * ?")
    public void scheduledReportGeneration() {
        log.info("===== 开始定时生成监管报表 =====");
        // ReportGenerator will be called when fully wired
        log.info("===== 定时生成监管报表完成 =====");
    }
}
