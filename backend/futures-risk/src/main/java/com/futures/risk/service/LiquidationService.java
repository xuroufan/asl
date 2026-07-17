package com.futures.risk.service;

import com.futures.risk.entity.LiquidationRecordEntity;
import com.futures.risk.mapper.LiquidationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自动强平执行服务
 * <p>
 * 当风险度超过强平阈值时，自动生成强平指令。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidationService {

    private final LiquidationRecordMapper liquidationRecordMapper;

    /**
     * 执行强平
     *
     * @param userId 用户ID
     * @param reason 触发原因
     */
    public void executeLiquidation(String userId, String reason) {
        // 1. 查询用户所有持仓，按浮动亏损从大到小排序
        // TODO: 从账户服务获取持仓列表

        log.warn("执行强平: userId={}, reason={}", userId, reason);

        // 2. 对每个亏损最大的持仓生成强平订单
        // TODO: 遍历持仓列表，按亏损排序

        // 模拟：强平一笔HSI合约
        String symbol = "HSI";
        int volume = 1;
        BigDecimal price = new BigDecimal("18000.00");

        // 3. 生成强平订单并发送给撮合引擎
        // TODO: 调用撮合引擎Feign接口发送强平市价单

        // 4. 记录强平记录
        LiquidationRecordEntity record = new LiquidationRecordEntity();
        record.setUserId(userId);
        record.setSymbol(symbol);
        record.setDirection("BUY");
        record.setVolume(volume);
        record.setLiquidationPrice(price);
        record.setRiskRatio(new BigDecimal("120.00"));
        record.setReason(reason);
        record.setStatus(1); // 已执行
        record.setCreatedAt(LocalDateTime.now());
        liquidationRecordMapper.insert(record);

        log.info("强平记录已保存: userId={}, symbol={}, volume={}, price={}",
                userId, symbol, volume, price);
    }

    /**
     * 按亏损排序执行强平：亏损最大的持仓优先平仓
     *
     * @param userId   用户ID
     * @param symbols  持仓合约列表（已按亏损排序）
     * @param targetRiskRatio 目标风险度（强平至该阈值以下）
     */
    public void liquidationByLossSort(String userId, java.util.List<String> symbols,
                                       BigDecimal targetRiskRatio) {
        for (String symbol : symbols) {
            // TODO: 对每个持仓执行强平
            log.info("强平持仓: userId={}, symbol={}", userId, symbol);
        }
    }
}
