package com.hackfuture.feature.market.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hackfuture.core.model.CandleInterval
import com.hackfuture.feature.market.MarketEffect
import com.hackfuture.feature.market.MarketIntent
import com.hackfuture.feature.market.MarketViewModel
import com.hackfuture.feature.market.ui.components.DepthAndTradesPanel
import com.hackfuture.feature.market.ui.components.KlineChart
import com.hackfuture.feature.market.ui.components.PriceOverviewCard

// 图表折叠后最小高度
private const val CHART_MIN_HEIGHT_DP = 80
// 图表展开时最大高度
private const val CHART_MAX_HEIGHT_DP = 320
// 图表折叠触发滚动偏移量（dp）
private const val COLLAPSE_THRESHOLD_DP = 60

/**
 * 行情页面（Market Screen）— 期货交易主页面。
 *
 * 布局（从上到下）：
 * 1. 顶部导航栏：Logo + "期货" | 搜索、设置
 * 2. 合约切换标签（水平滚动）
 * 3. 自选合约行（水平滑动，选中底部指示条）
 * 4. 价格概览卡片（合约名、最新价、涨跌幅、24h 统计）
 * 5. 图表周期切换按钮行
 * 6. K线图（约 45% 屏幕高度，向下滑动可折叠）
 * 7. 盘口深度 + 成交明细面板（约 30% 屏幕高度）
 *
 * 交互：
 * - 向下滑动：图表区域收缩，价格概览保留
 * - 点击盘口价格 → 触发 onPriceClick（可回调至下单页填充价格）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onNavigateToTrading: (String) -> Unit = {},
    viewModel: MarketViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // 图表折叠状态：根据滚动偏移量动态调整高度
    var chartCollapsedHeight by remember { mutableFloatStateOf(CHART_MAX_HEIGHT_DP.toFloat()) }
    val animatedChartHeight by animateDpAsState(
        targetValue = chartCollapsedHeight.dp,
        animationSpec = tween(300),
        label = "chartHeight",
    )

    // 监听滚动偏移，控制图表折叠
    LaunchedEffect(scrollState.value) {
        val offset = scrollState.value.toFloat()
        val newHeight = (CHART_MAX_HEIGHT_DP - offset / 2)
            .coerceAtLeast(CHART_MIN_HEIGHT_DP.toFloat())
            .coerceAtMost(CHART_MAX_HEIGHT_DP.toFloat())
        chartCollapsedHeight = newHeight
    }

    // 收集 ViewModel 副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MarketEffect.NavigateToTrading -> onNavigateToTrading(effect.symbol)
                is MarketEffect.ShowError -> { /* 外部 Snackbar 处理 */ }
            }
        }
    }

    val selectedData = state.marketDataList.find { it.symbol == state.selectedSymbol }

    Scaffold(
        topBar = {
            // ——— 1. 顶部导航栏 ———
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 品牌标识色块
                        Box(
                            modifier = Modifier
                                .width(20.dp).height(20.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp),
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "期货",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ——— 2. 合约切换标签 ———
            ContractSwitchingTabs(
                symbols = listOf(
                    "HK00700" to "腾讯",
                    "HK09988" to "阿里",
                    "HK03690" to "美团",
                    "HK00005" to "汇丰",
                    "HK00941" to "中移动",
                ),
                selectedSymbol = state.selectedSymbol,
                onSelect = { viewModel.onIntent(MarketIntent.SelectSymbol(it)) },
            )

            // ——— 3. 自选合约行 ———
            SelfSelectedRow(
                symbols = listOf(
                    "HK00700" to "00700.HK",
                    "HK09988" to "09988.HK",
                    "HK03690" to "03690.HK",
                ),
                selectedSymbol = state.selectedSymbol,
                onSelect = { viewModel.onIntent(MarketIntent.SelectSymbol(it)) },
            )

            // ——— 可滚动区域（图表 + 深度 + 成交） ———
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                // ——— 4. 价格概览卡片 ———
                selectedData?.let { md ->
                    PriceOverviewCard(
                        symbol = md.symbol,
                        price = md.price,
                        change = md.change,
                        changePercent = md.changePercent,
                        open = md.open,
                        high = md.high,
                        low = md.low,
                        volume = md.volume,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ——— 5. 图表周期选择 ———
                ChartPeriodRow(
                    selected = state.selectedInterval,
                    onSelect = { viewModel.onIntent(MarketIntent.SelectInterval(it)) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(Modifier.height(4.dp))

                // ——— 6. K线图 ———
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedChartHeight)
                        .padding(horizontal = 12.dp),
                ) {
                    if (state.isKlineLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        KlineChart(
                            candles = state.klineData,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ——— 7. 盘口深度 + 成交明细 ———
                selectedData?.let { md ->
                    DepthAndTradesPanel(
                        bids = state.depthBids,
                        asks = state.depthAsks,
                        maxBidVol = state.maxBidVolume,
                        maxAskVol = state.maxAskVolume,
                        currentPrice = md.price,
                        priceDirection = md.change.compareTo(0),
                        trades = state.recentTrades,
                        onPriceClick = { /* 可填充至下单页 */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(horizontal = 12.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ==================== 合约切换标签 ====================

/**
 * 合约切换标签行 — 水平可滚动，选中项高亮。
 */
@Composable
private fun ContractSwitchingTabs(
    symbols: List<Pair<String, String>>,
    selectedSymbol: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(symbols, key = { it.first }) { (key, label) ->
            val isSelected = key == selectedSymbol
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(key) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(4.dp),
            )
        }
    }
}

// ==================== 自选合约行 ====================

/**
 * 自选合约横向滑动行。
 * 选中合约底部显示主色指示条。
 */
@Composable
private fun SelfSelectedRow(
    symbols: List<Pair<String, String>>,
    selectedSymbol: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(symbols, key = { it.first }) { (key, label) ->
            val isSelected = key == selectedSymbol
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(key) },
                    )
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 选中底部指示条
                if (isSelected) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(3.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
        }
    }
}

// ==================== 图表周期选择行 ====================

/**
 * K线图表周期切换按钮行。
 */
@Composable
private fun ChartPeriodRow(
    selected: CandleInterval,
    onSelect: (CandleInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    val intervals = listOf(
        CandleInterval.M1 to "1分",
        CandleInterval.M5 to "5分",
        CandleInterval.M15 to "15分",
        CandleInterval.M30 to "30分",
        CandleInterval.H1 to "60分",
        CandleInterval.D1 to "日线",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        intervals.forEach { (interval, label) ->
            val isSelected = interval == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(interval) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(4.dp),
            )
        }
    }
}
