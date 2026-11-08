package com.decompress.app.ui

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.decompress.app.IncomingArchive
import com.decompress.app.OpenLocationRequest
import com.decompress.app.MainActivity
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.data.BrowserSortMode
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.FileBrowserController
import com.decompress.app.data.FileNameEncoding
import com.decompress.app.data.GlobalSearchHit
import com.decompress.app.data.QuickLocation
import com.decompress.app.data.QuickLocationIcon
import com.decompress.app.data.QuickLocationRepository
import com.decompress.app.data.ThemeMode
import com.decompress.app.data.TrashRepository
import com.decompress.app.service.TaskNavigation
import com.decompress.app.data.TaskHistoryCodec
import com.decompress.app.data.TaskHistoryEntry
import com.decompress.app.data.TaskHistoryStore
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.service.ExtractionTaskRegistry
import com.decompress.app.service.CompressionScheduler
import com.decompress.app.service.ArchiveTaskControl
import com.decompress.app.ui.theme.ZippestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


internal fun batchNameFor(entry: BrowserEntry, index: Int, pattern: String): String {
    val name = entry.name
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0) name.substring(0, dot) else name
    val extension = if (dot > 0) name.substring(dot) else ""
    val extOnly = extension.removePrefix(".")
    val now = java.util.Date(System.currentTimeMillis())
    val locale = java.util.Locale.US
    var usedExtensionToken = false
    val result = Regex("\\{(name|ext|n|num|date|time|\\d+)\\}").replace(pattern) { match ->
        when (val token = match.groupValues[1]) {
            "name" -> base
            "ext" -> {
                usedExtensionToken = true
                extOnly
            }
            "n", "num" -> index.toString()
            "date" -> java.text.SimpleDateFormat("yyyyMMdd", locale).format(now)
            "time" -> java.text.SimpleDateFormat("HHmmss", locale).format(now)
            else -> "%0${token.length}d".format(index)
        }
    }
    return if (usedExtensionToken || extension.isEmpty()) result else result + extension
}

@Composable
internal fun EmptyState(
    modifier: Modifier,
    filtered: Boolean,
    onBrowseDownloads: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (filtered) Icons.Outlined.Search else Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (filtered) "没有匹配的文件" else "此文件夹为空",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (filtered) "换个关键词试试" else "通过顶部的新建按钮创建文件夹",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!filtered && onBrowseDownloads != null) {
            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.OutlinedButton(onClick = onBrowseDownloads) {
                Text("去下载目录看看")
            }
        }
    }
}

@Composable
internal fun ErrorState(
    modifier: Modifier,
    message: String,
    onRetry: () -> Unit,
    onChooseFolder: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("重试") }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onChooseFolder) { Text("重新选择文件夹") }
    }
}

internal fun fileNameWithSuffixSelected(name: String): TextFieldValue {
    val dot = name.lastIndexOf('.')
    val start = if (dot >= 0) dot else name.length
    return TextFieldValue(
        text = name,
        selection = TextRange(start, name.length)
    )
}

internal fun selectedFileSuffix(value: TextFieldValue): String {
    val range = effectiveSuffixRange(value)
    return value.text.substring(range.start, range.end)
}

internal fun replaceSelectedFileSuffix(
    value: TextFieldValue,
    suffix: String
): TextFieldValue {
    val cleanSuffix = sanitizeFileSuffix(suffix)
    if (cleanSuffix.isEmpty()) return value
    val range = effectiveSuffixRange(value)
    val newText = value.text.replaceRange(range.start, range.end, cleanSuffix)
    return TextFieldValue(
        text = newText,
        selection = TextRange(range.start, range.start + cleanSuffix.length)
    )
}

private fun effectiveSuffixRange(value: TextFieldValue): TextRange {
    if (!value.selection.collapsed) return value.selection
    val dot = value.text.lastIndexOf('.')
    val start = if (dot >= 0) dot else value.text.length
    return TextRange(start, value.text.length)
}

internal fun sanitizeFileSuffix(value: String): String =
    value.trim()
        .replace(Regex("""[\\/:*?"<>|\u0000-\u001F\s]"""), "")
        .trimEnd('.')
        .let { clean ->
            val withoutLeadingDots = clean.trimStart('.')
            if (withoutLeadingDots.isEmpty()) "" else ".$withoutLeadingDots"
        }

@Suppress("DEPRECATION")
internal fun readStorageStats(): Pair<Long, Long> {
    return runCatching {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val available = stat.availableBlocksLong * stat.blockSizeLong
        (total - available) to total
    }.getOrDefault(0L to 0L)
}

internal fun describeIncomingFile(
    context: Context,
    uri: Uri,
    suppliedDisplayName: String?
): BrowserEntry {
    var displayName: String? = suppliedDisplayName
    var size = 0L
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.getString(0)
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
    }
    return BrowserEntry(
        uri = uri,
        name = displayName ?: uri.lastPathSegment ?: "已打开的压缩包",
        isDirectory = false,
        size = size,
        lastModified = 0L,
        mimeType = context.contentResolver.getType(uri)
    )
}

internal fun shareEntries(context: Context, entries: List<BrowserEntry>): Boolean {
    if (entries.isEmpty()) return false
    return runCatching {
        val uris = ArrayList<Uri>(entries.size)
        for (entry in entries) {
            val shareUri = if (entry.uri.scheme == ContentResolver.SCHEME_FILE) {
                val path = entry.uri.path ?: error("本地文件路径无效")
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(path)
                )
            } else {
                entry.uri
            }
            uris += shareUri
        }
        val mimeType = entries.singleOrNull()?.mimeType
            ?: entries.mapNotNull(BrowserEntry::mimeType).distinct().singleOrNull()
            ?: "*/*"
        val intent = if (entries.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(
                "解压文件",
                uris.first()
            ).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        }
        context.startActivity(Intent.createChooser(intent, "分享所选文件"))
    }.isSuccess
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
