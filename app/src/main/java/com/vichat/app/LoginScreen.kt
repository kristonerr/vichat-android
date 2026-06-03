package com.vichat.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegister by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colorScheme.primary, colorScheme.secondary)))
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ViChat", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Логин") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (passwordVisible) KeyboardType.Text else KeyboardType.Password),
                    trailingIcon = {
                        Text(
                            if (passwordVisible) "Скрыть" else "Показать",
                            fontSize = 12.sp,
                            color = colorScheme.primary,
                            modifier = Modifier
                                .clickable { passwordVisible = !passwordVisible }
                                .padding(8.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            error = "Заполни оба поля"
                            return@Button
                        }
                        loading = true
                        error = null
                        val callback: (Result<AuthResponse>) -> Unit = { result ->
                            mainHandler.post {
                                result.fold(
                                    onSuccess = {
                                    ApiClient.token = it.accessToken
                                        ApiClient.refreshToken = it.refreshToken
                                        ApiClient.currentUserId = it.user.id
                                        PrefsManager.saveToken(it.accessToken)
                                        PrefsManager.saveRefreshToken(it.refreshToken)
                                        loading = false
                                        onLogin(it.accessToken)
                                    },
                                    onFailure = {
                                        error = it.message ?: "Ошибка (${it::class.simpleName})"
                                        loading = false
                                    }
                                )
                            }
                        }
                        if (isRegister) ApiClient.register(username.trim(), password, callback)
                        else ApiClient.login(username.trim(), password, callback)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loading) Text("Подожди...", fontSize = 16.sp)
                    else Text(if (isRegister) "Зарегистрироваться" else "Войти", fontSize = 16.sp)
                }

                Spacer(Modifier.height(12.dp))

                TextButton(onClick = { isRegister = !isRegister; error = null }) {
                    Text(if (isRegister) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать")
                }
            }
        }
    }
}
