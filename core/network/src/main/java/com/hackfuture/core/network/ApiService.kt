package com.hackfuture.core.network

import com.hackfuture.core.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API 接口定义
 */
interface ApiService {

    // ==================== 认证 ====================

    @POST(ApiConstants.Endpoints.AUTH_LOGIN)
    suspend fun login(@Body request: LoginRequest): ApiResult

    @POST(ApiConstants.Endpoints.AUTH_REGISTER)
    suspend fun register(@Body request: RegisterRequest): ApiResult

    @POST(ApiConstants.Endpoints.AUTH_REFRESH)
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResult

    // ==================== 行情 ====================

    @GET(ApiConstants.Endpoints.MARKET_TICKER)
    suspend fun getTicker(@Query("symbol") symbol: String): ApiResult

    @GET(ApiConstants.Endpoints.MARKET_TICKER)
    suspend fun getAllTickers(): ApiResult

    @GET(ApiConstants.Endpoints.MARKET_CANDLES)
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100,
    ): ApiResult

    @GET(ApiConstants.Endpoints.MARKET_ORDER_BOOK)
    suspend fun getOrderBook(
        @Query("symbol") symbol: String,
        @Query("depth") depth: Int = 10,
    ): ApiResult

    // ==================== 订单 ====================

    @POST(ApiConstants.Endpoints.ORDER_CREATE)
    suspend fun placeOrder(@Body request: PlaceOrderRequest): ApiResult

    @POST(ApiConstants.Endpoints.ORDER_CANCEL)
    suspend fun cancelOrder(@Body request: CancelOrderRequest): ApiResult

    @GET(ApiConstants.Endpoints.ORDER_HISTORY)
    suspend fun getOrderHistory(
        @Query("symbol") symbol: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): ApiResult

    // ==================== 持仓 ====================

    @GET(ApiConstants.Endpoints.POSITION_LIST)
    suspend fun getPositions(@Query("symbol") symbol: String? = null): ApiResult

    @POST(ApiConstants.Endpoints.POSITION_CLOSE)
    suspend fun closePosition(@Body request: ClosePositionRequest): ApiResult

    // ==================== 账户 ====================

    @GET(ApiConstants.Endpoints.ACCOUNT_BALANCE)
    suspend fun getBalances(): ApiResult

    @GET(ApiConstants.Endpoints.ACCOUNT_TRANSACTIONS)
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): ApiResult
}
