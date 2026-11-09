package com.decompress.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.decompress.app.ui.theme.ZippestRadius
import com.decompress.app.ui.theme.ZippestSpacing

internal const val ZIPPEST_ISSUES_URL = "https://github.com/Tnomlav/Zippest/issues/new"

/**
 * 反馈用的预填文本：版本、机型、系统先填好，用户只需补“做了什么 / 发生了什么”。
 * 不含文件名、路径或任何用户内容——那部分要用户自己决定是否贴。
 */
internal fun buildFeedbackText(
    context: Context,
    versionName: String,
    versionCode: Long
): String {
    val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    return buildString {
        appendLine("版本：$versionName ($versionCode)")
        appendLine("机型：$model")
        appendLine("系统：Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）")
        appendLine()
        appendLine("做了什么：（解压 / 压缩 / 浏览；格式；是否加密或分卷；文件多大）")
        appendLine("发生了什么：")
        appendLine("期望怎样：")
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Zippest 反馈", text))
}

@Composable
internal fun FeedbackDialog(
    versionName: String,
    versionCode: Long,
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val text = remember(versionName, versionCode) {
        buildFeedbackText(context, versionName, versionCode)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("反馈问题") },
        text = {
            Column {
                Text(
                    "版本、机型和系统已经填好。复制后贴到 GitHub Issue，再补两句“做了什么、发生了什么”就够了。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier
                        .padding(top = ZippestSpacing.md)
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(ZippestRadius.sm)
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(ZippestSpacing.md)
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    copyToClipboard(context, text)
                    onOpenLink(ZIPPEST_ISSUES_URL)
                    onDismiss()
                }
            ) {
                Text("复制并打开 GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
