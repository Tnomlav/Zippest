package com.decompress.app.ui

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.ui.theme.ZippestRadius
import kotlinx.coroutines.delay

/** 系统「减少动态效果」是否开启（开发者选项里的动画时长缩放 = 0）。 */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * 三态统一放在内容区，垂直锚点约 40%：不随内容高度漂移。8.6
 */
@Composable
internal fun StatePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = maxHeight * 0.30f, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

/**
 * 加载骨架：首屏超过 300ms 才出现，避免一闪而过。8.6
 * 用静态灰条而不是闪烁动画，符合「不做无限装饰动画」的约定。
 */
@Composable
internal fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 6,
    delayMillis: Long = 300
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visible = true
    }
    if (!visible) return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(ZippestRadius.sm)
                    )
            )
        }
    }
}

/**
 * 错误模板：人话 + 原因 + 两个出口，长异常折叠进「详情」。8.6
 */
@Composable
internal fun ErrorTemplate(
    message: String,
    onRetry: () -> Unit,
    secondaryLabel: String?,
    onSecondary: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var detailOpen by remember { mutableStateOf(false) }
    StatePanel(modifier) {
        EmptyIllustration(EmptyArt.TASKS)
        Text(
            "这一步没走通",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (detailOpen) 12 else 2,
            overflow = TextOverflow.Ellipsis
        )
        if (message.length > 60) {
            Text(
                if (detailOpen) "收起详情" else "查看详情",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { detailOpen = !detailOpen }
            )
        }
        androidx.compose.material3.OutlinedButton(onClick = onRetry) {
            Text("重试")
        }
        if (secondaryLabel != null && onSecondary != null) {
            androidx.compose.material3.TextButton(onClick = onSecondary) {
                Text(secondaryLabel)
            }
        }
    }
}

/** 尊重系统「减少动态效果」：开启时列表增删直接切换，不做过渡。8.5 */
@Composable
internal fun LazyItemScope.animateItemRespectingMotion(): Modifier {
    val reduceMotion = rememberReduceMotion()
    return if (reduceMotion) Modifier else Modifier.animateItem()
}

/**
 * 空态模板：插画 + 一句解释 + 一个主操作。8.6
 * 文件页、回收站、任务中心、空压缩包都走这一个，不再各写各的。
 */
@Composable
internal fun EmptyStateView(
    art: EmptyArt,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StatePanel(modifier) {
        EmptyIllustration(art)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
