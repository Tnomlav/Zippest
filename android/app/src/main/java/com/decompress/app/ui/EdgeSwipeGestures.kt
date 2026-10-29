package com.decompress.app.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun EdgeSwipeBackZones(
    onBack: () -> Unit,
    onHome: (() -> Unit)? = null
) {
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalEdgeZone(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(24.dp)
                .fillMaxHeight(),
            threshold = threshold,
            shouldTrigger = { it > 0f },
            onTrigger = onBack
        )
        HorizontalEdgeZone(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .fillMaxHeight(),
            threshold = threshold,
            shouldTrigger = { it < 0f },
            onTrigger = onBack
        )
        if (onHome != null) {
            var dragDistance = 0f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInput(onHome, threshold) {
                        detectVerticalDragGestures(
                            onDragStart = { dragDistance = 0f },
                            onDragEnd = {
                                if (dragDistance < -threshold) onHome()
                            },
                            onVerticalDrag = { change, amount ->
                                dragDistance += amount
                                if (abs(dragDistance) > threshold / 3f) {
                                    change.consume()
                                }
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun HorizontalEdgeZone(
    modifier: Modifier,
    threshold: Float,
    shouldTrigger: (Float) -> Boolean,
    onTrigger: () -> Unit
) {
    var dragDistance = 0f
    Box(
        modifier = modifier.pointerInput(threshold, shouldTrigger, onTrigger) {
            detectHorizontalDragGestures(
                onDragStart = { dragDistance = 0f },
                onDragEnd = {
                    if (shouldTrigger(dragDistance) && abs(dragDistance) > threshold) {
                        onTrigger()
                    }
                },
                onHorizontalDrag = { change, amount ->
                    dragDistance += amount
                    if (abs(dragDistance) > threshold / 3f) {
                        change.consume()
                    }
                }
            )
        }
    )
}
