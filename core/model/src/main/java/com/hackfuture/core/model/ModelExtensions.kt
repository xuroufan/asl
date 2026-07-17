package com.hackfuture.core.model

import java.math.BigDecimal

// ============ MarketData ↔ MarketQuote 转换 ============

fun MarketData.toMarketQuote(): MarketQuote = MarketQuote(
    instrumentId = symbol,
    lastPrice = BigDecimal.valueOf(price),
    change = BigDecimal.valueOf(change),
    changePercent = BigDecimal.valueOf(changePercent),
    open = BigDecimal.valueOf(open),
    high = BigDecimal.valueOf(high),
    low = BigDecimal.valueOf(low),
    volume = volume,
    datetime = timestamp,
)

fun MarketQuote.toMarketData(): MarketData = MarketData(
    symbol = instrumentId,
    name = instrumentName,
    exchange = "",
    price = lastPrice.toDouble(),
    change = change.toDouble(),
    changePercent = changePercent.toDouble(),
    high = high.toDouble(),
    low = low.toDouble(),
    open = open.toDouble(),
    close = close.toDouble(),
    volume = volume,
    timestamp = datetime,
)

// ============ AccountInfo ↔ AccountBalance 转换 ============

fun AccountInfo.toAccountBalance(): AccountBalance = AccountBalance(
    asset = "CNY",
    free = available.toDouble(),
    locked = frozenMargin.toDouble(),
    total = balance.toDouble(),
)
