package com.hackfuture.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * LocaleManager 单元测试 — 验证语言代码转换、系统语言获取逻辑。
 *
 * 注意：SharedPreferences 的操作需要在 Android 环境下测试（Instrumented test），
 * 此处仅测试纯逻辑部分。
 */
class LocaleManagerTest {

    @Test
    fun `getSystemLanguage returns zh for Chinese locale`() {
        // 模拟系统为中文环境
        val original = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            val lang = LocaleManager.getSystemLanguage()
            assertEquals("zh", lang)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `getSystemLanguage returns en for English locale`() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            val lang = LocaleManager.getSystemLanguage()
            assertEquals("en", lang)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `getSystemLanguage returns zh-rTW for Traditional Chinese locale`() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.TRADITIONAL_CHINESE)
        try {
            val lang = LocaleManager.getSystemLanguage()
            assertEquals("zh-rTW", lang)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `locale to language code conversion is consistent`() {
        val testCases = mapOf(
            Locale.SIMPLIFIED_CHINESE to "zh",
            Locale.ENGLISH to "en",
            Locale.TRADITIONAL_CHINESE to "zh-rTW",
        )
        // 由于 languageCodeToLocale 是 private 方法，此处通过 getSystemLanguage 间接验证
        for ((locale, expectedCode) in testCases) {
            val original = Locale.getDefault()
            Locale.setDefault(locale)
            try {
                val code = LocaleManager.getSystemLanguage()
                assertEquals("Locale $locale should map to code $expectedCode", expectedCode, code)
            } finally {
                Locale.setDefault(original)
            }
        }
    }
}
