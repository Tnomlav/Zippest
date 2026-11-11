package com.decompress.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 空态插画（8.4）：单色线性 + 主色点缀，不用 3D / 渐变。 */
enum class EmptyArt { FOLDER, TRASH, TASKS, SEARCH }

@Composable
internal fun EmptyIllustration(
    kind: EmptyArt,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.5.dp.toPx()
        when (kind) {
            EmptyArt.FOLDER -> {
                drawRoundRect(
                    color = line,
                    topLeft = Offset(w * 0.08f, h * 0.30f),
                    size = Size(w * 0.84f, h * 0.48f),
                    cornerRadius = CornerRadius(w * 0.07f),
                    style = Stroke(stroke)
                )
                drawLine(line, Offset(w * 0.16f, h * 0.30f), Offset(w * 0.40f, h * 0.30f), stroke)
                drawLine(line, Offset(w * 0.40f, h * 0.30f), Offset(w * 0.48f, h * 0.22f), stroke)
                drawLine(line, Offset(w * 0.48f, h * 0.22f), Offset(w * 0.64f, h * 0.22f), stroke)
                drawCircle(accent, radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.56f))
            }
            EmptyArt.TRASH -> {
                drawRoundRect(
                    color = line,
                    topLeft = Offset(w * 0.26f, h * 0.28f),
                    size = Size(w * 0.48f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(stroke)
                )
                drawLine(line, Offset(w * 0.18f, h * 0.26f), Offset(w * 0.82f, h * 0.26f), stroke)
                drawLine(line, Offset(w * 0.40f, h * 0.18f), Offset(w * 0.60f, h * 0.18f), stroke)
                drawLine(accent, Offset(w * 0.42f, h * 0.44f), Offset(w * 0.42f, h * 0.66f), stroke)
                drawLine(accent, Offset(w * 0.58f, h * 0.44f), Offset(w * 0.58f, h * 0.66f), stroke)
            }
            EmptyArt.SEARCH -> {
                drawCircle(
                    color = line,
                    radius = w * 0.22f,
                    center = Offset(w * 0.44f, h * 0.42f),
                    style = Stroke(stroke)
                )
                drawLine(
                    line,
                    Offset(w * 0.60f, h * 0.58f),
                    Offset(w * 0.78f, h * 0.76f),
                    stroke
                )
                drawCircle(accent, radius = w * 0.035f, center = Offset(w * 0.44f, h * 0.42f))
            }
            EmptyArt.TASKS -> {
                drawRoundRect(
                    color = line,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.64f, h * 0.64f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(stroke)
                )
                drawLine(line, Offset(w * 0.30f, h * 0.38f), Offset(w * 0.70f, h * 0.38f), stroke)
                drawLine(line, Offset(w * 0.30f, h * 0.52f), Offset(w * 0.62f, h * 0.52f), stroke)
                drawLine(accent, Offset(w * 0.30f, h * 0.66f), Offset(w * 0.44f, h * 0.66f), stroke)
                drawCircle(accent, radius = w * 0.04f, center = Offset(w * 0.64f, h * 0.66f))
            }
        }
    }
}
