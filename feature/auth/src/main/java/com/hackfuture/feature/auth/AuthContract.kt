package com.hackfuture.feature.auth

import com.hackfuture.core.model.User

/**
 * Auth 模块 MVI 契约
 *
 * 单向数据流：Intent -> ViewModel -> State
 * UI 层只读 State，只发 Intent
 */

// ——————— State ———————

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val loginError: String? = null,
    val registerError: String? = null,
    val isRegisterMode: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isLoading && loginError == null
}

// ——————— Intent ———————

sealed class AuthIntent {
    data class Login(val username: String, val password: String) : AuthIntent()
    data class Register(
        val username: String,
        val email: String,
        val password: String,
    ) : AuthIntent()

    data object Logout : AuthIntent()
    data object ToggleMode : AuthIntent()
    data object ClearError : AuthIntent()
}

// ——————— Side Effect ———————

sealed class AuthEffect {
    data class NavigateToHome(val user: User) : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
    data object NavigateToLogin : AuthEffect()
}
