package com.hackfuture.core.network.alltick

import retrofit2.http.GET
import retrofit2.http.Query

interface AllTickApiService {

    /** 获取实时报价（不传 symbol 返回全部） */
    @GET("${AllTickConstants.PATH_PREFIX}/realtime")
    suspend fun getRealTime(
        @Query("token") token: String = AllTickConstants.API_TOKEN,
        @Query("companyId") companyId: String = AllTickConstants.COMPANY_ID,
        @Query("symbol") symbol: String? = null,
    ): AllTickQuoteResponse

    /** 获取 K 线数据 */
    @GET("${AllTickConstants.PATH_PREFIX}/kline")
    suspend fun getKline(
        @Query("token") token: String = AllTickConstants.API_TOKEN,
        @Query("companyId") companyId: String = AllTickConstants.COMPANY_ID,
        @Query("symbol") symbol: String,
        @Query("type") type: String,
        @Query("count") count: Int = 200,
    ): AllTickKlineResponse
}
