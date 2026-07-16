package com.hackfuture.trading.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 基础字体体系（用于系统组件）
 */
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 45.sp, lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)

/**
 * 交易终端专用字体体系
 *
 * 使用系统默认字体（Roboto / San Francisco），
 * 严格遵循设计规范中的字号/字重/行高定义。
 */
object TradingTypography {
    /** 28sp Bold 1.2 — 大标题（行情页合约名） */
    val LargeTitle = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 33.6.sp,
    )
    /** 22sp SemiBold 1.3 — 页面主标题 */
    val Title1 = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.6.sp,
    )
    /** 18sp SemiBold 1.3 — 区块标题 */
    val Title2 = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 23.4.sp,
    )
    /** 16sp Bold 1.4 — 列表项标题 */
    val Headline = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.4.sp,
    )
    /** 14sp Regular 1.5 — 正文（价格、手数等） */
    val Body = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp,
    )
    /** 13sp Medium 1.4 — 辅助信息（涨跌幅） */
    val Callout = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.2.sp,
    )
    /** 12sp Regular 1.4 — 脚注（时间、标签） */
    val Footnote = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.8.sp,
    )
    /** 10sp Regular 1.3 — 极小文字（盘口价格点） */
    val Caption = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 10.sp, lineHeight = 13.sp,
    )
    /** 32sp Bold 1.0 — 最新价（特大号） */
    val PriceLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp,
    )
    /** 20sp SemiBold 1.3 — 中等价格（持仓页面中的价格） */
    val PriceMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    )
    /** 13sp Medium 1.4 — 辅助信息（涨跌幅），与 Callout 一致 */
    val PriceSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.2.sp, letterSpacing = 0.2.sp,
    )
    /** 15sp Medium — 盘口价格等宽 */
    val PriceMonospace = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 22.sp,
    )
    /** 14sp SemiBold — 价格变化 */
    val PriceChange = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    )
    /** 16sp SemiBold — 按钮文字 */
    val ButtonText = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    )
}
