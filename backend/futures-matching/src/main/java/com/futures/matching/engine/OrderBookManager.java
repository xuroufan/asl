package com.futures.matching.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多品种撮合引擎管理器。
 * <p>
 * 每个交易品种（Symbol）拥有独立的 {@link MatchingEngine} 实例。
 * 使用 {@link ConcurrentHashMap} 保证并发安全。
 * 每个 Symbol 的撮合由单一线程处理（通过 Disruptor），避免锁竞争。
 * </p>
 */
@Slf4j
@Component
public class OrderBookManager {

    /** Symbol -> MatchingEngine 映射 */
    private final ConcurrentHashMap<String, MatchingEngine> engines;

    public OrderBookManager() {
        this.engines = new ConcurrentHashMap<>(64);
    }

    /**
     * 获取或创建指定合约的撮合引擎。
     *
     * @param symbol 合约代码
     * @return 撮合引擎实例
     */
    public MatchingEngine getOrCreateEngine(String symbol) {
        return engines.computeIfAbsent(symbol, sym -> {
            log.info("创建新撮合引擎: symbol={}", sym);
            return new MatchingEngine(sym);
        });
    }

    /**
     * 获取指定合约的撮合引擎（不存在返回 null）。
     *
     * @param symbol 合约代码
     * @return 撮合引擎实例，null 表示不存在
     */
    public MatchingEngine getEngine(String symbol) {
        return engines.get(symbol);
    }

    /**
     * 获取指定合约的订单簿（用于深度查询）。
     *
     * @param symbol 合约代码
     * @return 订单簿，null 表示不存在
     */
    public OrderBook getOrderBook(String symbol) {
        MatchingEngine engine = engines.get(symbol);
        return engine != null ? engine.getOrderBook() : null;
    }

    /**
     * 删除指定合约的撮合引擎。
     *
     * @param symbol 合约代码
     * @return 被删除的撮合引擎
     */
    public MatchingEngine remove(String symbol) {
        MatchingEngine removed = engines.remove(symbol);
        if (removed != null) {
            log.info("删除撮合引擎: symbol={}", symbol);
        }
        return removed;
    }

    /** 获取所有活跃的 Symbol 列表 */
    public Set<String> getSymbols() {
        return engines.keySet();
    }

    /** 获取所有撮合引擎 */
    public Map<String, MatchingEngine> getAllEngines() {
        return engines;
    }

    /** 获取管理的引擎数量 */
    public int size() {
        return engines.size();
    }

    /** 清除所有引擎 */
    public void clear() {
        engines.clear();
        log.info("已清除所有撮合引擎");
    }
}
