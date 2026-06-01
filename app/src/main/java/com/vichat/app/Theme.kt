package com.vichat.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

val Blue500 = Color(0xFF667eea)
val Blue700 = Color(0xFF5a6fd9)
val Purple500 = Color(0xFF764ba2)
val Green500 = Color(0xFF4CAF50)
val Red500 = Color(0xFFFF6B6B)
val Orange100 = Color(0xFFFFF3E0)
val Gray100 = Color(0xFFF5F5F5)
val Gray200 = Color(0xFFE8E8E8)
val Gray500 = Color(0xFF888888)
val Gray700 = Color(0xFF555555)
val Gray900 = Color(0xFF333333)

private val LightColors = lightColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    primaryContainer = Blue700,
    secondary = Purple500,
    background = Color(0xFFF8F9FC),
    surface = Color.White,
    onBackground = Gray900,
    onSurface = Gray900,
    outline = Gray200,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8899ff),
    onPrimary = Color(0xFF1a1a2e),
    primaryContainer = Color(0xFF4a5fc9),
    secondary = Color(0xFFa077c8),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF333333),
)

val LocalIsDarkTheme = compositionLocalOf { false }

@Composable
fun ViChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
