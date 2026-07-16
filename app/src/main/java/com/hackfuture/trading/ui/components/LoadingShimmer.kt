package com.hackfuture.trading.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingShimmer(

    modifier: Modifier = Modifier,
    itemCount: Int = 5,
    isDarkTheme: Boolean = false,
) {
    val baseColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
    val highlightColor = if (isDarkTheme) Color(0xFF3C3C3C) else Color(0xFFF5F5F5)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f),
    )
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(itemCount) { index ->
            ShimmerItem(brush = brush, isHeader = index == 0)
        }
    }
}

@Composable
private fun ShimmerItem(brush: Brush, isHeader: Boolean) {
    if (isHeader) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f).height(24.dp)
                .clip(RoundedCornerShape(4.dp)).background(brush),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(brush),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth(0.5f).height(14.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(0.3f).height(10.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush),
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(0.25f).height(16.dp)
                .clip(RoundedCornerShape(4.dp)).background(brush),
        )
    }
}

@Composable
fun MarketListShimmer(modifier: Modifier = Modifier, isDarkTheme: Boolean = false) {
    LoadingShimmer(modifier = modifier, itemCount = 6, isDarkTheme = isDarkTheme)
}

@Composable
fun OrderListShimmer(modifier: Modifier = Modifier, isDarkTheme: Boolean = false) {
    LoadingShimmer(modifier = modifier, itemCount = 4, isDarkTheme = isDarkTheme)
}
