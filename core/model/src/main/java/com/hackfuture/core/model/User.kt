package com.hackfuture.core.model

import kotlinx.serialization.Serializable

// ============ 用户模型（对齐管理后台响应） ============

@Serializable
data class User(
    val userId: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val status: Int = 0,
    val roles: List<String> = emptyList(),
    val menus: List<RouterVO> = emptyList(),
    val loginIp: String? = null,
    val loginDate: String? = null,
) {
    val displayName: String get() = nickname.ifEmpty { username }
    val avatarUrl: String? get() = avatar
    val isActive: Boolean get() = status == 0
}

@Serializable
data class RouterVO(
    val id: Int = 0,
    val parentId: Int = 0,
    val name: String = "",
    val path: String = "",
    val component: String? = null,
    val meta: RouterMeta = RouterMeta(),
    val children: List<RouterVO> = emptyList(),
)

@Serializable
data class RouterMeta(
    val title: String = "",
    val icon: String = "",
    val hideMenu: Boolean = false,
)

// ============ 认证模型（与 ApiService 通信） ============

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
    val user: User? = null,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 86400L,
)
