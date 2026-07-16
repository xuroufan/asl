package com.hackfuture.core.network.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hackfuture.core.model.RefreshTokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRY, currentTimeMillis() + expiresIn * 1000L)
            .apply()
        Timber.d("Tokens saved, expires in ${expiresIn}s")
    }

    fun getAccessToken(): String? = securePrefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = securePrefs.getString(KEY_REFRESH_TOKEN, null)

    fun getTokenExpiry(): Long = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0L)

    val isTokenExpired: Boolean
        get() = currentTimeMillis() >= getTokenExpiry() - REFRESH_MARGIN_MS

    val hasValidToken: Boolean
        get() = getAccessToken() != null && !isTokenExpired

    val hasRefreshToken: Boolean
        get() = getRefreshToken() != null

    fun clearTokens() {
        securePrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
        Timber.d("Tokens cleared")
    }

    fun getDeviceId(): String {
        val existing = securePrefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val newId = java.util.UUID.randomUUID().toString()
        securePrefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    fun encryptWithKeystore(plainText: String): ByteArray {
        val cipher = javax.crypto.Cipher.getInstance(KEYSTORE_TRANSFORM)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, loadOrCreateKey())
        return cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    }

    fun decryptWithKeystore(cipherText: ByteArray): String {
        val cipher = javax.crypto.Cipher.getInstance(KEYSTORE_TRANSFORM)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, loadOrCreateKey())
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun loadOrCreateKey(): javax.crypto.SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return if (ks.containsAlias(KEYSTORE_ALIAS)) {
            (ks.getEntry(KEYSTORE_ALIAS, null) as java.security.KeyStore.SecretKeyEntry).secretKey
        } else {
            val kg = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore")
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            kg.init(spec)
            kg.generateKey()
        }
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()

    companion object {
        private const val PREFS_NAME = "hackfuture_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEYSTORE_ALIAS = "hackfuture_master_key"
        private const val KEYSTORE_TRANSFORM = "AES/GCM/NoPadding"
        private const val REFRESH_MARGIN_MS = 5 * 60 * 1000L
    }
}
