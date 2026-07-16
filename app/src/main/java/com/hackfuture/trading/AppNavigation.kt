package com.hackfuture.trading

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hackfuture.feature.auth.navigation.AuthNavGraph
import com.hackfuture.trading.ui.MainScreen

/**
 * 应用级导航入口。
 *
 * 页面结构：
 * - auth: 登录/注册子导航图（未登录时显示）
 * - main: 主界面（包含底部导航 5 个 Tab）
 *
 * 登录后从 auth → main，退出后从 main → auth。
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = "auth",
    ) {
        // ——— 登录/注册 ———
        composable("auth") {
            AuthNavGraph(
                navController = rememberNavController(),
                onNavigateToHome = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
            )
        }

        // ——— 主界面（行情看板 / 交易 / 持仓 / 我的） ———
        composable("main") {
            MainScreen(
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                },
            )
        }
    }
}
