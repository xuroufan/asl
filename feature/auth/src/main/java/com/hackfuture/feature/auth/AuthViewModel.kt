package com.hackfuture.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackfuture.core.network.ApiService
import com.hackfuture.core.model.LoginRequest
import com.hackfuture.core.model.RegisterRequest
import com.hackfuture.core.model.LoginResponse
import com.hackfuture.core.model.User
import com.hackfuture.core.model.ApiResult
import com.hackfuture.core.model.parseData
import com.hackfuture.core.network.AuthInterceptor
import com.hackfuture.core.network.security.TokenManager
import com.hackfuture.core.network.websocket.WebSocketManager
import kotlinx.serialization.json.Json
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val json: Json,
    private val tokenManager: TokenManager,
    private val authInterceptor: AuthInterceptor,
    private val webSocketManager: WebSocketManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.Login -> handleLogin(intent.username, intent.password)
            is AuthIntent.Register -> handleRegister(intent.username, intent.email, intent.password)
            is AuthIntent.Logout -> handleLogout()
            is AuthIntent.ToggleMode -> toggleMode()
            is AuthIntent.ClearError -> clearError()
        }
    }

    private fun handleLogin(username: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loginError = null) }

            try {
                val result = apiService.login(LoginRequest(username, password))
                if (result.code == 200) {
                    val loginResponse = result.parseData<LoginResponse>(json)
                    if (loginResponse != null) {
                        // 保存 token
                        tokenManager.saveTokens(
                            accessToken = loginResponse.accessToken,
                            refreshToken = loginResponse.refreshToken,
                            expiresIn = 86400L
                        )
                        authInterceptor.updateToken(loginResponse.accessToken)

                        // 连接 WebSocket
                        webSocketManager.connect()

                        _state.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = loginResponse.user ?: User(),
                                loginError = null,
                            )
                        }
                        _effect.send(AuthEffect.NavigateToHome(loginResponse.user ?: User()))
                    } else {
                        throw Exception("解析登录响应失败")
                    }
                } else {
                    throw Exception(result.msg.ifEmpty { "登录失败" })
                }
            } catch (e: Exception) {
                Timber.e(e, "Login failed")
                val errorMsg = e.message ?: "登录失败，请重试"
                _state.update { it.copy(isLoading = false, loginError = errorMsg) }
                _effect.send(AuthEffect.ShowError(errorMsg))
            }
        }
    }

    private fun handleRegister(username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, registerError = null) }

            try {
                val result = apiService.register(RegisterRequest(username, email, password))
                if (result.code == 200) {
                    val loginResponse = result.parseData<LoginResponse>(json)
                    if (loginResponse != null) {
                        tokenManager.saveTokens(
                            accessToken = loginResponse.accessToken,
                            refreshToken = loginResponse.refreshToken,
                            expiresIn = 86400L
                        )
                        authInterceptor.updateToken(loginResponse.accessToken)
                        webSocketManager.connect()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = loginResponse.user ?: User(),
                                registerError = null,
                            )
                        }
                        _effect.send(AuthEffect.NavigateToHome(loginResponse.user ?: User()))
                    } else {
                        throw Exception("解析注册响应失败")
                    }
                } else {
                    throw Exception(result.msg.ifEmpty { "注册失败" })
                }
            } catch (e: Exception) {
                Timber.e(e, "Register failed")
                val errorMsg = e.message ?: "注册失败，请重试"
                _state.update { it.copy(isLoading = false, registerError = errorMsg) }
                _effect.send(AuthEffect.ShowError(errorMsg))
            }
        }
    }

    private fun handleLogout() {
        _state.update { AuthState() }
        _effect.trySend(AuthEffect.NavigateToLogin)
    }

    private fun toggleMode() {
        _state.update { it.copy(isRegisterMode = !it.isRegisterMode, loginError = null, registerError = null) }
    }

    private fun clearError() {
        _state.update { it.copy(loginError = null, registerError = null) }
    }
}
