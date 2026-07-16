package com.hackfuture.core.network

object ApiConstants {
    /** 后端网关地址（本地开发用 10.0.2.2 映射宿主机 localhost） */
    const val BASE_URL = "http://10.0.2.2:8088/"
    const val WS_URL = ""

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    const val MAX_RETRIES = 3
    const val RETRY_BASE_DELAY_MS = 1000L
    const val API_VERSION = "v1"

    object Endpoints {
        // Auth — 匹配 futures-account 模块
        const val AUTH_LOGIN = "api/$API_VERSION/auth/login"
        const val AUTH_REGISTER = "api/$API_VERSION/auth/register"
        const val AUTH_REFRESH = "api/$API_VERSION/auth/refresh"

        // Market — 匹配 futures-market 模块
        const val MARKET_QUOTE = "api/$API_VERSION/market/quote"
        const val MARKET_ALL_QUOTES = "api/$API_VERSION/market/all-quotes"
        const val MARKET_DEPTH = "api/$API_VERSION/market/depth"
        const val MARKET_KLINE = "api/$API_VERSION/market/kline"
        const val MARKET_TRADES = "api/$API_VERSION/market/trades"
        const val MARKET_SYMBOLS = "api/$API_VERSION/market/symbols"

        // 兼容旧路径（映射到新后端）
        const val MARKET_TICKER = MARKET_QUOTE
        const val MARKET_CANDLES = MARKET_KLINE
        const val MARKET_ORDER_BOOK = MARKET_DEPTH

        // Order — 匹配 futures-order 模块
        const val ORDER_CREATE = "api/$API_VERSION/order/create"
        const val ORDER_CANCEL = "api/$API_VERSION/order/cancel"
        const val ORDER_HISTORY = "api/$API_VERSION/order/history"

        // Position — 匹配 futures-order 模块
        const val POSITION_LIST = "api/$API_VERSION/position/list"
        const val POSITION_CLOSE = "api/$API_VERSION/position/close"

        // Account — 匹配 futures-account 模块
        const val ACCOUNT_BALANCE = "api/$API_VERSION/account/balance"
        const val ACCOUNT_TRANSACTIONS = "api/$API_VERSION/account/transactions"
    }
}
