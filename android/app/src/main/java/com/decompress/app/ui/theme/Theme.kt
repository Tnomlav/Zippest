package com.decompress.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F2E7),
    onPrimaryContainer = Color(0xFF083B2B),
    background = Color(0xFFF6F7F5),
    onBackground = Color(0xFF1B211E),
    surface = Color.White,
    onSurface = Color(0xFF1B211E),
    surfaceVariant = Color(0xFFE9EFEA),
    onSurfaceVariant = Color(0xFF59625D),
    outline = Color(0xFFB8C4BB),
    error = Color(0xFFC4622D),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6B6),
    onPrimary = Color(0xFF0A2C22),
    primaryContainer = Color(0xFF1D4B3A),
    onPrimaryContainer = Color(0xFFC8F4E2),
    background = Color(0xFF121815),
    onBackground = Color(0xFFE8EEE9),
    surface = Color(0xFF1B231F),
    onSurface = Color(0xFFE8EEE9),
    surfaceVariant = Color(0xFF2A342E),
    onSurfaceVariant = Color(0xFFB8C4BB),
    outline = Color(0xFF4E5D54),
    error = Color(0xFFF0A07A),
    onError = Color(0xFF3A1000)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun ZippestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
