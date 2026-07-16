package com.hackfuture.feature.trading.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hackfuture.core.model.OrderSide
import com.hackfuture.core.model.OrderType
import com.hackfuture.core.util.NumberUtil
import com.hackfuture.feature.trading.TradingEffect
import com.hackfuture.feature.trading.TradingIntent
import com.hackfuture.feature.trading.TradingViewModel
import com.hackfuture.feature.trading.PendingOrder
import kotlinx.coroutines.launch

private val BuyColor = Color(0xFFFF6B6B)
private val SellColor = Color(0xFF00C853)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TradingScreen(
    symbol: String = "BTCUSDT",
    viewModel: TradingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.onIntent(TradingIntent.SetSymbol(symbol))
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect {
            when (it) {
                is TradingEffect.ShowError -> scope.launch {
                    snackbarHostState.showSnackbar(it.message, duration = SnackbarDuration.Short)
                }
                is TradingEffect.OrderPlaced -> scope.launch {
                    snackbarHostState.showSnackbar("订单已提交", duration = SnackbarDuration.Short)
                }
                is TradingEffect.NavigateBack -> {}
            }
        }
    }

    // 二次确认弹窗
    state.confirmOrder?.let { order ->
        OrderConfirmDialog(
            order = order,
            onConfirm = { viewModel.onIntent(TradingIntent.ConfirmOrder) },
            onDismiss = { viewModel.onIntent(TradingIntent.DismissConfirm) },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {

            Text("交易 - $symbol", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)

            // 实时价格
            Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
            ) {
            Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            ) {
            Column {
            Text("最新价", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(NumberUtil.formatPrice(state.currentPrice),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            state.marketData?.let { md ->
            val c = if (md.change >= 0) BuyColor else SellColor
            Column(horizontalAlignment = Alignment.End) {
            Text("${if (md.change >= 0) "+" else ""}${NumberUtil.formatPercent(md.changePercent)}",
            style = MaterialTheme.typography.titleMedium, color = c,
            fontWeight = FontWeight.SemiBold)
            }
            }
            }
            }

            // 方向 + 类型选择
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
            selected = state.orderSide == OrderSide.BUY,
            onClick = { viewModel.onIntent(TradingIntent.SetSide(OrderSide.BUY)) },
           colors = SegmentedButtonDefaults.colors(activeContainerColor = BuyColor),
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
           ) { Text("买入", fontWeight = FontWeight.Bold) }
            SegmentedButton(
            selected = state.orderSide == OrderSide.SELL,
            onClick = { viewModel.onIntent(TradingIntent.SetSide(OrderSide.SELL)) },
           colors = SegmentedButtonDefaults.colors(activeContainerColor = SellColor),
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
           ) { Text("卖出", fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(OrderType.LIMIT to "限价", OrderType.MARKET to "市价", OrderType.STOP to "止损").forEachIndexed { idx, (t, l) ->
            SegmentedButton(
            selected = state.orderType == t,
            onClick = { viewModel.onIntent(TradingIntent.SetOrderType(t)) },
            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
            ) { Text(l) }
            }
            }

            Spacer(Modifier.height(16.dp))

            // 价格输入
            if (state.orderType != OrderType.MARKET) {
            OutlinedTextField(
            value = state.price,
            onValueChange = { viewModel.onIntent(TradingIntent.SetPrice(it)) },
            label = { Text("价格 (USDT)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
            value = state.quantity,
            onValueChange = { viewModel.onIntent(TradingIntent.SetQuantity(it)) },
            label = { Text("数量") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            // 预估金额
            val estPrice = if (state.orderType == OrderType.MARKET) state.currentPrice
            else state.price.toDoubleOrNull() ?: 0.0
            val estQty = state.quantity.toDoubleOrNull() ?: 0.0
            if (estPrice > 0 && estQty > 0) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("预估成交额", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("≈ ${NumberUtil.formatPrice(estPrice * estQty)} USDT",
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            }

            state.error?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(err, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
            }
            state.successMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall,
            color = BuyColor)
            }

            Spacer(Modifier.height(24.dp))

            // 下单按钮（触发预览弹窗）
            Button(
            onClick = { viewModel.onIntent(TradingIntent.PreviewOrder) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.canSubmit && !state.isSubmitting,
            colors = ButtonDefaults.buttonColors(
            containerColor = if (state.orderSide == OrderSide.BUY) BuyColor else SellColor,
            ),
            ) {
            if (state.isSubmitting) {
            CircularProgressIndicator(strokeWidth = 2.dp,
            color = Color.White)
            } else {
            Text(
            text = if (state.orderSide == OrderSide.BUY) "买入" else "卖出",
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
            )
                }
            }

        }
    }
}

@Composable
private fun OrderConfirmDialog(
    order: PendingOrder,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sideColor = if (order.side == OrderSide.BUY) BuyColor else SellColor
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("确认下单", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("方向")
                    Text(if (order.side == OrderSide.BUY) "买入" else "卖出",
                        color = sideColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("合约"); Text(order.symbol)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("类型"); Text(order.type.name)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("价格"); Text("${NumberUtil.formatPrice(order.price)} USDT")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("数量"); Text(NumberUtil.formatQuantity(order.quantity))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("总金额", fontWeight = FontWeight.Bold)
                    Text("${NumberUtil.formatPrice(order.total)} USDT",
                        fontWeight = FontWeight.Bold, color = sideColor)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = sideColor),
            ) {
                Text("确认${if (order.side == OrderSide.BUY) "买入" else "卖出"}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
