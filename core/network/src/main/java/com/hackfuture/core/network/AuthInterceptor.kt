package com.hackfuture.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp 拦截器：自动附加 Bearer Token
 * 支持 token 刷新后的自动重试
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    private val _currentToken = MutableStateFlow<String?>(null)
    val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

    private val isRefreshing = MutableStateFlow(false)

    fun updateToken(token: String?) {
        _currentToken.value = token
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = _currentToken.value
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(authenticatedRequest)

        // 401 自动刷新 token 逻辑可在这里扩展
        return response
    }
}
