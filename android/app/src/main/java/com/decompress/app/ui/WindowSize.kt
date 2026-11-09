package com.decompress.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 以 dp 宽度划分窗口尺寸，断点跟 Material 的建议一致：
 * compact < 600 ≤ medium < 840 ≤ expanded。
 * 用 `screenWidthDp` 而不是像素，折叠屏展开/分屏时同样成立。
 */
internal enum class ZippestWindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
@ReadOnlyComposable
internal fun rememberZippestWindowSize(): ZippestWindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> ZippestWindowSize.EXPANDED
        widthDp >= 600 -> ZippestWindowSize.MEDIUM
        else -> ZippestWindowSize.COMPACT
    }
}
