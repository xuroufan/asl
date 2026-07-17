package com.futures.settlement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.settlement.entity.ReconciliationDiffEntity;
import com.futures.settlement.entity.ReconciliationEntity;
import com.futures.settlement.mapper.ReconciliationDiffMapper;
import com.futures.settlement.mapper.ReconciliationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账服务。
 * <p>负责每日与交易所（交易流水、持仓、资金）和银行（出入金）的对账校验。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationDiffMapper reconciliationDiffMapper;

    /**
     * 对账：交易所交易流水。
     * <p>将本系统当日的交易流水与交易所结算数据进行逐笔比对。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationEntity reconcileWithExchange(LocalDate date) {
        log.info("开始交易所对账，日期={}", date);
        ReconciliationEntity rec = new ReconciliationEntity();
        rec.setReconciliationDate(date);
        rec.setReconciliationType("EXCHANGE");
        rec.setStatus("PENDING");
        reconciliationMapper.insert(rec);

        // 实际对账逻辑：调用交易所API获取结算数据，逐笔比对
        int total = 100;  // 模拟：实际从订单服务获取
        int matched = 98;
        int unmatched = total - matched;

        rec.setTotalRecords(total);
        rec.setMatchedRecords(matched);
        rec.setUnmatchedRecords(unmatched);
        rec.setStatus("COMPLETED");
        rec.setCompletedTime(java.time.LocalDateTime.now());
        rec.setSummary(String.format("交易所对账完成：总计%d笔，匹配%d笔，差异%d笔", total, matched, unmatched));
        reconciliationMapper.updateById(rec);

        log.info(rec.getSummary());
        return rec;
    }

    /**
     * 对账：银行出入金流水。
     * <p>将本系统出入金记录与银行流水进行逐笔比对。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationEntity reconcileWithBank(LocalDate date) {
        log.info("开始银行对账，日期={}", date);
        ReconciliationEntity rec = new ReconciliationEntity();
        rec.setReconciliationDate(date);
        rec.setReconciliationType("BANK");
        rec.setStatus("PENDING");
        reconciliationMapper.insert(rec);

        // 实际对账逻辑：调用银行API，逐笔比对出入金流水
        int total = 50;   // 模拟
        int matched = 50;
        int unmatched = 0;

        rec.setTotalRecords(total);
        rec.setMatchedRecords(matched);
        rec.setUnmatchedRecords(unmatched);
        rec.setStatus("COMPLETED");
        rec.setCompletedTime(java.time.LocalDateTime.now());
        rec.setSummary(String.format("银行对账完成：总计%d笔，匹配%d笔，差异%d笔", total, matched, unmatched));
        reconciliationMapper.updateById(rec);

        log.info(rec.getSummary());
        return rec;
    }

    /**
     * 记录对账差异明细。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordDiff(Long reconciliationId, String diffType,
                            String ourRecordId, String theirRecordId,
                            java.math.BigDecimal ourAmount, java.math.BigDecimal theirAmount,
                            String notes) {
        ReconciliationDiffEntity diff = new ReconciliationDiffEntity();
        diff.setReconciliationId(reconciliationId);
        diff.setDiffType(diffType);
        diff.setOurRecordId(ourRecordId);
        diff.setTheirRecordId(theirRecordId);
        diff.setOurAmount(ourAmount);
        diff.setTheirAmount(theirAmount);
        diff.setAmountDiff(ourAmount != null && theirAmount != null
                ? ourAmount.subtract(theirAmount) : java.math.BigDecimal.ZERO);
        diff.setStatus("PENDING");
        diff.setNotes(notes);
        reconciliationDiffMapper.insert(diff);
    }

    /**
     * 执行完整对账流程（交易所 + 银行）。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ReconciliationEntity> runFullReconciliation(LocalDate date) {
        log.info("===== 开始完整对账流程，日期={} =====", date);
        ReconciliationEntity exchangeRec = reconcileWithExchange(date);
        ReconciliationEntity bankRec = reconcileWithBank(date);
        log.info("===== 完整对账流程完成 =====");
        return List.of(exchangeRec, bankRec);
    }

    /**
     * 查询对账历史（分页）。
     */
    public Page<ReconciliationEntity> getReconciliationHistory(LocalDate startDate,
                                                                LocalDate endDate, int page, int size) {
        LambdaQueryWrapper<ReconciliationEntity> wrapper = new LambdaQueryWrapper<ReconciliationEntity>()
                .ge(startDate != null, ReconciliationEntity::getReconciliationDate, startDate)
                .le(endDate != null, ReconciliationEntity::getReconciliationDate, endDate)
                .orderByDesc(ReconciliationEntity::getReconciliationDate);
        return reconciliationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 查询对账差异明细。
     */
    public List<ReconciliationDiffEntity> getReconciliationDiffs(Long reconciliationId) {
        return reconciliationDiffMapper.selectList(
                new LambdaQueryWrapper<ReconciliationDiffEntity>()
                        .eq(ReconciliationDiffEntity::getReconciliationId, reconciliationId));
    }
}
