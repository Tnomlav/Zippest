package com.decompress.app.ui

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.decompress.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.decompress.app.data.QuickLocation
import com.decompress.app.data.QuickLocationIcon
import java.io.File
import com.decompress.app.ui.theme.ZippestRadius

@Composable
internal fun QuickAccessTopTitle(
    directStorage: Boolean,
    folderName: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(enabled = directStorage, onClick = onClick)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (directStorage) UiStrings.get(R.string.qa_title) else folderName.ifBlank { UiStrings.get(R.string.qa_files) },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (directStorage) {
            Spacer(Modifier.size(4.dp))
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) {
                    UiStrings.get(R.string.qa_collapse)
                } else {
                    UiStrings.get(R.string.qa_expand)
                },
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun StorageQuickLocationPanel(
    topInset: Dp,
    currentUri: Uri?,
    locations: List<QuickLocation>,
    externalStorages: List<ExternalStorageLocation>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (QuickLocation) -> Unit,
    onOpenExternal: (ExternalStorageLocation) -> Unit
) {
    val root = Environment.getExternalStorageDirectory()

    fun isCurrent(location: QuickLocation): Boolean {
        val expected = File(root, location.path)
        return currentUri == Uri.fromFile(expected)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topInset)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onDismiss)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = ZippestRadius.xl, bottomEnd = ZippestRadius.xl),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    top = 12.dp,
                    end = 12.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (locations.isEmpty()) {
                    Text(
                        UiStrings.get(R.string.qa_empty),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                locations.chunked(2).forEach { rowLocations ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLocations.forEach { location ->
                            StorageQuickLocation(
                                location = location,
                                selected = isCurrent(location),
                                modifier = Modifier.weight(1f),
                                onClick = { onOpen(location) }
                            )
                        }
                        if (rowLocations.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                externalStorages.forEach { storage ->
                    StorageExternalQuickLocation(
                        storage = storage,
                        selected = currentUri == Uri.fromFile(File(storage.path)),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenExternal(storage) }
                    )
                }
                StorageQuickLocationAdd(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAdd
                )
            }
        }
    }
}

@Composable
internal fun StorageExternalQuickLocation(
    storage: ExternalStorageLocation,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(ZippestRadius.sm),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (selected) 0.9f else 0.55f
        ),
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Storage,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    storage.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    storage.path,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
            }
        }
    }
}

@Composable
internal fun StorageQuickLocationAdd(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(ZippestRadius.sm),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                UiStrings.get(R.string.qa_add),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun StorageQuickLocation(
    location: QuickLocation,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val icon = when (location.icon) {
        QuickLocationIcon.HOME -> Icons.Outlined.Home
        QuickLocationIcon.DOWNLOAD -> Icons.Outlined.Download
        QuickLocationIcon.PICTURES -> Icons.Outlined.PhotoLibrary
        QuickLocationIcon.CAMERA -> Icons.Outlined.PhotoCamera
        QuickLocationIcon.MOVIES -> Icons.Outlined.Movie
        QuickLocationIcon.MUSIC -> Icons.Outlined.MusicNote
        QuickLocationIcon.DOCUMENTS -> Icons.Outlined.Description
        QuickLocationIcon.FOLDER -> Icons.Outlined.Folder
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(ZippestRadius.sm),
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(20.dp)
            )
            Text(
                location.displayLabel(),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun discoverExternalStorageLocations(
    context: Context
): List<ExternalStorageLocation> {
    val internalRoot = runCatching {
        Environment.getExternalStorageDirectory().canonicalPath
    }.getOrDefault(Environment.getExternalStorageDirectory().absolutePath)
    val roots = linkedMapOf<String, String>()
    val usedLabels = mutableSetOf<String>()

    fun addRoot(path: String?, label: String) {
        val directory = path?.let(::File) ?: return
        if (!directory.isDirectory) return
        val canonical = runCatching { directory.canonicalPath }
            .getOrDefault(directory.absolutePath)
        if (canonical == internalRoot || roots.containsKey(canonical)) return
        val baseLabel = label.ifBlank { UiStrings.get(R.string.qa_external) }
        var uniqueLabel = baseLabel
        var index = 2
        while (!usedLabels.add(uniqueLabel)) {
            uniqueLabel = "$baseLabel $index"
            index++
        }
        roots[canonical] = uniqueLabel
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val storageManager = context.getSystemService(StorageManager::class.java)
        storageManager?.storageVolumes.orEmpty()
            .filter { volume ->
                !volume.isPrimary &&
                    (
                        volume.state == Environment.MEDIA_MOUNTED ||
                            volume.state == Environment.MEDIA_MOUNTED_READ_ONLY
                        )
            }
            .forEach { volume ->
                addRoot(
                    volume.directory?.absolutePath,
                    volume.getDescription(context)
                )
            }
    }

    context.getExternalFilesDirs(null).forEachIndexed { index, appDirectory ->
        val appPath = appDirectory?.absolutePath ?: return@forEachIndexed
        val androidMarker = appPath.indexOf("/Android/")
        if (androidMarker <= 0) return@forEachIndexed
        addRoot(
            appPath.substring(0, androidMarker),
            if (index == 0) UiStrings.get(R.string.qa_external)
            else UiStrings.get(R.string.qa_external_index, index)
        )
    }
    return roots.entries
        .map { (path, label) -> ExternalStorageLocation(label, path) }
        .sortedBy { it.label.lowercase() }
}

@Composable
internal fun StorageBar(
    context: Context,
    hasFullAccess: Boolean,
    onSwipeUpHome: () -> Unit
) {
    val (used, total) = remember { readStorageStats() }
    val fraction = if (total > 0L) used.toFloat() / total.toFloat() else 0f

    val swipeThreshold = with(LocalDensity.current) { 40.dp.toPx() }
    var swipeDistance = 0f
    Surface(
        modifier = Modifier.pointerInput(onSwipeUpHome) {
            detectVerticalDragGestures(
                onDragStart = { swipeDistance = 0f },
                onDragEnd = {
                    if (swipeDistance < -swipeThreshold) {
                        onSwipeUpHome()
                    }
                },
                onVerticalDrag = { change, amount ->
                    swipeDistance += amount
                    if (swipeDistance < -12f) change.consume()
                }
            )
        },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // 竖排而不是左右 SpaceBetween：200% 字号时两个长句会互相挤压，
            // 右侧状态会被压成每行一个字母。
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    UiStrings.get(
                        R.string.qa_storage,
                        Formatter.formatShortFileSize(context, used),
                        Formatter.formatShortFileSize(context, total)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(ZippestRadius.pill)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

