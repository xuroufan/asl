package com.hackfuture.feature.market.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hackfuture.core.model.PriceDirection
import com.hackfuture.core.model.Quote
import com.hackfuture.core.model.QuoteCategory
import com.hackfuture.core.model.SortMode
import com.hackfuture.core.util.NumberUtil
import com.hackfuture.feature.market.MarketOverviewEffect
import com.hackfuture.feature.market.MarketOverviewIntent
import com.hackfuture.feature.market.MarketOverviewViewModel

// 本地颜色常量（遵循全局主题色系）
private val UpColor = Color(0xFFFF6B6B)
private val DownColor = Color(0xFF00C853)
private val FlatColor = Color(0xFF9E9E9E)
private val GreenIndicator = Color(0xFF4CAF50)
private val GrayIndicator = Color(0xFF757575)

private val Sm = 8.dp
private val Md = 12.dp
private val Lg = 16.dp
private val Xxl = 32.dp

/**
 * 行情看板 — 专业交易终端风格的全合约行情概览页面。
 *
 * 区块说明：
 * - TopAppBar: 标题"行情看板"、连接状态灯、刷新/设置按钮
 * - MarketSummaryBar: 市场涨跌/平盘合约统计与更新时间
 * - SearchBar: 关键词搜索过滤
 * - CategoryFilterRow: 品种分类筛选（全部/港股/商品期货）
 * - SortHeaderRow: 可排序表头（合约/最新价/涨跌幅/成交量）
 * - QuoteListItem: 单合约报价行，含代码名称、实时价格、涨跌幅、成交量柱
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketOverviewScreen(
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: MarketOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // 收集 ViewModel 发出的导航等副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MarketOverviewEffect.NavigateToDetail -> onNavigateToDetail(effect.symbol)
                is MarketOverviewEffect.ShowError -> { /* 由外部 SnackbarHost 处理 */ }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ——— 顶部导航栏 ———
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("行情看板", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    ConnectionIndicator(isConnected = state.isConnected)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.onIntent(MarketOverviewIntent.Refresh) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Lg),
            verticalArrangement = Arrangement.spacedBy(Sm),
        ) {
            // ——— 市场统计摘要横幅 ———
            item(key = "summary") {
                MarketSummaryBar(
                    upCount = state.upCount,
                    downCount = state.downCount,
                    flatCount = state.flatCount,
                )
            }

            // ——— 搜索栏 ———
            item(key = "search") {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onIntent(MarketOverviewIntent.Search(it)) },
                    onClear = { viewModel.onIntent(MarketOverviewIntent.Search("")) },
                    onSearch = { focusManager.clearFocus() },
                )
            }

            // ——— 分类筛选标签 ———
            item(key = "categories") {
                CategoryFilterRow(
                    selectedCategory = state.selectedCategory,
                    onSelectCategory = { viewModel.onIntent(MarketOverviewIntent.SelectCategory(it)) },
                )
            }

            // ——— 表头排序行 ———
            item(key = "header") {
                SortHeaderRow(
                    currentSort = state.sortMode,
                    sortDescending = state.sortDescending,
                    onSelectSort = { viewModel.onIntent(MarketOverviewIntent.SelectSort(it)) },
                )
            }

            // ——— 加载指示器 ———
            if (state.isLoading && state.quotes.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // ——— 搜索无结果空状态 ———
            if (!state.isLoading && state.displayQuotes.isEmpty() && state.quotes.isNotEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "未匹配到合约",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ——— 合约报价列表 ———
            itemsIndexed(
                items = state.displayQuotes,
                key = { _, quote -> quote.symbol },
            ) { index, quote ->
                QuoteListItem(
                    quote = quote,
                    onClick = { viewModel.onIntent(MarketOverviewIntent.SelectSymbol(quote.symbol)) },
                    showDivider = index < state.displayQuotes.lastIndex,
                )
            }

            // 底部留白
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(Xxl))
            }
        }
    }
}

// ==================== 子组件 ====================

/**
 * 连接状态指示灯。
 * 绿色圆点 = WebSocket 已连接，灰色 = 断开。
 */
@Composable
private fun ConnectionIndicator(isConnected: Boolean) {
    val color by animateColorAsState(
        targetValue = if (isConnected) GreenIndicator else GrayIndicator,
        animationSpec = tween(400), label = "connColor",
    )
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
}

/**
 * 市场统计摘要横幅。
 * 显示当前市场上合约的上涨/下跌/平盘数量，带方向图标与颜色。
 */
@Composable
private fun MarketSummaryBar(
    upCount: Int,
    downCount: Int,
    flatCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Lg, vertical = Md),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatBadge(Icons.Default.TrendingUp, upCount, UpColor, "上涨")
            Box(
                modifier = Modifier
                    .width(1.dp).height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            StatBadge(Icons.Default.TrendingDown, downCount, DownColor, "下跌")
            Box(
                modifier = Modifier
                    .width(1.dp).height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            StatBadge(Icons.Default.TrendingFlat, flatCount, FlatColor, "平盘")
        }
    }
}

/**
 * 单个统计徽标：方向图标 + 数量 + 标签文本。
 */
