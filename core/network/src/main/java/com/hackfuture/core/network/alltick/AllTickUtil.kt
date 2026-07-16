package com.hackfuture.core.network.alltick

import com.hackfuture.core.model.CandleData
import com.hackfuture.core.model.CandleInterval
import com.hackfuture.core.model.MarketData
import com.hackfuture.core.model.Quote
import com.hackfuture.core.model.QuoteCategory

/** 将 App 格式的 symbol（HK00700）转为 AllTick 格式（HK.00700） */
fun String.toAllTickSymbol(): String {
    // HK00700 → HK.00700
    if (length > 2) {
        return "${take(2)}.${drop(2)}"
    }
    return this
}

/** 将 AllTick 格式的 symbol（HK.00700）转为 App 格式（HK00700） */
fun String.fromAllTickSymbol(): String = replace(".", "")

/** 将 App 端的 CandleInterval 映射为 AllTick 的 type 参数 */
fun CandleInterval.toAllTickType(): String = when (this) {
    CandleInterval.M1 -> "1"
    CandleInterval.M5 -> "5"
    CandleInterval.M15 -> "15"
    CandleInterval.M30 -> "30"
    CandleInterval.H1 -> "60"
    CandleInterval.H4 -> "240"
    CandleInterval.D1 -> "D"
    CandleInterval.W1 -> "W"
}

/** 将 AllTick Quote 映射为 App 的 MarketData */
fun AllTickQuote.toMarketData(): MarketData {
    val appSymbol = symbol.fromAllTickSymbol()
    val name = hkStockNames[appSymbol] ?: appSymbol
    return MarketData(
        symbol = appSymbol,
        name = name,
        exchange = "HKEX",
        price = close,
        change = change,
        changePercent = changePercent,
        high = high,
        low = low,
        open = open,
        close = close,
        volume = volume,
        timestamp = timestamp,
    )
}

/** 将 AllTick Quote 映射为 App 的 Quote */
fun AllTickQuote.toWatchlistQuote(): Quote {
    val appSymbol = symbol.fromAllTickSymbol()
    val name = hkStockNames[appSymbol] ?: appSymbol
    return Quote(
        symbol = appSymbol,
        name = name,
        exchange = "HKEX",
        lastPrice = close,
        change = change,
        changePercent = changePercent,
        open = open,
        high = high,
        low = low,
        volume = volume,
        category = QuoteCategory.STOCK,
    )
}

/** 将 AllTick Kline 映射为 App 的 CandleData */
fun AllTickKline.toCandleData(symbol: String): CandleData = CandleData(
    symbol = symbol,
    interval = CandleInterval.D1,
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume,
    timestamp = timestamp,
)

/** 港股代码 → 中文名 */
private val hkStockNames = mapOf(
    "HK00001" to "长和",
    "HK00005" to "汇丰控股",
    "HK00700" to "腾讯控股",
    "HK00941" to "中国移动",
    "HK01299" to "友邦保险",
    "HK00388" to "香港交易所",
    "HK01810" to "小米集团",
    "HK03690" to "美团",
    "HK09618" to "京东集团",
    "HK09888" to "百度集团",
    "HK09988" to "阿里巴巴",
    "HK09999" to "网易",
)
