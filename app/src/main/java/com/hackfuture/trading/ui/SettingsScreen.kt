package com.hackfuture.trading.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackfuture.trading.i18n.LocalLanguage
import com.hackfuture.trading.ui.theme.Radius
import com.hackfuture.trading.ui.theme.Spacing
import com.hackfuture.trading.ui.theme.TradingColors
import com.hackfuture.trading.ui.theme.TradingTypography

/**
 * 设置/个人中心页面 — 包含账户信息、语言切换、主题、通知、关于、退出。
 */
@Composable
fun SettingsScreen(
    onNavigateToLanguage: () -> Unit = {},
    onThemeChange: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val currentLang = LocalLanguage.current

    // 当前语言显示名
    val langDisplay = when (currentLang) {
        "zh" -> "简体中文"
        "zh-rTW" -> "繁體中文"
        "en" -> "English"
        else -> "简体中文"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ——— 用户信息头部 ———
        Spacer(Modifier.height(Spacing.xl))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像占位
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(Spacing.xl))
            Column {
                Text(
                    "期货交易用户",
                    style = TradingTypography.Title2,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "ID: UH****1234",
                    style = TradingTypography.Footnote,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))

        // ——— 设置项分组 ———
        SettingsGroup("设置") {
            SettingsItem(
                icon = Icons.Default.Language,
                label = "语言",
                trailingText = langDisplay,
                onClick = onNavigateToLanguage,
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.DarkMode,
                label = "主题",
                trailingText = "跟随系统",
                onClick = { onThemeChange?.invoke("system") },
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.Notifications,
                label = "通知设置",
                onClick = { },
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.Security,
                label = "安全设置",
                onClick = { },
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        SettingsGroup("其他") {
            SettingsItem(
                icon = Icons.Default.Info,
                label = "关于",
                trailingText = "v1.0.0",
                onClick = { },
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "退出登录",
                iconTint = TradingColors.buy,
                labelColor = TradingColors.buy,
                onClick = { showLogoutDialog = true },
            )
        }

        Spacer(Modifier.height(Spacing.xxxl + 16.dp))
    }

    // 退出确认弹窗
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录", fontWeight = FontWeight.Bold) },
            text = { Text("确定退出登录?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TradingColors.buy,
                    ),
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

/**
 * 设置项分组卡片。
 */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
        Text(
            title,
            style = TradingTypography.Callout,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
        )
        Card(
            shape = RoundedCornerShape(Radius.md),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            content()
        }
    }
}

/**
 * 单行设置项：图标 + 标签 + 尾部文字(可选) + 右箭头。
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(Spacing.lg))
        Text(
            label,
            style = TradingTypography.Body,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(
                trailingText,
                style = TradingTypography.Footnote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 设置项分割线。
 */
@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = Spacing.xl),
    )
}
