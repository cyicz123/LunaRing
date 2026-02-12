package com.example.menstruation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PinkPrimary,
    onPrimary = Color.White,
    primaryContainer = PinkDark,
    onPrimaryContainer = Color.White,
    secondary = PinkLight,
    onSecondary = Color.White,
    tertiary = PinkLight,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderGray,
    error = Error,
    onError = Color.White
)

// 浅色主题（备用）
private val LightColorScheme = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = Color.White,
    primaryContainer = PinkLight,
    onPrimaryContainer = PinkDark,
    secondary = PinkDark,
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    error = Error,
    onError = Color.White
)

@Composable
fun MenstruationTheme(
    darkTheme: Boolean = true, // 默认使用深色主题
    dynamicColor: Boolean = false, // 禁用动态颜色
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}