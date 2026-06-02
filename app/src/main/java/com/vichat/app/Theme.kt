package com.vichat.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class AppTheme(val key: String, val label: String) {
    BLUE_PURPLE("blue_purple", "Сине-фиолетовая"),
    BLACK_GOLD("black_gold", "Чёрно-золотая"),
    PURPLE_PINK("purple_pink", "Фиолетово-розовая"),
    EMERALD_TEAL("emerald_teal", "Изумрудная")
}

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

// ─── Blue-Purple (original) ────────────────────────────────────────────────
private val BluePurpleLight = lightColorScheme(
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

private val BluePurpleDark = darkColorScheme(
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

// ─── Black-Gold ────────────────────────────────────────────────────────────
private val BlackGoldLight = lightColorScheme(
    primary = Color(0xFFB8860B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4A017),
    secondary = Color(0xFF8B6914),
    background = Color(0xFFFFFAF0),
    surface = Color.White,
    onBackground = Color(0xFF1a1a0a),
    onSurface = Color(0xFF1a1a0a),
    outline = Color(0xFFE8DCC8),
)

private val BlackGoldDark = darkColorScheme(
    primary = Color(0xFFFFD700),
    onPrimary = Color(0xFF1a1a0a),
    primaryContainer = Color(0xFFB8860B),
    secondary = Color(0xFFDAA520),
    background = Color(0xFF12120a),
    surface = Color(0xFF1E1E12),
    onBackground = Color(0xFFE8E0C8),
    onSurface = Color(0xFFE8E0C8),
    outline = Color(0xFF3A3520),
)

// ─── Purple-Pink (cyberpunk) ───────────────────────────────────────────────
private val PurplePinkLight = lightColorScheme(
    primary = Color(0xFF7B2D8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9C27B0),
    secondary = Color(0xFFC2185B),
    background = Color(0xFFFDF2F8),
    surface = Color.White,
    onBackground = Color(0xFF1a0a1a),
    onSurface = Color(0xFF1a0a1a),
    outline = Color(0xFFE8D0E8),
)

private val PurplePinkDark = darkColorScheme(
    primary = Color(0xFFCE5AE8),
    onPrimary = Color(0xFF0a0a1a),
    primaryContainer = Color(0xFF7B2D8E),
    secondary = Color(0xFFFF69B4),
    background = Color(0xFF0d0d1a),
    surface = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE8D0F0),
    onSurface = Color(0xFFE8D0F0),
    outline = Color(0xFF3A2A4A),
)

// ─── Emerald-Teal ──────────────────────────────────────────────────────────
private val EmeraldTealLight = lightColorScheme(
    primary = Color(0xFF1B8A5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2EAD75),
    secondary = Color(0xFF0D9488),
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    onBackground = Color(0xFF0a1a0a),
    onSurface = Color(0xFF0a1a0a),
    outline = Color(0xFFC8E0D0),
)

private val EmeraldTealDark = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF0a1a0a),
    primaryContainer = Color(0xFF1B8A5C),
    secondary = Color(0xFF2DD4BF),
    background = Color(0xFF0a1a0a),
    surface = Color(0xFF0F1F0F),
    onBackground = Color(0xFFC8E8D0),
    onSurface = Color(0xFFC8E8D0),
    outline = Color(0xFF2A3A2A),
)

fun appColorScheme(themeKey: String, dark: Boolean): ColorScheme = when (themeKey) {
    "black_gold" -> if (dark) BlackGoldDark else BlackGoldLight
    "purple_pink" -> if (dark) PurplePinkDark else PurplePinkLight
    "emerald_teal" -> if (dark) EmeraldTealDark else EmeraldTealLight
    else -> if (dark) BluePurpleDark else BluePurpleLight
}

fun appThemeFromKey(key: String): AppTheme =
    AppTheme.entries.firstOrNull { it.key == key } ?: AppTheme.BLUE_PURPLE

val LocalIsDarkTheme = compositionLocalOf { false }

@Composable
fun ViChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeKey: String = "blue_purple",
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = appColorScheme(themeKey, darkTheme),
            content = content
        )
    }
}
