package com.hackfuture.feature.trading.navigation

object TradingRoutes {
    const val TRADING = "trading/{symbol}"

    fun createRoute(symbol: String = "BTCUSDT"): String = "trading/$symbol"
}