@Composable
private fun StatBadge(icon: ImageVector, count: Int, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon, contentDescription = null,
            tint = color, modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "$count", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = color,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 搜索栏 — 输入关键词实时过滤合约。
 * 支持 IME 搜索动作关闭键盘。右侧显示清除按钮。
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("搜索合约代码或名称", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "清除", modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(Md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

/**
 * 品种分类筛选标签行。
 * 支持"全部"（null）、加密货币、股指期货、商品期货、外汇。
 */
@Composable
private fun CategoryFilterRow(
    selectedCategory: QuoteCategory?,
    onSelectCategory: (QuoteCategory?) -> Unit,
) {
    val categories = listOf(null to "全部") +
        QuoteCategory.entries.map { it to it.label }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(Sm)) {
        items(categories, key = { it.first?.name ?: "all" }) { (category, label) ->
            val isSelected = selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/**
 * 可排序的表头行。
 * 点击某一列切换为该列排序，再次点击切换升降序。
 * 激活的排序列显示 ▼（降序）或 ▲（升序）箭头。
 */
@Composable
private fun SortHeaderRow(
    currentSort: SortMode,
    sortDescending: Boolean,
    onSelectSort: (SortMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortHeaderCell(
            label = "合约", mode = SortMode.SYMBOL,
            isActive = currentSort == SortMode.SYMBOL,
            sortDescending = sortDescending,
            onClick = { onSelectSort(SortMode.SYMBOL) },
            modifier = Modifier.weight(0.28f),
            alignment = TextAlign.Start,
        )
        SortHeaderCell(
            label = "最新价", mode = SortMode.PRICE,
            isActive = currentSort == SortMode.PRICE,
            sortDescending = sortDescending,
            onClick = { onSelectSort(SortMode.PRICE) },
            modifier = Modifier.weight(0.24f),
            alignment = TextAlign.End,
        )
        SortHeaderCell(
            label = "涨跌幅", mode = SortMode.CHANGE,
            isActive = currentSort == SortMode.CHANGE,
            sortDescending = sortDescending,
            onClick = { onSelectSort(SortMode.CHANGE) },
            modifier = Modifier.weight(0.22f),
            alignment = TextAlign.End,
        )
        SortHeaderCell(
            label = "成交量", mode = SortMode.VOLUME,
            isActive = currentSort == SortMode.VOLUME,
            sortDescending = sortDescending,
            onClick = { onSelectSort(SortMode.VOLUME) },
            modifier = Modifier.weight(0.26f),
            alignment = TextAlign.End,
        )
    }
}

/**
 * 单个表头排序单元格。
 */
@Composable
private fun SortHeaderCell(
    label: String,
    mode: SortMode,
    isActive: Boolean,
    sortDescending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: TextAlign = TextAlign.Start,
) {
    val textColor = if (isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    TextButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
    ) {
        Text(
            text = if (isActive) "$label ${if (sortDescending) "▼" else "▲"}" else label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            textAlign = alignment,
            maxLines = 1,
        )
    }
}

// ==================== 合约报价行 ====================

/**
 * 单合约报价行。
 *
 * 布局（4列）：
 *   列1（28%）：合约代码（粗体）+ 名称（小字）
 *   列2（24%）：最新价（等宽字体，涨红色/跌绿色）
 *   列3（22%）：涨跌额 + 涨跌幅（涨红色/跌绿色）
 *   列4（26%）：成交量（紧凑格式）+ 成交量迷你柱状条
 *
 * 交互：点击整行 → onNavigateToDetail(symbol)
 * 视觉反馈：涨跌时行背景色微闪（红/绿 4% 透明度）
 */
@Composable
private fun QuoteListItem(
    quote: Quote,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    val priceColor = when (quote.direction) {
        PriceDirection.UP -> UpColor
        PriceDirection.DOWN -> DownColor
        PriceDirection.FLAT -> MaterialTheme.colorScheme.onSurface
    }
    // 行背景色根据方向微闪动画
    val bgColor by animateColorAsState(
        targetValue = when (quote.direction) {
            PriceDirection.UP -> UpColor.copy(alpha = 0.04f)
            PriceDirection.DOWN -> DownColor.copy(alpha = 0.04f)
            PriceDirection.FLAT -> Color.Transparent
        },
        animationSpec = tween(300), label = "rowBg",
    )

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            shape = RoundedCornerShape(4.dp),
            color = bgColor,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Md, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 列1：合约代码 + 名称
                Column(modifier = Modifier.weight(0.28f)) {
                    Text(
                        text = quote.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = quote.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 列2：最新价
                Text(
                    text = NumberUtil.formatPrice(quote.lastPrice),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = priceColor,
                    modifier = Modifier.weight(0.24f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )

                // 列3：涨跌额 + 涨跌幅
                Column(
                    modifier = Modifier.weight(0.22f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "${if (quote.change >= 0) "+" else ""}${NumberUtil.formatPrice(quote.change)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        fontWeight = FontWeight.Medium,
                        color = priceColor,
                        maxLines = 1,
                    )
                    Text(
                        text = NumberUtil.formatPercent(quote.changePercent),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = priceColor,
                        maxLines = 1,
                    )
                }

                // 列4：成交量 + 柱状条
                Column(
                    modifier = Modifier.weight(0.26f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = formatVolumeCompact(quote.volume),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    VolumeBar(
                        volume = quote.volume.toDouble(),
                        maxVolume = 1_000_000_000_000.0,
                        color = priceColor.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // 行间分割线
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }
    }
}

/**
 * 成交量迷你柱状条。
 * 根据成交量占固定最大值（1万亿）的比例，动画填充宽度。
 */
@Composable
private fun VolumeBar(volume: Double, maxVolume: Double, color: Color) {
    val fraction = (volume / maxVolume).coerceIn(0.0, 1.0).toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(500),
        label = "volumeBar",
    )
    Box(
        modifier = Modifier
            .width(56.dp).height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}

// ==================== 工具函数 ====================

/**
 * 将成交量格式化为中文紧凑形式。
 * >= 1亿 → "X.X亿"，>= 1万 → "X.X万"，否则原数值。
 */
private fun formatVolumeCompact(volume: Long): String = when {
    volume >= 100_000_000 -> "${"%.1f".format(volume / 100_000_000.0)}亿"
    volume >= 10_000 -> "${"%.1f".format(volume / 10_000.0)}万"
    else -> "$volume"
}
