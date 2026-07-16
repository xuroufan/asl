package com.hackfuture.feature.position.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hackfuture.core.model.Position
import com.hackfuture.core.model.PositionSide
import com.hackfuture.core.util.NumberUtil
import com.hackfuture.feature.position.CapitalSummary
import com.hackfuture.feature.position.PositionEffect
import com.hackfuture.feature.position.PositionIntent
import com.hackfuture.feature.position.PositionViewModel
import kotlinx.coroutines.launch

private val PnlUpColor = Color(0xFFFF6B6B)
private val PnlDownColor = Color(0xFF00C853)
private val LongColor = Color(0xFFFF6B6B)
private val ShortColor = Color(0xFF00C853)

@Composable
fun PositionScreen(
    viewModel: PositionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effect.collect {
            when (it) {
                is PositionEffect.ShowError -> scope.launch {
                    snackbarHostState.showSnackbar(it.message, duration = SnackbarDuration.Short)
                }
                is PositionEffect.PositionClosed -> scope.launch {
                    snackbarHostState.showSnackbar("持仓已平仓", duration = SnackbarDuration.Short)
                }
                is PositionEffect.NavigateToTrading -> {}
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 资金概览
        item {
            Spacer(Modifier.height(8.dp))
            Text("持仓", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            state.summary?.let { CapitalOverviewCard(summary = it) }
        }

        // 持仓列表
        if (state.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.positions.isEmpty()) {
            item {
                Text("暂无持仓", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center)
            }
        } else {
            items(state.positions, key = { it.id }) { position ->
                PositionCard(
                    position = position,
                    onClose = { viewModel.onIntent(PositionIntent.ClosePosition(position.id)) },
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun CapitalOverviewCard(summary: CapitalSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("总权益", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(NumberUtil.formatPrice(summary.totalEquity),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("未实现盈亏", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${if (summary.unrealizedPnl >= 0) "+" else ""}${NumberUtil.formatPrice(summary.unrealizedPnl)}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = if (summary.unrealizedPnl >= 0) PnlUpColor else PnlDownColor,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("可用", NumberUtil.formatPrice(summary.available))
                StatItem("保证金", NumberUtil.formatPrice(summary.marginUsed))
                StatItem("风险度", "${"%.1f".format(summary.riskRatio)}%",
                    if (summary.riskRatio > 80) MaterialTheme.colorScheme.error else
                        if (summary.riskRatio > 60) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface)
                StatItem("持仓数", "${summary.positionCount}")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun PositionCard(
    position: Position,
    onClose: () -> Unit,
) {
    val sideColor = if (position.side == PositionSide.LONG) LongColor else ShortColor
    val pnlColor = if (position.unrealizedPnl >= 0) PnlUpColor else PnlDownColor

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 合约名 + 方向
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(position.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        if (position.side == PositionSide.LONG) "做多" else "做空",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = sideColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text("${position.leverage}x", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            // 关键数据
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("手数", NumberUtil.formatQuantity(position.quantity))
                InfoColumn("开仓均价", NumberUtil.formatPrice(position.entryPrice))
                InfoColumn("最新价", NumberUtil.formatPrice(position.currentPrice))
            }
            Spacer(Modifier.height(8.dp))
            // 浮动盈亏
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("浮动盈亏",
                    "${if (position.unrealizedPnl >= 0) "+" else ""}${NumberUtil.formatPrice(position.unrealizedPnl)}",
                    pnlColor)
                InfoColumn("收益率",
                    NumberUtil.formatPercent(position.unrealizedPnlPercent), pnlColor)
            }
            Spacer(Modifier.height(12.dp))
            // 平仓按钮
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PnlDownColor),
                shape = MaterialTheme.shapes.small,
            ) {
                Text("平仓", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
