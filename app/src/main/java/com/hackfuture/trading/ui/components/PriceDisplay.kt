package com.hackfuture.trading.ui.components


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackfuture.core.util.NumberUtil
import com.hackfuture.trading.ui.theme.TradingColors

import com.hackfuture.trading.ui.theme.TradingTypography

/**
 * 价格显示组件 — 根据涨跌自动变色
 *
 * @param price 当前价格
 * @param change 涨跌额
 * @param changePercent 涨跌幅百分比
 * @param size 显示尺寸 large / small
 * @param showSymbol 是否显示币种符号
 */
@Composable
fun PriceDisplay(
    price: Double,
    change: Double? = null,
    changePercent: Double? = null,
    modifier: Modifier = Modifier,
    size: PriceSize = PriceSize.LARGE,
    showSymbol: Boolean = true,
) {
    val priceColor = when {
        change == null -> Color.Unspecified
        change > 0 -> TradingColors.buy
        change < 0 -> TradingColors.sell
        else -> TradingColors.flat
    }

    val priceStyle = when (size) {
        PriceSize.LARGE -> TradingTypography.PriceLarge
        PriceSize.MEDIUM -> TradingTypography.PriceMedium
        PriceSize.SMALL -> TradingTypography.PriceSmall
    }

    Column(modifier = modifier) {
        Text(
            text = "${if (showSymbol) "$ " else ""}${NumberUtil.formatPrice(price)}",
            style = priceStyle,
            color = priceColor,
            fontWeight = FontWeight.Bold,
        )

        if (change != null || changePercent != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (change != null) {
                        append("${if (change >= 0) "+" else ""}${NumberUtil.formatPrice(change)}")
                    }
                    if (change != null && changePercent != null) {
                        append("  ")
                    }
                    if (changePercent != null) {
                        append(NumberUtil.formatPercent(changePercent))
                    }
                },
                style = TradingTypography.PriceChange,
                color = priceColor,
            )
        }
    }
}

enum class PriceSize { LARGE, MEDIUM, SMALL }

/**
 * 紧凑型价格行 — 用于列表项
 */
@Composable
fun CompactPriceRow(
    label: String,
    price: Double,
    change: Double? = null,
    changePercent: Double? = null,
    modifier: Modifier = Modifier,
) {
    val priceColor = when {
        change == null -> MaterialTheme.colorScheme.onSurface
        change > 0 -> TradingColors.buy
        change < 0 -> TradingColors.sell
        else -> TradingColors.flat
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = NumberUtil.formatPrice(price),
            style = MaterialTheme.typography.bodyMedium,
            color = priceColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 涨跌幅标签 — 带背景色的小标签
 */
@Composable
fun ChangeBadge(
    changePercent: Double,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor) = when {
        changePercent > 0 -> TradingColors.buy.copy(alpha = 0.15f) to TradingColors.buy
        changePercent < 0 -> TradingColors.sell.copy(alpha = 0.15f) to TradingColors.sell
        else -> TradingColors.flat.copy(alpha = 0.15f) to TradingColors.flat
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
    ) {
        Text(
            text = NumberUtil.formatPercent(changePercent),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 多资产价格汇总组件
 */
@Composable
fun PortfolioSummaryDisplay(
    totalValue: Double,
    totalPnl: Double,
    totalPnlPercent: Double,
    modifier: Modifier = Modifier,
) {
    val pnlColor = when {
        totalPnl > 0 -> TradingColors.buy
        totalPnl < 0 -> TradingColors.sell
        else -> TradingColors.flat
    }
    Column(modifier = modifier) {
        Text(
            text = "总资产",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = NumberUtil.formatCompactChinese(totalValue),
            style = TradingTypography.PriceLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${if (totalPnl >= 0) "+" else ""}${NumberUtil.formatPrice(totalPnl)}",
                style = TradingTypography.PriceChange,
                color = pnlColor,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            ChangeBadge(changePercent = totalPnlPercent)
        }
    }
}
