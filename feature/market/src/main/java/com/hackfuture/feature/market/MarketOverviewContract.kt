package com.hackfuture.feature.market

import com.hackfuture.core.model.Quote
import com.hackfuture.core.model.QuoteCategory
import com.hackfuture.core.model.SortMode

/**
 * 行情看板页面状态 — 聚合所有行情概览所需数据。
 */
data class MarketOverviewState(
    val isLoading: Boolean = false,
    /** 完整合约报价列表 */
    val quotes: List<Quote> = emptyList(),
    /** 搜索关键词 */
    val searchQuery: String = "",
    /** 当前选中的品种分类筛选（null = 全部） */
    val selectedCategory: QuoteCategory? = null,
    /** 当前排序方式 */
    val sortMode: SortMode = SortMode.VOLUME,
    /** 排序方向：true = 降序 */
    val sortDescending: Boolean = true,
    /** 错误消息 */
    val error: String? = null,
    /** 最后更新时间戳 */
    val lastUpdated: Long = 0L,
    /** 连接状态 */
    val isConnected: Boolean = false,
) {
    /** 经过搜索、分类筛选、排序后的展示列表 */
    val displayQuotes: List<Quote>
        get() {
            var result = quotes
            // 分类筛选
            if (selectedCategory != null) {
                result = result.filter { it.category == selectedCategory }
            }
            // 关键词搜索
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().uppercase()
                result = result.filter {
                    it.symbol.contains(q) || it.name.contains(q, ignoreCase = true)
                }
            }
            // 排序
            result = when (sortMode) {
                SortMode.SYMBOL -> result.sortedBy { it.symbol }
                SortMode.PRICE -> result.sortedBy { it.lastPrice }
                SortMode.CHANGE -> result.sortedBy { it.changePercent }
                SortMode.VOLUME -> result.sortedBy { it.volume }
            }
            return if (sortDescending) result.reversed() else result
        }

    /** 上涨合约数量 */
    val upCount: Int get() = quotes.count { it.change > 0 }
    /** 下跌合约数量 */
    val downCount: Int get() = quotes.count { it.change < 0 }
    /** 平盘合约数量 */
    val flatCount: Int get() = quotes.count { it.change == 0.0 }
}

/** 用户操作意图 */
sealed class MarketOverviewIntent {
    data object LoadData : MarketOverviewIntent()
    data object Refresh : MarketOverviewIntent()
    data class Search(val query: String) : MarketOverviewIntent()
    data class SelectCategory(val category: QuoteCategory?) : MarketOverviewIntent()
    data class SelectSort(val mode: SortMode) : MarketOverviewIntent()
    data object ToggleSortDirection : MarketOverviewIntent()
    data class SelectSymbol(val symbol: String) : MarketOverviewIntent()
}

/** 单次副作用 */
sealed class MarketOverviewEffect {
    data class ShowError(val message: String) : MarketOverviewEffect()
    data class NavigateToDetail(val symbol: String) : MarketOverviewEffect()
}
