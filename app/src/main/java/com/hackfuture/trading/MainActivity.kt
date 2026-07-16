package com.hackfuture.trading

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hackfuture.trading.i18n.LanguageProvider
import com.hackfuture.trading.ui.theme.FuturesTheme
import com.hackfuture.trading.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主 Activity — 应用语言入口点与 Compose 根节点。
 *
 * 继承 [BaseComposeActivity] 以获得：
 * - attachBaseContext 中的语言初始化
 * - onConfigurationChanged 中的语言保持
 * - currentLanguage 状态用于 Compose 重组
 */
@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // 将 BaseComposeActivity 中的语言状态传递给 Compose 树
            val langCode = currentLanguage.value

            LanguageProvider(languageCode = langCode) {
                FuturesTheme(themeMode = ThemeManager.currentMode) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
