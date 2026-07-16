package com.hackfuture.core.model

import kotlinx.serialization.Serializable

/**
 * 实时报价数据模型 — 用于行情概览列表，展示单个合约的实时快照。
 */
@Serializable
data class Quote(
    /** 合约代码，如 "HK00700" */
    val symbol: String,
    /** 合约中文名称，如 "腾讯控股" */
    val name: String = "",
    /** 交易所，如 "HKEX" */
    val exchange: String = "",
    /** 品种分类 */
    val category: QuoteCategory = QuoteCategory.STOCK,
    /** 最新成交价 */
    val lastPrice: Double,
    /** 涨跌额（相对于前收盘） */
    val change: Double,
    /** 涨跌幅百分比 */
    val changePercent: Double,
    /** 今日开盘价 */
    val open: Double,
    /** 今日最高价 */
    val high: Double,
    /** 今日最低价 */
    val low: Double,
    /** 成交量 */
    val volume: Long,
    /** 成交额 */
    val turnover: Double = 0.0,
    /** 持仓量 */
    val openInterest: Long = 0,
) {
    /** 价格涨跌方向 */
    val direction: PriceDirection
        get() = when {
            change > 0 -> PriceDirection.UP
            change < 0 -> PriceDirection.DOWN
            else -> PriceDirection.FLAT
        }
}

/** 品种分类 */
@Serializable
enum class QuoteCategory(val label: String) {
    STOCK("港股"),
    COMMODITY("商品期货"),
}

/** 排序模式 */
@Serializable
enum class SortMode(val label: String) {
    SYMBOL("合约"),
    PRICE("最新价"),
    CHANGE("涨跌幅"),
    VOLUME("成交量"),
}
