package com.hackfuture.feature.market.navigation

object MarketRoutes {
    const val OVERVIEW = "market_overview"
    const val DETAIL = "market_detail/{symbol}"

    fun overviewRoute(): String = OVERVIEW
    fun detailRoute(symbol: String): String = "market_detail/$symbol"
}
