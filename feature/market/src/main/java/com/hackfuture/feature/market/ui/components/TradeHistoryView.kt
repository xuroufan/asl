package com.hackfuture.feature.market.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.hackfuture.core.util.DateTimeUtil
import com.hackfuture.core.util.NumberUtil
import com.hackfuture.feature.market.MarketTrade

private val BuyColor = Color(0xFFFF6B6B)
private val SellColor = Color(0xFF00C853)

@Composable
fun TradeHistoryView(
    trades: List<MarketTrade>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text("成交", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("价格", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            Text("数量", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("时间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(trades.take(30), key = { "${it.timestamp}-${it.price}" }) { trade ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = if (trade.isBuyerMaker) SellColor else BuyColor
                    Text(NumberUtil.formatPrice(trade.price),
                        style = MaterialTheme.typography.bodySmall, color = color,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(formatQty(trade.quantity),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center)
                    Text(DateTimeUtil.formatTime(trade.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End)
                }
            }
        }
    }
}

private fun formatQty(v: Double) = if (v >= 1000) "%.1fK".format(v / 1000) else "%.4f".format(v)
