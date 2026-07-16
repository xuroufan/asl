package com.hackfuture.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hackfuture.feature.auth.AuthIntent
import com.hackfuture.feature.auth.AuthViewModel

/**
 * 注册页面 — 与登录页设计一致。
 */
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val canRegister = username.isNotBlank() && email.isNotBlank() &&
        password.isNotBlank() && !state.isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // ——— 品牌标识 ———
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("B", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text("创建账号", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("注册期货交易账户", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(32.dp))

            // ——— 注册表单卡片 ———
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    // 用户名
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("用户名") }, placeholder = { Text("请输入用户名") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        enabled = !state.isLoading,
                    )
                    Spacer(Modifier.height(12.dp))

                    // 邮箱
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("邮箱") }, placeholder = { Text("请输入邮箱") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        enabled = !state.isLoading,
                    )
                    Spacer(Modifier.height(12.dp))

                    // 密码
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("密码") }, placeholder = { Text("设置登录密码") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        enabled = !state.isLoading,
                    )

                    state.registerError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(Modifier.height(20.dp))

                    // 注册按钮
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onIntent(AuthIntent.Register(username.trim(), email.trim(), password))
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = canRegister,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("注册", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ——— 回到登录 ———
            TextButton(onClick = onNavigateToLogin) {
                Text("已有账号？登录", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
