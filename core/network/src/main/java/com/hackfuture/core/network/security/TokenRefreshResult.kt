package com.hackfuture.core.network.security

sealed class TokenRefreshResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long,
    ) : TokenRefreshResult()

    data class Error(
        val message: String,
        val isTerminal: Boolean = false,
    ) : TokenRefreshResult()
}
