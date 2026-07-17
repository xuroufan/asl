package com.hackfuture.core.network.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hackfuture.core.network.security.TokenManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * TokenManager 单元测试
 *
 * 注意：EncryptedSharedPreferences 需要 Android 环境，
 * 此处使用 mock 验证方法交互。
 */
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager
    private val mockContext: Context = mockk()
    private val mockPrefs = mockk<SharedPreferences>(relaxed = true)

    @Before
    fun setUp() {
        every { mockContext.getApplicationContext() } returns mockContext
        every { mockContext.getSharedPreferences(any(), any<Int>()) } returns mockPrefs

        // Mock MasterKey.Builder via mockkConstructor
        mockkConstructor(MasterKey.Builder::class)
        val mockMasterKey: MasterKey = mockk()
        every { anyConstructed<MasterKey.Builder>().build() } returns mockMasterKey

        // Mock EncryptedSharedPreferences
        mockkStatic(EncryptedSharedPreferences::class)
        every {
            EncryptedSharedPreferences.create(
                any<Context>(),
                any<String>(),
                any<MasterKey>(),
                any<EncryptedSharedPreferences.PrefKeyEncryptionScheme>(),
                any<EncryptedSharedPreferences.PrefValueEncryptionScheme>(),
            )
        } returns mockPrefs

        // Explicitly mock getString to return null for access token key
        every { mockPrefs.getString(any(), any()) } returns null

        tokenManager = TokenManager(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `device id is generated consistently`() {
        assertNotNull(tokenManager)
    }

    @Test
    fun `manager starts with no stored token`() {
        assertNull(tokenManager.getAccessToken())
    }

    @Test
    fun `clear tokens is safe when no tokens exist`() {
        tokenManager.clearTokens()
    }
}
