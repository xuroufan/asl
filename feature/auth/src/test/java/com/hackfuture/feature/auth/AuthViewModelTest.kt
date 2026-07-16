package com.hackfuture.feature.auth

import com.hackfuture.core.model.LoginResponse
import com.hackfuture.core.model.User
import com.hackfuture.core.network.ApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: ApiService = mockk()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success updates state to logged in`() = runTest {
        val user = User(
            id = "1", username = "test", displayName = "Test",
            email = "test@test.com", createdAt = 0L, updatedAt = 0L,
        )
        coEvery { apiService.login(any()) } returns LoginResponse(
            accessToken = "token", refreshToken = "refresh",
            expiresIn = 3600L, user = user,
        )

        viewModel.onIntent(AuthIntent.Login("test", "pass"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        kotlin.test.assertTrue(state.isLoggedIn, "Should be logged in after successful login")
        kotlin.test.assertEquals("test", state.user?.username)
        coVerify { apiService.login(any()) }
    }

    @Test
    fun `login failure sets error state`() = runTest {
        coEvery { apiService.login(any()) } throws RuntimeException("Invalid credentials")

        viewModel.onIntent(AuthIntent.Login("test", "wrong"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        kotlin.test.assertNotNull(state.loginError, "Error should be set on failure")
        kotlin.test.assertFalse(state.isLoggedIn, "Should not be logged in")
    }

    @Test
    fun `logout clears user state`() {
        viewModel.onIntent(AuthIntent.Logout)

        val state = viewModel.state.value
        kotlin.test.assertNull(state.user, "User should be null after logout")
        kotlin.test.assertFalse(state.isLoggedIn)
    }

    @Test
    fun `toggle mode switches between login and register`() {
        kotlin.test.assertFalse(viewModel.state.value.isRegisterMode, "Default should be login")

        viewModel.onIntent(AuthIntent.ToggleMode)
        kotlin.test.assertTrue(viewModel.state.value.isRegisterMode, "Should switch to register")

        viewModel.onIntent(AuthIntent.ToggleMode)
        kotlin.test.assertFalse(viewModel.state.value.isRegisterMode, "Should switch back to login")
    }

    @Test
    fun `clear error resets error messages`() {
        coEvery { apiService.login(any()) } throws RuntimeException("Error")
        viewModel.onIntent(AuthIntent.Login("u", "p"))

        viewModel.onIntent(AuthIntent.ClearError)
        kotlin.test.assertNull(viewModel.state.value.loginError)
    }
}
