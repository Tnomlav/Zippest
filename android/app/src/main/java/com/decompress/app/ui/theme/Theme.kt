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
import com.decompress.app.data.AppAccent

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
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F1),
    surfaceContainer = Color(0xFFEBEFEB),
    surfaceContainerHigh = Color(0xFFE5EAE6),
    surfaceContainerHighest = Color(0xFFDFE6E0),
    onSurfaceVariant = Color(0xFF59625D),
    outline = Color(0xFFB8C4BB),
    outlineVariant = Color(0xFFDDE4DE),
    secondary = Color(0xFF4F6B5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9E2),
    onSecondaryContainer = Color(0xFF12201A),
    tertiary = Color(0xFF41586A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E3EE),
    onTertiaryContainer = Color(0xFF0E1E2A),
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
    surfaceContainerLowest = Color(0xFF0C110E),
    surfaceContainerLow = Color(0xFF171E1A),
    surfaceContainer = Color(0xFF1B231F),
    surfaceContainerHigh = Color(0xFF222B26),
    surfaceContainerHighest = Color(0xFF2C3630),
    onSurfaceVariant = Color(0xFFB8C4BB),
    outline = Color(0xFF4E5D54),
    outlineVariant = Color(0xFF33403A),
    secondary = Color(0xFFA9C7B8),
    onSecondary = Color(0xFF16251E),
    secondaryContainer = Color(0xFF2A3B34),
    onSecondaryContainer = Color(0xFFCFE6DA),
    tertiary = Color(0xFFA8C0D4),
    onTertiary = Color(0xFF10202B),
    tertiaryContainer = Color(0xFF2A3B48),
    onTertiaryContainer = Color(0xFFD3E4F1),
    error = Color(0xFFF0A07A),
    onError = Color(0xFF3A1000)
)

/**
 * 字阶：完整 13 档，数值与 `design/tokens.md` 的 Type scale 表一致。
 * 之前只覆盖 5 档，其余档位走 M3 默认值（Roboto 22/16/14…），中英文混排时行高对不齐。
 */
private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun ZippestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AppAccent = AppAccent.GREEN,
    content: @Composable () -> Unit
) {
    val light = LightColors.copy(
        primary = Color(accent.lightPrimary),
        onPrimary = Color.White,
        primaryContainer = Color(accent.lightContainer),
        onPrimaryContainer = Color(accent.lightOnContainer)
    )
    val dark = DarkColors.copy(
        primary = Color(accent.darkPrimary),
        onPrimary = Color(0xFF0B1512),
        primaryContainer = Color(accent.darkContainer),
        onPrimaryContainer = Color(accent.darkOnContainer)
    )
    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        typography = AppTypography,
        shapes = ZippestShapes,
        content = content
    )
}
