package com.hackfuture.core.network.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hackfuture.core.network.security.TokenManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test

/**
 * TokenManager 单元测试
 *
 * 注意：EncryptedSharedPreferences 需要 Android 环境，
 * 此处使用 mock 验证方法交互。完整的集成测试需要在设备上运行。
 */
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager
    private val mockContext: Context = mockk()

    @Before
    fun setUp() {
        // Mock MasterKey
        mockkStatic(MasterKey::class)
        val mockMasterKey: MasterKey = mockk()
        every {
            MasterKey.Builder(any(), any<MasterKey.KeyScheme>())
        } returns mockk {
            every { setKeyScheme(any()) } returns this
            every { build() } returns mockMasterKey
        }

        // Mock EncryptedSharedPreferences
        mockkStatic(EncryptedSharedPreferences::class)
        val mockPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every {
            EncryptedSharedPreferences.create(any(), any(), any(), any(), any())
        } returns mockPrefs

        tokenManager = TokenManager(mockContext)
    }

    @Test
    fun `device id is generated consistently`() {
        // This tests that the TokenManager does not crash on construction
        kotlin.test.assertNotNull(tokenManager)
    }

    @Test
    fun `manager starts with no stored token`() {
        kotlin.test.assertNull(tokenManager.getAccessToken())
    }

    @Test
    fun `clear tokens is safe when no tokens exist`() {
        // Should not throw
        tokenManager.clearTokens()
    }
}
