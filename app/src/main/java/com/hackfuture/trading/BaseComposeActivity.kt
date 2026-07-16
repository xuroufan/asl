package com.hackfuture.trading

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.hackfuture.core.util.LocaleManager

/**
 * 基础 Compose Activity — 处理语言切换与配置变更。
 *
 * 所有 Activity 应继承此类而非直接继承 ComponentActivity，以确保：
 * 1. attachBaseContext 中应用用户保存的语言
 * 2. onConfigurationChanged 中维持语言设置
 * 3. 提供 recreateWithLanguage 方法实现无感切换
 */
open class BaseComposeActivity : ComponentActivity() {

    /** 当前语言代码，用于 Compose 重组触发 */
    protected val currentLanguage = mutableStateOf(LocaleManager.getSystemLanguage())

    override fun attachBaseContext(newBase: Context) {
        val savedLang = LocaleManager.getCurrentLanguage(newBase)
        val context = LocaleManager.setLocale(newBase, savedLang)
        currentLanguage.value = if (savedLang.isNotBlank()) savedLang else LocaleManager.getSystemLanguage()
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化语言状态
        val saved = LocaleManager.getCurrentLanguage(this)
        currentLanguage.value = if (saved.isNotBlank()) saved else LocaleManager.getSystemLanguage()
    }

    /**
     * 切换语言并重建 Activity。
     *
     * @param languageCode 目标语言代码（"zh", "zh-rTW", "en"）
     */
    protected fun switchLanguage(languageCode: String) {
        val saved = LocaleManager.getCurrentLanguage(this)
        if (languageCode == saved) return // 相同语言不重复设置

        // 持久化并更新
        LocaleManager.setLocale(this, languageCode)
        currentLanguage.value = languageCode

        // 重建 Activity 以应用新的 Configuration
        recreate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 如果用户手动设置了语言，确保系统配置变更不覆盖
        if (LocaleManager.isLanguageSet(this)) {
            val saved = LocaleManager.getCurrentLanguage(this)
            val code = if (saved.isNotBlank()) saved else LocaleManager.getSystemLanguage()
            if (currentLanguage.value != code) {
                currentLanguage.value = code
            }
        } else {
            // 跟随系统语言
            currentLanguage.value = LocaleManager.getSystemLanguage()
        }
    }
}
