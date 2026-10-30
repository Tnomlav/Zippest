package com.decompress.app.ui

import android.content.ComponentName
import android.content.ClipData
import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import android.webkit.MimeTypeMap
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
import androidx.core.content.FileProvider
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.OpenWithPreferences
import com.decompress.app.data.localFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class OpenWithOption(
    val label: String,
    val component: ComponentName,
    val icon: ImageBitmap?,
    val category: OpenWithCategory,
    val action: String
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

internal suspend fun createOpenWithIntent(
    context: Context,
    entry: BrowserEntry
): Intent? = withContext(Dispatchers.IO) {
    val extension = entry.name.substringAfterLast('.', "").lowercase()
    val mimeType = entry.mimeType
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: "*/*"
    val localFile = entry.uri.localFile()
    val viewUri = localFile?.let { mediaStoreUri(context, it, mimeType) }
        ?: if (entry.uri.scheme == "file") {
            val file = localFile ?: return@withContext null
            fileProviderUri(context, file)
        } else {
            entry.uri
        }
    val clipData = ClipData.newRawUri(entry.name, viewUri)
    if (localFile != null) {
        siblingMediaUris(context, localFile, mimeType).forEach { sibling ->
            if (sibling != viewUri) {
                clipData.addItem(ClipData.Item(sibling))
            }
        }
    }
    Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(viewUri, mimeType)
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        this.clipData = clipData
    }
}

private fun fileProviderUri(context: Context, file: File) =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

private fun siblingMediaUris(
    context: Context,
    selectedFile: File,
    selectedMimeType: String
): List<Uri> {
    val parent = selectedFile.parentFile ?: return emptyList()
    val family = selectedMimeType.substringBefore('/')
    val selectedExtension = selectedFile.extension.lowercase()
    return parent.listFiles()
        .orEmpty()
        .asSequence()
        .filter { it.isFile && it.absolutePath != selectedFile.absolutePath }
        .filter { candidate ->
            val candidateMime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(candidate.extension.lowercase())
                ?: ""
            if (family in setOf("image", "video", "audio")) {
                candidateMime.startsWith("$family/")
            } else {
                candidate.extension.equals(selectedExtension, ignoreCase = true)
            }
        }
        .sortedBy { it.name.lowercase() }
        .take(300)
        .mapNotNull { candidate ->
            mediaStoreUri(context, candidate, selectedMimeTypeFor(candidate))
                ?: runCatching { fileProviderUri(context, candidate) }.getOrNull()
        }
        .toList()
}

private fun selectedMimeTypeFor(file: File): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
        ?: "*/*"

private fun mediaStoreUri(context: Context, file: File, mimeType: String): Uri? {
    val collection = when {
        mimeType.startsWith("image/") -> mediaCollection("image")
        mimeType.startsWith("video/") -> mediaCollection("video")
        mimeType.startsWith("audio/") -> mediaCollection("audio")
        else -> null
    } ?: return null
    val projection = arrayOf(MediaStore.MediaColumns._ID)
    val directMatch = runCatching<Uri?> {
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.DATA}=?",
            arrayOf(file.absolutePath),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return@runCatching ContentUris.withAppendedId(
                    collection,
                    cursor.getLong(0)
                )
            }
            null
        }
    }.getOrNull()
    if (directMatch != null) return directMatch

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val root = storageRootFor(context, file) ?: return null
    val relativePath = runCatching {
        file.parentFile
            ?.relativeTo(root)
            ?.invariantSeparatorsPath
            ?.trim('/')
            ?.let { if (it.isEmpty()) "" else "$it/" }
    }.getOrNull() ?: return null
    return runCatching {
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(file.name, relativePath),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                ContentUris.withAppendedId(collection, cursor.getLong(0))
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun mediaCollection(type: String): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        when (type) {
            "image" -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            "video" -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
    } else {
        when (type) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

private fun storageRootFor(context: Context, file: File): File? {
    val candidates = buildList {
        add(Environment.getExternalStorageDirectory())
        context.getExternalFilesDirs(null).forEach { appDirectory ->
            val path = appDirectory?.absolutePath ?: return@forEach
            add(File(path.substringBefore("/Android/", path)))
        }
    }
    val filePath = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
    return candidates
        .distinctBy { it.absolutePath }
        .filter { candidate ->
            filePath == candidate.absolutePath ||
                filePath.startsWith(candidate.absolutePath + File.separator)
        }
        .maxByOrNull { it.absolutePath.length }
}

internal fun queryOpenWithOptions(context: Context, intent: Intent): List<OpenWithOption> {
    val packageManager = context.packageManager
    val candidates = linkedMapOf<ComponentName, Pair<String, ResolveInfo>>()
    listOf(Intent.ACTION_VIEW, Intent.ACTION_EDIT).forEach { action ->
        val actionIntent = Intent(intent).setAction(action)
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                actionIntent,
                PackageManager.ResolveInfoFlags.of(
                    PackageManager.MATCH_DEFAULT_ONLY.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(
                actionIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        }
        infos.forEach { info ->
            val activityInfo = info.activityInfo
            if (activityInfo.packageName == context.packageName) return@forEach
            val component = ComponentName(activityInfo.packageName, activityInfo.name)
            candidates.putIfAbsent(component, action to info)
        }
    }
    val options = candidates
        .asSequence()
        .map { (component, actionAndInfo) ->
            val (action, resolveInfo) = actionAndInfo
            val activityInfo = resolveInfo.activityInfo
            val label = resolveInfo.loadLabel(packageManager).toString()
            val category = openWithCategory(
                label,
                component.packageName,
                component.className
            )
            OpenWithOption(
                label = if (action == Intent.ACTION_EDIT) "$label（编辑）" else label,
                component = component,
                icon = runCatching {
                    resolveInfo.loadIcon(packageManager)
                        .toBitmap(64, 64)
                        .asImageBitmap()
                }.getOrNull(),
                category = category,
                action = action
            )
        }
        .toList()
    val mimeType = intent.type.orEmpty()
    return options.sortedWith(
        compareBy<OpenWithOption> { it.category.ordinal }
            .thenByDescending { directPriority(mimeType, it) }
            .thenBy { it.label.lowercase() }
    )
}

internal fun openWithCategory(
    label: String,
    packageName: String,
    className: String
): OpenWithCategory {
    val searchable = buildString {
        append(label)
        append(' ')
        append(packageName)
        append(' ')
        append(className)
    }.lowercase()
    return if (listOf(
        "分享", "发送", "朋友圈", "好友", "聊天", "传输",
        "share", "send", "moment", "friend", "message", "chat", "transfer"
    ).any(searchable::contains)) {
        OpenWithCategory.SHARE
    } else {
        OpenWithCategory.DIRECT
    }
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
    context.startActivity(
        Intent(request.intent)
            .setAction(option.action)
            .setComponent(option.component)
    )
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
