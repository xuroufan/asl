package com.hackfuture.core.network.auth

import com.hackfuture.core.network.ApiConstants
import com.hackfuture.core.network.security.TokenManager
import com.hackfuture.core.network.security.TokenRefreshResult
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
) : Authenticator {

    @Volatile private var isRefreshing = false
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) > 1) {
            Timber.w("Token retry exhausted")
            tokenManager.clearTokens()
            return null
        }
        synchronized(lock) {
            if (isRefreshing) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokenManager.getAccessToken() ?: return null}")
                    .build()
            }
            isRefreshing = true
        }
        return try {
            val refreshToken = tokenManager.getRefreshToken() ?: run {
                Timber.e("No refresh token")
                tokenManager.clearTokens()
                return null
            }
            val result = runBlocking { doRefresh(refreshToken) }
            when (result) {
                is TokenRefreshResult.Success -> {
                    tokenManager.saveTokens(result.accessToken, result.refreshToken, result.expiresIn)
                    Timber.i("Token refreshed")
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${result.accessToken}")
                        .build()
                }
                is TokenRefreshResult.Error -> {
                    if (result.isTerminal) tokenManager.clearTokens()
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Auth exception")
            null
        } finally {
            synchronized(lock) { isRefreshing = false }
        }
    }

    private suspend fun doRefresh(refreshToken: String): TokenRefreshResult {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val payload = JSONObject().apply { put("refreshToken", refreshToken) }
        val request = Request.Builder()
            .url("${ApiConstants.BASE_URL}${ApiConstants.Endpoints.AUTH_REFRESH}")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = runBlocking { client.newCall(request).execute() }
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return TokenRefreshResult.Error("Empty body")
            val json = JSONObject(body)
            return TokenRefreshResult.Success(
                accessToken = json.getString("accessToken"),
                refreshToken = json.getString("refreshToken"),
                expiresIn = json.getLong("expiresIn"),
            )
        }
        return TokenRefreshResult.Error("HTTP ${response.code}", response.code == 401 || response.code == 403)
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}
