package com.hackfuture.trading.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackfuture.trading.i18n.LocalLanguage
import com.hackfuture.trading.ui.theme.Radius
import com.hackfuture.trading.ui.theme.Spacing
import com.hackfuture.trading.ui.theme.TradingTypography

/**
 * 语言选择页面 — 展示三种语言选项，当前选中项带 ✔️ 标记。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    currentLanguage: String = LocalLanguage.current,
    onLanguageSelected: (String) -> Unit = {},
) {
    val languages = listOf(
        LanguageOption("zh", "简体中文"),
        LanguageOption("zh-rTW", "繁體中文"),
        LanguageOption("en", "English"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语言 / Language", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                "选择语言 / Select Language",
                style = TradingTypography.Callout,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))

            Card(
                shape = RoundedCornerShape(Radius.md),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column {
                    languages.forEachIndexed { index, option ->
                        LanguageItem(
                            label = option.displayName,
                            isSelected = currentLanguage == option.code,
                            onClick = { onLanguageSelected(option.code) },
                        )
                        if (index < languages.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Text(
                "注意：切换语言后将重建当前页面。",
                style = TradingTypography.Footnote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LanguageItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick,
            )
            .padding(Spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = TradingTypography.Body,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Spacer(Modifier.width(Spacing.md))
            Icon(
                Icons.Default.Check, contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private data class LanguageOption(val code: String, val displayName: String)
