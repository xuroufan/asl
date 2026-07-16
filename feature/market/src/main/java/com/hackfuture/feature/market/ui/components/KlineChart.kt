package com.hackfuture.feature.market.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.hackfuture.core.model.CandleData

private val CandleUpColor = Color(0xFFFF6B6B)
private val CandleDownColor = Color(0xFF00C853)

@Composable
fun KlineChart(
    candles: List<CandleData>,
    modifier: Modifier = Modifier,
) {
    if (candles.isEmpty()) return
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var crosshairIdx by remember { mutableIntStateOf(-1) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val crossColor = Color.White.copy(alpha = 0.6f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            isAntiAlias = true
        }
    }.also { paint ->
        paint.color = onSurfaceColor.hashCode()
        paint.textSize = with(density) { 10.sp.toPx() }
    }
    val cw = (4f * scale).coerceIn(2f, 20f)
    val step = cw + 2f
    val visibleCount = (candles.size * (1f / scale)).toInt().coerceIn(10, candles.size)
    val startIdx = ((candles.size - visibleCount) + scrollOffset.toInt())
        .coerceIn(0, (candles.size - visibleCount).coerceAtLeast(0))
    val vis = candles.subList(startIdx, (startIdx + visibleCount).coerceAtMost(candles.size))
    if (vis.isEmpty()) return
    val high = vis.maxOf { it.high }; val low = vis.minOf { it.low }
    val maxVol = vis.maxOfOrNull { it.volume }?.toDouble() ?: 1.0

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    scrollOffset = (scrollOffset + pan.x / cw)
                        .coerceIn(0f, (candles.size - visibleCount).coerceAtLeast(0).toFloat())
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val idx = (offset.x / step).toInt().coerceIn(0, vis.size - 1)
                    crosshairIdx = if (idx == crosshairIdx) -1 else startIdx + idx
                }
            },
        ) {
            val chartH = size.height * 0.8f; val volH = size.height * 0.2f
            for (i in 0..4) {
                val y = chartH * i / 4
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
            }
            vis.forEachIndexed { idx, c ->
                val x = step * (idx + 0.5f)
                val isUp = c.close >= c.open
                val col = if (isUp) CandleUpColor else CandleDownColor
                val oy = yPos(c.open, high, low, chartH)
                val cy = yPos(c.close, high, low, chartH)
                drawLine(col, Offset(x, yPos(c.high, high, low, chartH)),
                    Offset(x, yPos(c.low, high, low, chartH)), strokeWidth = 1f)
                drawRect(col, Offset(x - cw / 3, minOf(oy, cy)),
                    Size(cw * 0.66f, (maxOf(oy, cy) - minOf(oy, cy)).coerceAtLeast(1f)))
                // 成交量
                val vh = (c.volume / maxVol * volH * 0.8f).toFloat()
                drawRect(col.copy(alpha = 0.3f), Offset(x - cw / 3, size.height - vh), Size(cw * 0.66f, vh))
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText(fmtPrice(high), 4f, textPaint.textSize + 4f, textPaint)
                drawText(fmtPrice(low), 4f, chartH - 4f, textPaint)
            }
            if (crosshairIdx in startIdx until (startIdx + vis.size)) {
                val c = candles[crosshairIdx]; val li = crosshairIdx - startIdx; val cx = step * (li + 0.5f)
                val cy = yPos(c.close, high, low, chartH)
                drawLine(crossColor, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
                drawLine(crossColor, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
                drawCircle(Color.White, 3f, Offset(cx, cy))
            }
        }
    }
}

private fun yPos(price: Double, high: Double, low: Double, h: Float): Float =
    ((high - price) / (high - low).coerceAtLeast(0.0001)).toFloat() * h

private fun fmtPrice(v: Double) = if (v >= 100) "%.2f".format(v) else "%.6f".format(v)
