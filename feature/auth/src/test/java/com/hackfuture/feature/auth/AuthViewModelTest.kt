package com.hackfuture.feature.auth

import com.hackfuture.core.model.LoginResponse
import com.hackfuture.core.model.User
import com.hackfuture.core.model.ApiResult
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: ApiService = mockk()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(apiService, json)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun userJson(user: User): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(user.id))
        put("username", JsonPrimitive(user.username))
        put("displayName", JsonPrimitive(user.displayName))
        put("email", JsonPrimitive(user.email))
        put("createdAt", JsonPrimitive(0))
        put("updatedAt", JsonPrimitive(0))
    }

    private fun loginResponseJson(user: User): JsonObject = buildJsonObject {
        put("accessToken", JsonPrimitive("token"))
        put("refreshToken", JsonPrimitive("refresh"))
        put("expiresIn", JsonPrimitive(3600))
        put("user", userJson(user))
    }

    private val testUser = User(
        id = "1", username = "test", displayName = "Test",
        email = "test@test.com", createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `login success updates state to logged in`() = runTest {
        coEvery { apiService.login(any()) } returns ApiResult(
            code = 200, msg = "ok", data = loginResponseJson(testUser),
        )

        viewModel.onIntent(AuthIntent.Login("test", "pass"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Should be logged in after successful login", state.isLoggedIn)
        assertEquals("test", state.user?.username)
        coVerify { apiService.login(any()) }
    }

    @Test
    fun `login failure sets error state`() = runTest {
        coEvery { apiService.login(any()) } throws RuntimeException("Invalid credentials")

        viewModel.onIntent(AuthIntent.Login("test", "wrong"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull("Error should be set on failure", state.loginError)
        assertFalse("Should not be logged in", state.isLoggedIn)
    }

    @Test
    fun `logout clears user state`() {
        viewModel.onIntent(AuthIntent.Logout)

        val state = viewModel.state.value
        assertNull("User should be null after logout", state.user)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun `toggle mode switches between login and register`() {
        assertFalse("Default should be login", viewModel.state.value.isRegisterMode)
        viewModel.onIntent(AuthIntent.ToggleMode)
        assertTrue("Should switch to register", viewModel.state.value.isRegisterMode)
        viewModel.onIntent(AuthIntent.ToggleMode)
        assertFalse("Should switch back to login", viewModel.state.value.isRegisterMode)
    }

    @Test
    fun `clear error resets error messages`() {
        coEvery { apiService.login(any()) } throws RuntimeException("Error")
        viewModel.onIntent(AuthIntent.Login("u", "p"))
        testDispatcher.scheduler.advanceTimeBy(1)
        viewModel.onIntent(AuthIntent.ClearError)
        assertNull(viewModel.state.value.loginError)
    }

    @Test
    fun `register with ApiResult success updates state`() = runTest {
        coEvery { apiService.register(any()) } returns ApiResult(
            code = 200, msg = "ok", data = userJson(testUser),
        )

        viewModel.onIntent(AuthIntent.Register("newuser", "new@test.com", "pass123"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Should be logged in after register", state.isLoggedIn)
        // The mock returns the testUser which has username="test"
        assertEquals("test", state.user?.username)
        coVerify { apiService.register(any()) }
    }

    @Test
    fun `register failure sets register error`() = runTest {
        coEvery { apiService.register(any()) } throws RuntimeException("Username taken")

        viewModel.onIntent(AuthIntent.Register("taken", "e@e.com", "pw"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull("Register error should be set", state.registerError)
        assertFalse("Should not be logged in", state.isLoggedIn)
    }

    @Test
    fun `toggle mode clears errors`() {
        coEvery { apiService.login(any()) } throws RuntimeException("err")
        viewModel.onIntent(AuthIntent.Login("u", "p"))
        testDispatcher.scheduler.advanceTimeBy(1)
        // State should have an error while loading
        viewModel.onIntent(AuthIntent.ToggleMode)
        assertNull(viewModel.state.value.loginError)
        assertNull(viewModel.state.value.registerError)
    }

    @Test
    fun `canSubmit starts true`() {
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `login sets isLoading and disables submit`() = runTest {
        coEvery { apiService.login(any()) } coAnswers {
            kotlinx.coroutines.delay(10000)
            throw RuntimeException()
        }
        viewModel.onIntent(AuthIntent.Login("u", "p"))
        testDispatcher.scheduler.advanceTimeBy(1)
        // Immediately after sending login, isLoading is true so canSubmit is false
        assertTrue("isLoading should be true", viewModel.state.value.isLoading)
        assertFalse("canSubmit should be false when loading", viewModel.state.value.canSubmit)
    }
}
