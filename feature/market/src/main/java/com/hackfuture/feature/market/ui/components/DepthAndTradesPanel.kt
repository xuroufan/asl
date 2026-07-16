package com.hackfuture.feature.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackfuture.core.model.OrderBookLevel
import com.hackfuture.core.util.NumberUtil
import com.hackfuture.feature.market.MarketTrade

// 本地颜色常量
private val BuyColor = Color(0xFFFF6B6B)
private val SellColor = Color(0xFF00C853)
private val LocalSm = 4.dp
private val LocalMd = 8.dp
private val LocalLg = 12.dp

/**
 * 盘口深度 + 成交明细组合面板。
 */
@Composable
fun DepthAndTradesPanel(
    bids: List<OrderBookLevel>,
    asks: List<OrderBookLevel>,
    maxBidVol: Double,
    maxAskVol: Double,
    currentPrice: Double,
    priceDirection: Int,
    trades: List<MarketTrade>,
    onPriceClick: (Double) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val priceColor = when {
        priceDirection > 0 -> BuyColor
        priceDirection < 0 -> SellColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val spread = if (asks.isNotEmpty() && bids.isNotEmpty())
        asks.first().price - bids.first().price else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(LocalMd)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(0.5f).padding(LocalMd),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("买盘", fontSize = 10.sp, color = BuyColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("价格", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                Text("卖盘", fontSize = 10.sp, color = SellColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(LocalSm))

            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                    bids.take(5).forEach { level ->
                        DepthRow(level, BuyColor, maxBidVol, isLeft = true, onClick = { onPriceClick(level.price) })
                    }
                }
                Column(
                    Modifier.width(64.dp).fillMaxHeight().padding(horizontal = LocalSm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(NumberUtil.formatPrice(currentPrice), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = priceColor, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    if (spread > 0 && asks.isNotEmpty() && bids.isNotEmpty()) {
                        Text(NumberUtil.formatPrice(spread), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                    asks.take(5).reversed().forEach { level ->
                        DepthRow(level, SellColor, maxAskVol, isLeft = false, onClick = { onPriceClick(level.price) })
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(
            modifier = Modifier.fillMaxWidth().weight(0.5f).padding(horizontal = LocalMd),
        ) {
            Text("成交明细", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = LocalSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("价格", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("数量", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("时间", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(trades.take(30), key = { "${it.timestamp}-${it.price}" }) { trade ->
                    val tradeColor = if (trade.isBuyerMaker) SellColor else BuyColor
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(NumberUtil.formatPrice(trade.price), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = tradeColor, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(formatQty(trade.quantity), fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(formatTradeTime(trade.timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun DepthRow(level: OrderBookLevel, color: Color, maxVol: Double, isLeft: Boolean, onClick: () -> Unit) {
    val depthFraction = (level.quantity / maxVol.coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxWidth().height(18.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        if (depthFraction > 0.01f) {
            Box(Modifier.width((60 * depthFraction).dp).height(14.dp).align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd).background(color.copy(alpha = 0.12f), RoundedCornerShape(2.dp)))
        }
        Row(Modifier.fillMaxWidth().matchParentSize().padding(horizontal = 2.dp), horizontalArrangement = if (isLeft) Arrangement.Start else Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(NumberUtil.formatPrice(level.price), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = color, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(2.dp))
            Text(formatQty(level.quantity), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatQty(v: Double): String = if (v >= 1000) "%.1fK".format(v / 1000) else "%.1f".format(v)

private fun formatTradeTime(timestamp: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return "%02d:%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), cal.get(java.util.Calendar.SECOND))
}
