package com.decompress.app.ui

import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.decompress.app.data.QuickLocation
import com.decompress.app.data.QuickLocationRepository
import com.decompress.app.data.normalizeQuickLocationPath
import com.decompress.app.data.relativePathFromInternalTreeDocumentId
import java.io.File

@Composable
internal fun QuickLocationEditorDialog(
    repository: QuickLocationRepository,
    location: QuickLocation?,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var label by remember(location?.id) { mutableStateOf(location?.label.orEmpty()) }
    var path by remember(location?.id) { mutableStateOf(location?.path.orEmpty()) }
    var hasSelectedPath by remember(location?.id) {
        mutableStateOf(location != null)
    }
    var error by remember(location?.id) { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val documentId = runCatching {
            DocumentsContract.getTreeDocumentId(uri)
        }.getOrNull()
        val relativePath = documentId?.let(::relativePathFromInternalTreeDocumentId)
        if (relativePath == null) {
            error = "快捷位置必须位于内部存储中"
            return@rememberLauncherForActivityResult
        }
        val resolvedPath = resolveInternalDirectoryPath(relativePath)
        path = resolvedPath
        hasSelectedPath = true
        if (label.isBlank()) {
            label = resolvedPath.substringAfterLast('/').ifBlank { "快捷位置" }
        }
    }

    fun save() {
        val cleanLabel = label.trim()
        val normalizedPath = normalizeQuickLocationPath(path)
        if (cleanLabel.isEmpty()) {
            error = "请输入快捷位置名称"
            return
        }
        if (!hasSelectedPath) {
            error = "请先通过文件管理器选择文件夹"
            return
        }
        if (normalizedPath == null) {
            error = "目录地址不能包含 . 或 .."
            return
        }
        val target = File(
            Environment.getExternalStorageDirectory(),
            normalizedPath
        )
        if (!target.isDirectory) {
            error = "目录不存在或无法访问"
            return
        }
        if (location == null) {
            repository.add(cleanLabel, normalizedPath)
            onSaved("已添加快捷位置")
        } else {
            repository.update(location.copy(label = cleanLabel, path = normalizedPath))
            onSaved("已更新快捷位置")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (location == null) "新建快捷位置" else "编辑快捷位置")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("名称") }
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Text(
                        if (!hasSelectedPath) {
                            "尚未选择文件夹"
                        } else if (path.isBlank()) {
                            "内部存储"
                        } else {
                            "内部存储 / $path"
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        error = null
                        folderPicker.launch(null)
                    }) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (hasSelectedPath) "重新选择文件夹" else "选择文件夹",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = ::save) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun resolveInternalDirectoryPath(relativePath: String): String {
    val normalized = normalizeQuickLocationPath(relativePath).orEmpty()
    val root = Environment.getExternalStorageDirectory()
    if (normalized.isBlank() || File(root, normalized).isDirectory) {
        return normalized
    }
    val segments = normalized.split('/').toMutableList()
    val localizedAliases = mapOf(
        "下载" to "Download",
        "图片" to "Pictures",
        "相册" to "Pictures",
        "相机" to "DCIM",
        "视频" to "Movies",
        "电影" to "Movies",
        "音乐" to "Music",
        "文档" to "Documents"
    )
    segments.firstOrNull()?.let { first ->
        localizedAliases[first]?.let { segments[0] = it }
    }
    return segments.joinToString("/")
}
