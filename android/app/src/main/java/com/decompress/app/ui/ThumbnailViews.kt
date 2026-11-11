package com.decompress.app.ui

import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.decompress.app.data.BrowserEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun EntryLeading(entry: BrowserEntry, showThumbnail: Boolean) {
    if (showThumbnail && (entry.isImage || entry.isVideo)) {
        MediaThumbnail(entry)
        return
    }

    val tint = when {
        entry.isDirectory -> MaterialTheme.colorScheme.primary
        entry.isArchive -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }
    val icon = when {
        entry.isDirectory -> Icons.Outlined.Folder
        entry.isArchive -> Icons.Outlined.Archive
        else -> Icons.Outlined.InsertDriveFile
    }

    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
    }
}

private object ThumbnailCache {
    private val cache = object : android.util.LruCache<String, android.graphics.Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount
    }

    fun get(key: String): android.graphics.Bitmap? = cache.get(key)

    fun put(key: String, bitmap: android.graphics.Bitmap) {
        cache.put(key, bitmap)
    }
}

@Composable
internal fun MediaThumbnail(entry: BrowserEntry, size: Dp = 44.dp) {
    val context = LocalContext.current
    // 列表行用 96 px，宽屏预览栏用 320 px；缓存键带上尺寸，避免互相顶掉。
    val thumbPx = if (size.value >= 100f) 320 else 96
    val corner = if (size.value >= 100f) 16.dp else 12.dp
    val cacheKey = entry.uri.toString() + "@" + thumbPx
    var bitmap by remember(entry.uri, thumbPx) {
        mutableStateOf(ThumbnailCache.get(cacheKey)?.asImageBitmap())
    }

    LaunchedEffect(entry.uri, thumbPx) {
        if (bitmap != null) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    if (entry.uri.scheme == "file") {
                        val file = File(entry.uri.path ?: return@runCatching null)
                        if (entry.isVideo) {
                            ThumbnailUtils.createVideoThumbnail(
                                file,
                                Size(thumbPx, thumbPx),
                                null
                            )
                        } else {
                            ThumbnailUtils.createImageThumbnail(
                                file,
                                Size(thumbPx, thumbPx),
                                null
                            )
                        }
                    } else {
                        context.contentResolver.loadThumbnail(
                            entry.uri,
                            Size(thumbPx, thumbPx),
                            null
                        )
                    }
                }.getOrNull()
            }
            if (loaded != null) {
                ThumbnailCache.put(cacheKey, loaded)
                bitmap = loaded.asImageBitmap()
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (entry.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.58f)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(2.dp)
                    )
                }
            }
        } else {
            Icon(
                if (entry.isVideo) Icons.Outlined.PlayArrow else Icons.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
