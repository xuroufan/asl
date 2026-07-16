package com.hackfuture.core.model

import kotlinx.serialization.Serializable

/**
 * 用户账户模型
 */
@Serializable
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val kycLevel: KycLevel = KycLevel.NONE,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isVerified: Boolean
        get() = kycLevel >= KycLevel.BASIC

    val isActive: Boolean
        get() = accountStatus == AccountStatus.ACTIVE
}

@Serializable
enum class AccountStatus {
    ACTIVE,
    SUSPENDED,
    FROZEN,
    CLOSED,
}

@Serializable
enum class KycLevel {
    NONE,
    BASIC,
    ADVANCED,
}

/**
 * 登录请求
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

/**
 * 登录响应
 */
@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: User,
)

/**
 * 注册请求
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null,
)

/**
 * Token 刷新请求
 */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

/**
 * Token 刷新响应
 */
@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
