package com.hackfuture.trading.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

/**
 * Compose 语言 CompositionLocal — 在 Compose 树中传递当前语言代码。
 *
 * 在根 Composable 中通过 [LanguageProvider] 提供，
 * 子组件通过 LocalLanguage.current 获取当前语言。
 *
 * 当语言切换时更新此值，将触发整个 UI 树重组。
 */
val LocalLanguage = compositionLocalOf { "zh" }

/**
 * 当前语言对应的 Locale 对象。
 */
val LocalLanguageLocale: Locale
    @Composable get() = when (LocalLanguage.current) {
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "zh-rTW" -> Locale.TRADITIONAL_CHINESE
        "en" -> Locale.ENGLISH
        else -> Locale.getDefault()
    }

/**
 * 语言提供者 — 包裹 Compose 根节点，使所有子组件感知语言变化。
 */
@Composable
fun LanguageProvider(
    languageCode: String,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLanguage provides languageCode) {
        content()
    }
}
