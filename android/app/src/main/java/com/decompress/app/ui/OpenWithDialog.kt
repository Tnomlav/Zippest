package com.decompress.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.OpenWithPreferences
import com.decompress.app.data.localFile
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap
import java.io.File

internal data class OpenWithOption(
    val label: String,
    val component: ComponentName,
    val icon: ImageBitmap?,
    val category: OpenWithCategory
)

internal enum class OpenWithCategory {
    DIRECT,
    SHARE
}

internal data class OpenWithRequest(
    val displayName: String,
    val mimeType: String,
    val extension: String,
    val intent: Intent,
    val options: List<OpenWithOption>
)

internal fun createOpenWithIntent(context: Context, entry: BrowserEntry): Intent? {
    val viewUri = if (entry.uri.scheme == "file") {
        val file = entry.uri.localFile() ?: return null
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(file.absolutePath)
        )
    } else {
        entry.uri
    }
    val extension = entry.name.substringAfterLast('.', "").lowercase()
    val mimeType = entry.mimeType
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: "*/*"
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(viewUri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri(entry.name, viewUri)
    }
}

internal fun queryOpenWithOptions(context: Context, intent: Intent): List<OpenWithOption> {
    val packageManager = context.packageManager
    val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    val options = infos
        .asSequence()
        .filter { it.activityInfo.packageName != context.packageName }
        .map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val label = resolveInfo.loadLabel(packageManager).toString()
            val component = ComponentName(activityInfo.packageName, activityInfo.name)
            OpenWithOption(
                label = label,
                component = component,
                icon = runCatching {
                    resolveInfo.loadIcon(packageManager)
                        .toBitmap(64, 64)
                        .asImageBitmap()
                }.getOrNull(),
                category = if (isShareLike(label, component)) {
                    OpenWithCategory.SHARE
                } else {
                    OpenWithCategory.DIRECT
                }
            )
        }
        .distinctBy { it.component.flattenToString() }
        .toList()
    val mimeType = intent.type.orEmpty()
    return options.sortedWith(
        compareBy<OpenWithOption> { it.category.ordinal }
            .thenByDescending { directPriority(mimeType, it) }
            .thenBy { it.label.lowercase() }
    )
}

private fun isShareLike(label: String, component: ComponentName): Boolean {
    val searchable = buildString {
        append(label)
        append(' ')
        append(component.packageName)
        append(' ')
        append(component.className)
    }.lowercase()
    return listOf(
        "分享", "发送", "朋友圈", "好友", "聊天", "传输",
        "share", "send", "moment", "friend", "message", "chat", "transfer"
    ).any(searchable::contains)
}

private fun directPriority(mimeType: String, option: OpenWithOption): Int {
    val searchable = buildString {
        append(option.label)
        append(' ')
        append(option.component.packageName)
        append(' ')
        append(option.component.className)
    }.lowercase()
    val keywords = when {
        mimeType.startsWith("image/") -> listOf(
            "相册", "图库", "照片", "图片", "gallery", "photo", "image", "viewer"
        )
        mimeType.startsWith("video/") -> listOf(
            "视频", "播放器", "影音", "video", "player", "movie"
        )
        mimeType.startsWith("audio/") -> listOf(
            "音乐", "播放器", "音频", "music", "audio", "player"
        )
        mimeType == "application/pdf" -> listOf(
            "pdf", "阅读器", "查看器", "wps", "office", "reader"
        )
        mimeType.startsWith("text/") -> listOf(
            "文本", "笔记", "编辑器", "text", "editor", "notes", "reader"
        )
        mimeType == "application/vnd.android.package-archive" -> listOf(
            "安装", "installer", "packageinstaller"
        )
        else -> listOf(
            "文档", "阅读", "查看", "编辑", "office", "wps",
            "viewer", "reader", "editor", "document"
        )
    }
    return keywords.count(searchable::contains) * 100
}

internal fun launchRememberedOpenWith(
    context: Context,
    request: OpenWithRequest
): Boolean {
    val saved = OpenWithPreferences.get(
        context,
        request.mimeType,
        request.extension
    ) ?: return false
    val option = request.options.firstOrNull { it.component == saved }
    if (option == null) {
        OpenWithPreferences.clear(context)
        return false
    }
    return launchOpenWith(context, request, option, rememberChoice = false)
}

internal fun launchOpenWith(
    context: Context,
    request: OpenWithRequest,
    option: OpenWithOption,
    rememberChoice: Boolean
): Boolean = runCatching {
    if (rememberChoice) {
        OpenWithPreferences.set(
            context,
            request.mimeType,
            request.extension,
            option.component
        )
    }
    context.startActivity(Intent(request.intent).setComponent(option.component))
}.isSuccess

@Composable
internal fun OpenWithDialog(
    request: OpenWithRequest,
    onDismiss: () -> Unit,
    onLaunch: (OpenWithOption, Boolean) -> Unit
) {
    var selectedIndex by remember(request) { mutableIntStateOf(0) }
    var rememberChoice by remember(request) { mutableStateOf(false) }
    val selected = request.options.getOrNull(selectedIndex) ?: request.options.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("打开方式") },
        text = {
            Column {
                Text(
                    request.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    request.options.forEachIndexed { index, option ->
                        val previous = request.options.getOrNull(index - 1)
                        if (index == 0 || previous?.category != option.category) {
                            Text(
                                if (option.category == OpenWithCategory.DIRECT) {
                                    "直接打开"
                                } else {
                                    "分享或发送"
                                },
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    top = if (index == 0) 0.dp else 8.dp,
                                    bottom = 2.dp
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (option.icon != null) {
                                    Image(
                                        bitmap = option.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Spacer(Modifier.size(32.dp))
                                }
                                Text(
                                    option.label,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index == selectedIndex) {
                                        FontWeight.Medium
                                    } else {
                                        FontWeight.Normal
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                RadioButton(
                                    selected = index == selectedIndex,
                                    onClick = { selectedIndex = index }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !rememberChoice,
                        onClick = { rememberChoice = false },
                        label = { Text("仅一次") }
                    )
                    FilterChip(
                        selected = rememberChoice,
                        onClick = { rememberChoice = true },
                        label = { Text("总是") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onLaunch(selected, rememberChoice) }) {
                Text("打开")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
