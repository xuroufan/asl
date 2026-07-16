package com.hackfuture.trading.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 深色主题（默认） ====================

val DarkPrimary = Color(0xFF4F8CF7)        // 品牌蓝
val DarkSecondary = Color(0xFF2A3A5A)       // 辅色（卡片背景、次级元素）
val DarkTertiary = Color(0xFF1A2744)        // 深蓝黑（主背景）
val DarkBackground = Color(0xFF0B0F1A)      // 最底层背景
val DarkSurface = Color(0xFF121A2E)         // 卡片/面板背景
val DarkOnPrimary = Color.White
val DarkOnBackground = Color(0xFFE8EDF5)
val DarkOnSurface = Color(0xFFD0D9E8)
val DarkError = Color(0xFFFF6B6B)           // 卖/跌/亏损（红色）
val DarkOnError = Color.White
val DarkErrorContainer = Color(0xFF4A1010)
val DarkOnErrorContainer = Color(0xFFFFD6D6)
val DarkOutline = Color(0xFF2A3A5A)
val DarkOutlineVariant = Color(0xFF1E2A44)
val DarkSurfaceVariant = Color(0xFF162040)
val DarkOnSurfaceVariant = Color(0xFF8A9BB5)
val DarkInverseSurface = Color(0xFFE8EDF5)
val DarkInverseOnSurface = Color(0xFF0B0F1A)
val DarkInversePrimary = Color(0xFF4F8CF7)
val DarkPrimaryContainer = Color(0xFF1A3570)
val DarkOnPrimaryContainer = Color(0xFFB0C9FF)
val DarkSecondaryContainer = Color(0xFF1E2A44)
val DarkOnSecondaryContainer = Color(0xFFD0D9E8)
val DarkTertiaryContainer = Color(0xFF0F1A30)
val DarkOnTertiaryContainer = Color(0xFFB0C0E0)
val DarkScrim = Color(0x99000000)

// ==================== 浅色主题 ====================

val LightPrimary = Color(0xFF4F8CF7)
val LightSecondary = Color(0xFFE8EDF5)
val LightTertiary = Color(0xFFF0F4FF)
val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color.White
val LightOnPrimary = Color.White
val LightOnBackground = Color(0xFF1A1F2E)
val LightOnSurface = Color(0xFF1A1F2E)
val LightError = Color(0xFFFF6B6B)
val LightOnError = Color.White
val LightErrorContainer = Color(0xFFFDE8E8)
val LightOnErrorContainer = Color(0xFF4A1010)
val LightOutline = Color(0xFFD0D9E8)
val LightOutlineVariant = Color(0xFFE8EDF5)
val LightSurfaceVariant = Color(0xFFF0F4FF)
val LightOnSurfaceVariant = Color(0xFF5A6B8A)
val LightInverseSurface = Color(0xFF0B0F1A)
val LightInverseOnSurface = Color(0xFFE8EDF5)
val LightInversePrimary = Color(0xFF4F8CF7)
val LightPrimaryContainer = Color(0xFFD6E4FF)
val LightOnPrimaryContainer = Color(0xFF1A3570)
val LightSecondaryContainer = Color(0xFFE8EDF5)
val LightOnSecondaryContainer = Color(0xFF1A1F2E)
val LightOnTertiaryContainer = Color(0xFF1A1F2E)
val LightScrim = Color(0x66000000)

// ==================== 自定义交易颜色 ====================

/** 买入/上涨颜色（港股红色惯例） */
object TradingColors {
    /** 买入/上涨（红色） */
    val buy = Color(0xFFFF6B6B)
    /** 卖出/下跌（绿色） */
    val sell = Color(0xFF00C853)
    /** 涨跌持平 */
    val flat = Color(0xFF9E9E9E)
    /** 预警 */
    val warning = Color(0xFFFFB74D)
    /** 成功 */
    val success = Color(0xFF4CAF50)
    /** 信息 */
    val info = Color(0xFF4F8CF7)
    /** K线网格线 */
    val chartGrid = Color(0xFF2A3A5A)
    /** K线文字 */
    val chartText = Color(0xFF8A9BB5)
    /** 买入按钮背景 */
    val buyButton = Color(0xFFFF6B6B)
    /** 卖出按钮背景 */
    val sellButton = Color(0xFF00C853)
    /** 涨跌幅背景（红色浅） */
    val buyBg = Color(0x33FF6B6B)
    /** 涨跌幅背景（绿色浅） */
    val sellBg = Color(0x3300C853)

    // 浅色主题的交易色（更柔和的版本）
    val buyLight = Color(0xFFFF5252)
    val sellLight = Color(0xFF00E676)
    val warningLight = Color(0xFFFFA726)
    val chartGridLight = Color(0xFFE0E6F0)
    val chartTextLight = Color(0xFF8A9BB5)
}
