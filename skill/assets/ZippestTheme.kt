/*
 * Zippest Design Language — Compose adapter
 * 与 tokens.json 一一对应；改色/字号/间距先改 token，再改这里，不要在界面里写字面量。
 * 用法：ZippestTheme(accent = ZippestAccent.OCEAN) { App() }
 */
package com.zippest.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- 间距：4dp 基线；xl 是版心，lg 是区块间距 ----
object ZippestSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
}

// ---- 圆角：8/12/16/20/24，弹层 28，胶囊 999 ----
object ZippestRadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val sheet = 28.dp
    val pill = 999.dp
}

// ---- 尺寸 ----
object ZippestSize {
    val iconSm = 20.dp
    val icon = 24.dp
    val touchTarget = 48.dp
    val rowMin = 64.dp
    val kindIconContainer = 40.dp
    val thumbnail = 44.dp
    val topBar = 64.dp
    val bottomBarMin = 64.dp
    val illustration = 72.dp
    val onboardingBadge = 76.dp
    val navPane = 240.dp
    val thirdPane = 320.dp
}

// ---- 动效 ----
object ZippestMotion {
    const val fast = 120
    const val normal = 200
    const val slow = 320
}

// ---- 强调色：只替换 primary 四件套 ----
enum class ZippestAccent(
    val lightPrimary: Long,
    val lightContainer: Long,
    val lightOnContainer: Long,
    val darkPrimary: Long,
    val darkContainer: Long,
    val darkOnContainer: Long
) {
    FOREST(0xFF0F6B4F, 0xFFD9F2E7, 0xFF083B2B, 0xFF7FD6B6, 0xFF1D4B3A, 0xFFC8F4E2),
    OCEAN(0xFF1E5FA8, 0xFFDCE9F8, 0xFF0B2B4D, 0xFF8FC2FF, 0xFF1B3A5C, 0xFFD6E9FF),
    VIOLET(0xFF6A4C93, 0xFFEDE4F7, 0xFF2E1F45, 0xFFC9AEF0, 0xFF3A2B52, 0xFFEEE2FB),
    AMBER(0xFFA8561C, 0xFFF7E5D6, 0xFF4A2408, 0xFFF0B183, 0xFF4A2C16, 0xFFFBE3D2),
    ROSE(0xFFA8365A, 0xFFF8DFE6, 0xFF4C1828, 0xFFF2A6BC, 0xFF4C2030, 0xFFFBDDE5),
    GRAPHITE(0xFF3F4A45, 0xFFE2E7E4, 0xFF1E2522, 0xFFC2CFC8, 0xFF2B342F, 0xFFE4EBE7)
}

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
    surfaceContainer = Color(0xFFF6F8F6),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEDF0ED),
    onSurfaceVariant = Color(0xFF59625D),
    surfaceDim = Color(0xFFDDE4DE),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF2F3531),
    inverseOnSurface = Color(0xFFF1F4F1),
    inversePrimary = Color(0xFF7FD6B6),
    scrim = Color(0xFF000000),
    outline = Color(0xFF7D8C82),
    outlineVariant = Color(0xFFDDE4DE),
    secondary = Color(0xFF4F6B5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9E2),
    onSecondaryContainer = Color(0xFF12201A),
    tertiary = Color(0xFF41586A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E3EE),
    onTertiaryContainer = Color(0xFF0E1E2A),
    error = Color(0xFFA24617),
    onError = Color.White,
    errorContainer = Color(0xFFFBE7DC),
    onErrorContainer = Color(0xFF5A1E00)
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
    surfaceDim = Color(0xFF121815),
    surfaceBright = Color(0xFF2C3630),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFE8EEE9),
    inverseOnSurface = Color(0xFF1B231E),
    inversePrimary = Color(0xFF0F6B4F),
    scrim = Color(0xFF000000),
    outline = Color(0xFF66766B),
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
    onError = Color(0xFF3A1000),
    errorContainer = Color(0xFF6B2A0F),
    onErrorContainer = Color(0xFFFFDBCB)
)

// ---- 13 档字阶，全部等宽数字 ----
private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontFeatureSettings = "tnum"),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum"),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = "tnum"),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
)

val ZippestShapes = Shapes(
    extraSmall = RoundedCornerShape(ZippestRadius.xs),
    small = RoundedCornerShape(ZippestRadius.sm),
    medium = RoundedCornerShape(ZippestRadius.md),
    large = RoundedCornerShape(ZippestRadius.lg),
    extraLarge = RoundedCornerShape(ZippestRadius.sheet)
)

@Composable
fun ZippestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: ZippestAccent = ZippestAccent.FOREST,
    content: @Composable () -> Unit
) {
    val light = LightColors.copy(
        primary = Color(accent.lightPrimary),
        primaryContainer = Color(accent.lightContainer),
        onPrimaryContainer = Color(accent.lightOnContainer),
        inversePrimary = Color(accent.darkPrimary),
        surfaceTint = Color.Transparent
    )
    val dark = DarkColors.copy(
        primary = Color(accent.darkPrimary),
        primaryContainer = Color(accent.darkContainer),
        onPrimaryContainer = Color(accent.darkOnContainer),
        inversePrimary = Color(accent.lightPrimary),
        surfaceTint = Color.Transparent
    )
    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        typography = AppTypography,
        shapes = ZippestShapes,
        content = content
    )
}
