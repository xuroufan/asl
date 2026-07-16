package com.hackfuture.feature.market.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackfuture.core.model.OrderBookLevel
import com.hackfuture.core.util.NumberUtil

private val BidColor = Color(0xFFFF6B6B)
private val AskColor = Color(0xFF00C853)

@Composable
fun OrderBookView(
    bids: List<OrderBookLevel>,
    asks: List<OrderBookLevel>,
    maxBidVol: Double,
    maxAskVol: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text("盘口", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        // 表头
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("价格", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            Text("数量", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("累计", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        // 卖盘（从高到低）
        val reversedAsks = asks.reversed()
        reversedAsks.forEach { level ->
            DepthRow(level, AskColor, maxAskVol, isAsk = true)
        }
        if (asks.isNotEmpty()) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        }
        // 买盘（从高到低）
        bids.forEach { level ->
            DepthRow(level, BidColor, maxBidVol, isAsk = false)
        }
    }
}

@Composable
private fun DepthRow(
    level: OrderBookLevel,
    color: Color,
    maxVol: Double,
    isAsk: Boolean,
) {
    val depthF = (level.quantity / maxVol.coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f)
    Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
        // 深度条背景
        Box(
            modifier = Modifier.fillMaxWidth().height(20.dp)
                .padding(end = if (isAsk) 0.dp else 0.dp),
        ) {
            Box(
                modifier = Modifier.width((80 * depthF).dp).height(20.dp)
                    .align(if (isAsk) Alignment.CenterEnd else Alignment.CenterStart),
            ) {
                Box(
                    modifier = Modifier.matchParentSize().padding(vertical = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().matchParentSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(NumberUtil.formatPrice(level.price),
                style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text(formatQty(level.quantity),
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center)
            Text(formatQty(level.quantity), // simplified cumulative
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                textAlign = TextAlign.End)
        }
    }
}

private fun formatQty(v: Double) = if (v >= 1000) "%.1fK".format(v / 1000) else "%.2f".format(v)
