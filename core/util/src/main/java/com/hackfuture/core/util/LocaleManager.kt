package com.hackfuture.core.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * 语言管理器 — 负责持久化存储用户语言偏好、动态切换和恢复。
 *
 * 语言代码采用 Android 标准的 BCP 47 格式：
 * - "zh" → 简体中文
 * - "zh-rTW" → 繁体中文（台湾）
 * - "en" → 英文
 */
object LocaleManager {

    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val LANGUAGE_DEFAULT = ""

    // ==================== 核心 API ====================

    /**
     * 设置并持久化用户选择的语言，返回更新后的 Context。
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val targetLang = if (languageCode.isBlank()) getSystemLanguage() else languageCode
        saveLanguage(context, languageCode)

        val locale = languageCodeToLocale(targetLang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * 获取用户保存的语言代码；若从未设置则返回空字符串。
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_DEFAULT) ?: LANGUAGE_DEFAULT
    }

    /**
     * 获取设备当前系统语言代码。
     */
    fun getSystemLanguage(): String {
        return localeToLanguageCode(Locale.getDefault())
    }

    /**
     * 判断用户是否手动设置过语言。
     */
    fun isLanguageSet(context: Context): Boolean {
        return getCurrentLanguage(context).isNotBlank()
    }

    /**
     * 清除用户语言偏好，恢复跟随系统。
     */
    fun clearLanguage(context: Context) {
        saveLanguage(context, LANGUAGE_DEFAULT)
    }

    // ==================== 内部方法 ====================

    private fun saveLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    /** 将语言代码转为 Locale 对象 */
    private fun languageCodeToLocale(code: String): Locale {
        return when {
            code == "zh-rTW" -> Locale.TRADITIONAL_CHINESE
            code.startsWith("zh") -> Locale.SIMPLIFIED_CHINESE
            code.startsWith("en") -> Locale.ENGLISH
            code.contains("-r") -> {
                val parts = code.split("-r")
                Locale(parts[0], parts[1])
            }
            else -> Locale(code)
        }
    }

    /** 将 Locale 转为语言代码 */
    private fun localeToLanguageCode(locale: Locale): String {
        return when {
            locale.language == "zh" && locale.country == "TW" -> "zh-rTW"
            locale.language == "zh" -> "zh"
            locale.language == "en" -> "en"
            else -> locale.language
        }
    }
}
