package com.hackfuture.core.network.alltick

import kotlinx.serialization.Serializable

/** AllTick 实时报价响应 */
@Serializable
data class AllTickQuoteResponse(
    val ret: Int,
    val msg: String,
    val data: List<AllTickQuote> = emptyList(),
)

/** AllTick 实时报价项 */
@Serializable
data class AllTickQuote(
    val symbol: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val turnover: Double,
    val change: Double,
    val changePercent: Double,
)

/** AllTick K 线响应 */
@Serializable
data class AllTickKlineResponse(
    val ret: Int,
    val msg: String,
    val data: List<AllTickKline> = emptyList(),
)

/** AllTick K 线数据项 */
@Serializable
data class AllTickKline(
    val symbol: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val turnover: Double,
    val timestamp: Long,
)
