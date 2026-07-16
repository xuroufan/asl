package com.hackfuture.feature.market.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackfuture.core.util.NumberUtil

private val UpColor = Color(0xFFFF6B6B)
private val DownColor = Color(0xFF00C853)

@Composable
fun PriceCard(
    symbol: String,
    price: Double,
    change: Double,
    changePercent: Double,
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(NumberUtil.formatPrice(price), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = priceColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${if (change >= 0) "+" else ""}${NumberUtil.formatPrice(change)}",
                        style = MaterialTheme.typography.titleMedium, color = priceColor, fontWeight = FontWeight.SemiBold)
                    Text(NumberUtil.formatPercent(changePercent), style = MaterialTheme.typography.titleMedium,
                        color = priceColor, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("24h高", NumberUtil.formatPrice(high))
                StatItem("24h低", NumberUtil.formatPrice(low))
                StatItem("24h量", formatVolume(volume))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatVolume(volume: Long): String = when {
    volume >= 1_0000_0000 -> "${"%.2f".format(volume / 1_0000_0000.0)}B"
    volume >= 1_0000 -> "${"%.2f".format(volume / 1_0000.0)}M"
    volume >= 1000 -> "${"%.2f".format(volume / 1000.0)}K"
    else -> volume.toString()
}
