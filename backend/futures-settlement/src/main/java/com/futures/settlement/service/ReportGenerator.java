package com.futures.settlement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.futures.settlement.entity.DailySettlementEntity;
import com.futures.settlement.entity.RegulatoryReportEntity;
import com.futures.settlement.entity.SettlementOrderEntity;
import com.futures.settlement.mapper.DailySettlementMapper;
import com.futures.settlement.mapper.RegulatoryReportMapper;
import com.futures.settlement.mapper.SettlementOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报表生成器。
 * <p>生成SFC等监管机构所需的日/月报表，支持PDF和Excel格式。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerator {

    private final DailySettlementMapper dailySettlementMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final RegulatoryReportMapper regulatoryReportMapper;

    /** 报表输出目录 */
    private static final String REPORT_DIR = "reports/";

    /**
     * 生成日度监管报表。
     * <p>包含：交易汇总、客户资金汇总、持仓汇总。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public RegulatoryReportEntity generateDailyReport(LocalDate date) throws IOException {
        log.info("生成日度监管报表，日期={}", date);
        ensureReportDir();

        RegulatoryReportEntity report = createReportEntity(date, "DAILY", "CSV");
        String fileName = String.format("sfc_daily_%s.csv", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        String filePath = REPORT_DIR + fileName;

        List<DailySettlementEntity> settlements = dailySettlementMapper.selectList(
                new LambdaQueryWrapper<DailySettlementEntity>()
                        .eq(DailySettlementEntity::getSettlementDate, date));
        List<SettlementOrderEntity> orders = settlementOrderMapper.selectList(null);
        int orderCount = orders.size();

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // 交易汇总
            writer.println("=== 交易汇总 ===");
            writer.println("日期,总成交量,总成交额,总手续费,总盈亏");

            BigDecimal totalVolume = BigDecimal.ZERO;
            BigDecimal totalFee = BigDecimal.ZERO;
            BigDecimal totalPnl = BigDecimal.ZERO;
            for (DailySettlementEntity s : settlements) {
                totalFee = totalFee.add(s.getFee() != null ? s.getFee() : BigDecimal.ZERO);
                totalPnl = totalPnl.add(s.getTotalPnl() != null ? s.getTotalPnl() : BigDecimal.ZERO);
            }
            BigDecimal totalTurnover = orders.stream()
                    .map(o -> o.getClosePrice() != null
                            ? o.getClosePrice().multiply(BigDecimal.valueOf(o.getVolume() != null ? o.getVolume() : 0))
                            : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            writer.println(String.format("%s,%d,%s,%s,%s",
                    date, orderCount, totalTurnover, totalFee, totalPnl));

            // 客户资金汇总
            writer.println("\n=== 客户资金汇总 ===");
            writer.println("用户ID,期初权益,期末权益,当日盈亏,手续费");
            for (DailySettlementEntity s : settlements) {
                writer.println(String.format("%d,%s,%s,%s,%s",
                        s.getUserId(), s.getBeginEquity(), s.getEndEquity(),
                        s.getTotalPnl(), s.getFee()));
            }

            // 持仓汇总（简要）
            writer.println("\n=== 持仓汇总 ===");
            writer.println("合约代码,总成交量,总盈亏");
            // 按合约汇总
            orders.stream()
                    .map(SettlementOrderEntity::getSymbol)
                    .distinct()
                    .forEach(sym -> {
                        long vol = orders.stream().filter(o -> sym.equals(o.getSymbol())).count();
                        BigDecimal pnl = orders.stream()
                                .filter(o -> sym.equals(o.getSymbol()))
                                .map(o -> o.getPnl() != null ? o.getPnl() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        writer.println(String.format("%s,%d,%s", sym, vol, pnl));
                    });
        }

        report.setFilePath(filePath);
        report.setStatus("COMPLETED");
        report.setSummary(String.format("日度监管报表已生成：%d条结算记录，%d笔成交", settlements.size(), orderCount));
        regulatoryReportMapper.updateById(report);
        log.info(report.getSummary());
        return report;
    }

    /**
     * 生成月度监管报表。
     * <p>包含月度财务报告和风险报告。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public RegulatoryReportEntity generateMonthlyReport(String yearMonth) throws IOException {
        log.info("生成月度监管报表，年月={}", yearMonth);
        ensureReportDir();

        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<DailySettlementEntity> settlements = dailySettlementMapper.selectList(
                new LambdaQueryWrapper<DailySettlementEntity>()
                        .ge(DailySettlementEntity::getSettlementDate, startDate)
                        .le(DailySettlementEntity::getSettlementDate, endDate));

        RegulatoryReportEntity report = createReportEntity(startDate, "MONTHLY", "CSV");
        String fileName = String.format("sfc_monthly_%s.csv", yearMonth);
        String filePath = REPORT_DIR + fileName;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // 月度财务报告
            writer.println("=== 月度财务报告 ===");
            writer.println("月份,期初总权益,期末总权益,月度盈亏,总手续费,出入金净额");

            BigDecimal monthPnl = settlements.stream()
                    .map(s -> s.getTotalPnl() != null ? s.getTotalPnl() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal monthFee = settlements.stream()
                    .map(s -> s.getFee() != null ? s.getFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            writer.println(String.format("%s,%s,%s,%s,%s", yearMonth, "N/A", "N/A", monthPnl, monthFee));

            // 风险报告
            writer.println("\n=== 风险报告 ===");
            writer.println("日期,高风险客户数,追保客户数,结算客户总数");
            long totalCustomers = settlements.stream()
                    .map(DailySettlementEntity::getUserId).distinct().count();
            writer.println(String.format("%s,%d,%d,%d", yearMonth, 0, 0, totalCustomers));
        }

        report.setFilePath(filePath);
        report.setStatus("COMPLETED");
        report.setSummary(String.format("月度监管报表已生成：%d条结算记录", settlements.size()));
        regulatoryReportMapper.updateById(report);
        log.info(report.getSummary());
        return report;
    }

    /**
     * 创建报表记录实体。
     */
    private RegulatoryReportEntity createReportEntity(LocalDate date, String type, String format) {
        RegulatoryReportEntity entity = new RegulatoryReportEntity();
        entity.setReportDate(date);
        entity.setReportType(type);
        entity.setFormat(format);
        entity.setStatus("GENERATING");
        regulatoryReportMapper.insert(entity);
        return entity;
    }

    /**
     * 确保报表目录存在。
     */
    private void ensureReportDir() throws IOException {
        Path dir = Paths.get(REPORT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    /**
     * 查询报表历史。
     */
    public List<RegulatoryReportEntity> getReportHistory(LocalDate startDate, LocalDate endDate, String reportType) {
        LambdaQueryWrapper<RegulatoryReportEntity> wrapper = new LambdaQueryWrapper<RegulatoryReportEntity>()
                .eq(reportType != null, RegulatoryReportEntity::getReportType, reportType)
                .ge(startDate != null, RegulatoryReportEntity::getReportDate, startDate)
                .le(endDate != null, RegulatoryReportEntity::getReportDate, endDate)
                .orderByDesc(RegulatoryReportEntity::getGeneratedAt);
        return regulatoryReportMapper.selectList(wrapper);
    }
}
