package com.decompress.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 设计 token —— 与 `design/tokens.md` 一一对应，那边是唯一事实来源。
 * 颜色不在这里：颜色跟随主题（见 `Theme.kt` 的 LightColors / DarkColors 与 `AppAccent`）。
 */

/** 间距：4 dp 基线。`xl` 是页面版心，`lg` 是区块间距。 */
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

/** 圆角：8 / 12 / 16 / 20 / 24 五级，弹层顶角 28。 */
object ZippestRadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val sheet = 28.dp
    val pill = 999.dp
}

/** 尺寸：图标 24 dp，可点区域 ≥ 48 dp，列表行高 64 dp 起。 */
object ZippestSize {
    val iconSm = 20.dp
    val icon = 24.dp
    val touchTarget = 48.dp
    val rowMin = 64.dp
}

/** 动效时长（毫秒）：短反馈 120，常规过渡 200，页面级 320。 */
object ZippestMotion {
    const val fast = 120
    const val normal = 200
    const val slow = 320
}

/** Material 组件默认形状：小控件 8、卡片 12、对话框/弹层 16–28。 */
val ZippestShapes = Shapes(
    extraSmall = RoundedCornerShape(ZippestRadius.xs),
    small = RoundedCornerShape(ZippestRadius.sm),
    medium = RoundedCornerShape(ZippestRadius.md),
    large = RoundedCornerShape(ZippestRadius.lg),
    extraLarge = RoundedCornerShape(ZippestRadius.sheet)
)
