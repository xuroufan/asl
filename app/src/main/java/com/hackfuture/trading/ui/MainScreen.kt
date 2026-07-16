package com.hackfuture.trading.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hackfuture.feature.market.ui.MarketOverviewScreen
import com.hackfuture.feature.position.ui.PositionScreen
import com.hackfuture.feature.trading.ui.TradingScreen
import com.hackfuture.trading.ui.settings.LanguageSettingsScreen
import com.hackfuture.trading.ui.theme.Elevation
import com.hackfuture.trading.ui.theme.TradingTypography

/**
 * 底部导航项定义。
 * 使用 Material Icons Outlined（未选中）和 Filled（选中），符合设计规范。
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    MARKET("market_overview", "行情", Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    TRADING("trading", "交易", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz),
    POSITION("position", "持仓", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    MESSAGES("messages", "消息", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    PROFILE("settings", "我的", Icons.Filled.Person, Icons.Outlined.Person),
}

/**
 * 主界面 — 带底部导航栏的应用外壳。
 *
 * Scaffold + NavigationBar + NavHost 组合：
 * - 5 个底部 Tab 切换主页面
 * - 详情页（market_detail, language_settings）push 在 Tab 之上，隐藏导航栏
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 需要隐藏底部导航的详情页路由
    val bottomNavRoutes = BottomNavItem.entries.map { it.route }
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                TradingBottomBar(
                    navController = navController,
                    currentDestination = currentDestination?.route,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.MARKET.route,
            modifier = Modifier.padding(padding),
        ) {
            // ——— 行情看板 Tab ———
            composable(BottomNavItem.MARKET.route) {
                MarketOverviewScreen(
                    onNavigateToDetail = { symbol ->
                        navController.navigate("market_detail/$symbol")
                    },
                )
            }

            // ——— 交易 Tab ———
            composable(BottomNavItem.TRADING.route) {
                TradingScreen()
            }

            // ——— 持仓 Tab ———
            composable(BottomNavItem.POSITION.route) {
                PositionScreen()
            }

            // ——— 消息 Tab（占位） ———
            composable(BottomNavItem.MESSAGES.route) {
                MessagesPlaceholder()
            }

            // ——— 设置/我的 Tab ———
            composable(BottomNavItem.PROFILE.route) {
                SettingsScreen(
        onLogout = onLogout,
                    onNavigateToLanguage = {
                        navController.navigate("language_settings")
                    },
                )
            }

            // ——— 合约详情页（push，非 Tab） ———
            composable("market_detail/{symbol}") { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                com.hackfuture.feature.market.ui.MarketScreen(
                    onNavigateToTrading = { s ->
                        navController.navigate("trading?symbol=$s") {
                            popUpTo(BottomNavItem.TRADING.route) { inclusive = false }
                        }
                    },
                )
            }

            // ——— 语言设置页（push，非 Tab） ———
            composable("language_settings") {
                LanguageSettingsScreen(
                    onLanguageSelected = { /* Activity 层处理 recreate */ },
                )
            }
        }
    }
}

/**
 * 底部导航栏 — 5 个 Tab，选中图标填充 + 主色，未选中图标线框 + 灰色。
 */
@Composable
private fun TradingBottomBar(
    navController: NavHostController,
    currentDestination: String?,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevation.bottomNav,
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = currentDestination == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = TradingTypography.Footnote,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

/**
 * 消息页面占位。
 */
@Composable
private fun MessagesPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "消息",
            style = TradingTypography.Title2,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
