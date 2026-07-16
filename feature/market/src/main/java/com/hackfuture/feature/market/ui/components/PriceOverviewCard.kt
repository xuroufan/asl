package com.hackfuture.feature.market.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackfuture.core.util.NumberUtil

// 本地常量
private val UpColor = Color(0xFFFF6B6B)
private val DownColor = Color(0xFF00C853)
private val LocalSm = 4.dp
private val LocalMd = 8.dp
private val LocalLg = 12.dp
private val LocalXl = 16.dp

/**
 * 价格概览卡片 — 展示合约名称、最新价、涨跌幅、24h 统计。
 */
@Composable
fun PriceOverviewCard(
    symbol: String,
    price: Double,
    change: Double,
    changePercent: Double,
    open: Double,
    high: Double,
    low: Double,
    volume: Long,
    modifier: Modifier = Modifier,
) {
    val priceColor = when {
        change > 0 -> UpColor
        change < 0 -> DownColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val animatedColor by animateColorAsState(targetValue = priceColor, animationSpec = tween(300), label = "priceColor")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(LocalXl)) {
            Text(symbol, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(LocalSm))
            Text(NumberUtil.formatPrice(price), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = animatedColor)
            Spacer(Modifier.height(LocalSm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val arrow = if (change > 0) "▲" else if (change < 0) "▼" else "―"
                Text("$arrow ${NumberUtil.formatPrice(change)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = animatedColor)
                Spacer(Modifier.width(LocalMd))
                Text(NumberUtil.formatPercent(changePercent), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = animatedColor)
            }
            Spacer(Modifier.height(LocalLg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PriceSubInfo("开盘", NumberUtil.formatPrice(open))
                PriceSubInfo("最高", NumberUtil.formatPrice(high))
                PriceSubInfo("最低", NumberUtil.formatPrice(low))
                PriceSubInfo("成交量", formatVolumeCompact(volume))
            }
        }
    }
}

@Composable
private fun PriceSubInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatVolumeCompact(volume: Long): String = when {
    volume >= 100_000_000 -> "${"%.1f".format(volume / 100_000_000.0)}亿"
    volume >= 10_000 -> "${"%.1f".format(volume / 10_000.0)}万"
    else -> "$volume"
}
