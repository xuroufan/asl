package com.hackfuture.core.network

import com.hackfuture.core.model.MarketQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

/**
 * 实时行情数据引擎
 *
 * 参考 Shinny Futures DataManager 的设计思路。
 * 统一管理所有合约的实时行情，提供响应式数据流。
 *
 * 功能：
 * - 行情数据缓存 (ConcurrentHashMap)
 * - 响应式订阅 (StateFlow)
 * - 集合合约最新行情
 * - 涨跌幅计算
 */
object MarketDataEngine {

    /** 所有合约的实时行情 */
    private val _quotes = ConcurrentHashMap<String, MarketQuote>()

    /** 自选合约列表 */
    private val _watchlist = MutableStateFlow<List<String>>(emptyList())

    /** 活跃合约 ID */
    private val _activeInstrument = MutableStateFlow<String?>(null)

    /** 所有行情的 StateFlow */
    private val _allQuotes = MutableStateFlow<Map<String, MarketQuote>>(emptyMap())

    /** 最新行情快照 */
    val allQuotes: StateFlow<Map<String, MarketQuote>> = _allQuotes.asStateFlow()

    /** 自选列表 */
    val watchlist: StateFlow<List<String>> = _watchlist.asStateFlow()

    /** 当前选中合约 */
    val activeInstrument: StateFlow<String?> = _activeInstrument.asStateFlow()

    /**
     * 更新单个合约行情
     */
    fun updateQuote(instrumentId: String, quote: MarketQuote) {
        _quotes[instrumentId] = quote
        _allQuotes.value = _quotes.toMap()
    }

    /**
     * 批量更新行情（WebSocket 推送场景）
     */
    fun updateQuotes(updates: Map<String, MarketQuote>) {
        _quotes.putAll(updates)
        _allQuotes.value = _quotes.toMap()
    }

    /**
     * 获取指定合约行情
     */
    fun getQuote(instrumentId: String): MarketQuote? = _quotes[instrumentId]

    /**
     * 设置当前选中合约
     */
    fun setActiveInstrument(instrumentId: String?) {
        _activeInstrument.value = instrumentId
    }

    /**
     * 切换自选
     */
    fun toggleWatchlist(instrumentId: String) {
        val current = _watchlist.value.toMutableList()
        if (current.contains(instrumentId)) {
            current.remove(instrumentId)
        } else {
            current.add(instrumentId)
        }
        _watchlist.value = current
    }

    /**
     * 清空所有行情
     */
    fun clear() {
        _quotes.clear()
        _allQuotes.value = emptyMap()
    }

    // ============ 辅助方法 ============

    /**
     * 计算涨跌幅
     */
    fun calculateChange(lastPrice: BigDecimal, preClose: BigDecimal): BigDecimal =
        if (preClose > BigDecimal.ZERO) lastPrice - preClose
        else BigDecimal.ZERO

    /**
     * 计算涨跌幅百分比
     */
    fun calculateChangePercent(lastPrice: BigDecimal, preClose: BigDecimal): BigDecimal =
        if (preClose > BigDecimal.ZERO)
            (lastPrice - preClose) / preClose * BigDecimal(100)
        else BigDecimal.ZERO
}
