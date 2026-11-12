package com.decompress.app.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.decompress.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.data.ArchiveItem
import com.decompress.app.data.BrowserEntry
import com.decompress.app.ui.theme.ZippestSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 宽屏（≥840 dp）第三栏：选中项的预览。
 * 没有选中项时退回当前文件夹信息，保持原来的行为。
 */
@Composable
internal fun BrowserPreviewPane(
    entry: BrowserEntry?,
    folderName: String,
    path: List<String>,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (entry == null) {
        BrowserInfoPane(folderName, path, itemCount, modifier)
        return
    }
    val context = LocalContext.current
    InfoPaneScaffold(title = UiStrings.get(R.string.preview_title), modifier = modifier) {
        if (entry.isImage || entry.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ZippestSpacing.xs),
                contentAlignment = Alignment.Center
            ) {
                MediaThumbnail(entry, size = 160.dp)
            }
        }
        InfoLine(UiStrings.get(R.string.preview_name), entry.name)
        InfoLine(UiStrings.get(R.string.preview_size), if (entry.isDirectory) "—" else Formatter.formatShortFileSize(context, entry.size))
        InfoLine(UiStrings.get(R.string.preview_time), modifiedLabel(entry))
        InfoLine(UiStrings.get(R.string.preview_type), typeLabel(entry))
        if (!entry.isDirectory && !entry.isImage && !entry.isVideo) {
            TextPreview(entry)
        }
    }
}

/** 宽屏第三栏：压缩包内选中条目的信息。 */
@Composable
internal fun ArchiveEntryPane(
    archiveName: String,
    sizeText: String,
    entryCount: Int,
    encodingLabel: String,
    entry: ArchiveItem?,
    modifier: Modifier = Modifier
) {
    if (entry == null) {
        ArchiveInfoPane(archiveName, sizeText, entryCount, encodingLabel, modifier)
        return
    }
    val context = LocalContext.current
    InfoPaneScaffold(title = UiStrings.get(R.string.preview_selected), modifier = modifier) {
        InfoLine(UiStrings.get(R.string.preview_name), entry.path.substringAfterLast('/'))
        InfoLine(
            UiStrings.get(R.string.preview_location),
            entry.path.substringBeforeLast('/', "/").let { if (it == "/") UiStrings.get(R.string.preview_root) else it }
        )
        InfoLine(UiStrings.get(R.string.preview_size), if (entry.isDirectory) "—" else Formatter.formatShortFileSize(context, entry.size))
        InfoLine(UiStrings.get(R.string.preview_type), if (entry.isDirectory) UiStrings.get(R.string.preview_folder) else extensionLabel(entry.path))
    }
}

private fun modifiedLabel(entry: BrowserEntry): String =
    if (entry.lastModified > 0L) {
        DateUtils.getRelativeTimeSpanString(
            entry.lastModified,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else {
        "—"
    }

private fun typeLabel(entry: BrowserEntry): String = when {
    entry.isDirectory -> UiStrings.get(R.string.preview_folder)
    entry.isArchive -> UiStrings.get(R.string.preview_archive_ext, entry.extension.uppercase())
    entry.extension.isNotBlank() -> entry.extension.uppercase()
    entry.mimeType?.isNotBlank() == true -> entry.mimeType.substringAfter('/').uppercase()
    else -> UiStrings.get(R.string.preview_file)
}

private fun extensionLabel(path: String): String {
    val extension = path.substringAfterLast('.', "")
    return if (extension.isBlank()) UiStrings.get(R.string.preview_file) else extension.uppercase()
}

private val TEXT_EXTENSIONS = setOf(
    "txt", "md", "json", "xml", "log", "csv", "ini", "conf", "properties",
    "kt", "java", "js", "ts", "html", "css", "yml", "yaml", "sh", "bat", "gitignore"
)

@Composable
private fun TextPreview(entry: BrowserEntry) {
    if (!entry.looksLikeText() || entry.size > 2L * 1024 * 1024) return
    val context = LocalContext.current
    val text by produceState<String?>(initialValue = null, entry.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(entry.uri)?.use { input ->
                    val buffer = ByteArray(2048)
                    val read = input.read(buffer)
                    if (read <= 0) return@use null
                    val slice = buffer.copyOf(read)
                    if (slice.any { it == 0.toByte() }) return@use null
                    String(slice, Charsets.UTF_8).trim()
                }
            }.getOrNull()
        }
    }
    val preview = text
    if (preview.isNullOrBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ZippestSpacing.xs)
    ) {
        Text(
            UiStrings.get(R.string.preview_head),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            preview,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 40,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun BrowserEntry.looksLikeText(): Boolean =
    mimeType?.startsWith("text/") == true || extension in TEXT_EXTENSIONS
