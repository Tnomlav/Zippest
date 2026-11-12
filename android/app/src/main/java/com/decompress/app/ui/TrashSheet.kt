@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.decompress.app.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.decompress.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.data.TrashItem
import com.decompress.app.data.TrashRepository
import kotlinx.coroutines.launch
import com.decompress.app.ui.theme.ZippestRadius

/** 底部导航「回收站」页与弹层共用的一份内容。 */
@Composable
fun TrashSheet(
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        TrashContent(
            onChanged = onChanged,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            onClose = onDismiss
        )
    }
}

@Composable
fun TrashContent(
    onChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    showHeader: Boolean = true
) {
    val context = LocalContext.current
    val repository = remember { TrashRepository(context.applicationContext) }
    val settings = remember {
        com.decompress.app.data.AppSettingsRepository(context.applicationContext).settings
    }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var usedBytes by remember { mutableStateOf(0L) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TrashItem?>(null) }

    LaunchedEffect(refreshVersion) {
        loading = true
        items = repository.list()
        usedBytes = repository.usedBytes()
        loading = false
    }

    fun refresh(message: String) {
        refreshVersion++
        onChanged(message)
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
    ) {
        if (showHeader) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    UiStrings.get(R.string.trash_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (items.isEmpty()) {
                        UiStrings.get(R.string.trash_subtitle)
                    } else {
                        val quotaLabel = if (settings.trashQuota.bytes <= 0L) {
                            UiStrings.get(R.string.unlimited)
                        } else {
                            settings.trashQuota.label()
                        }
                        UiStrings.get(
                            R.string.trash_usage,
                            items.size,
                            Formatter.formatShortFileSize(context, usedBytes),
                            quotaLabel
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (items.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) {
                    Text(UiStrings.get(R.string.empty), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(ZippestRadius.md),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    items.isEmpty() -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyStateView(
                            art = EmptyArt.TRASH,
                            title = UiStrings.get(R.string.trash_empty),
                            description = UiStrings.get(R.string.trash_empty_hint),
                            actionLabel = if (onClose != null) UiStrings.get(R.string.close) else null,
                            onAction = onClose
                        )
                    }
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items, key = { it.id }) { item ->
                            TrashItemRow(
                                item = item,
                                onRestore = {
                                    scope.launch {
                                        val policy = settings.restoreConflictPolicy
                                        val restoredName = repository.restore(item.id, policy)
                                        refresh(
                                            when {
                                                restoredName == null -> UiStrings.get(R.string.restore_failed)
                                                policy == com.decompress.app.data
                                                    .RestoreConflictPolicy.OVERWRITE ->
                                                    UiStrings.get(R.string.restored_overwrite, item.name)
                                                restoredName == item.name ->
                                                    UiStrings.get(R.string.restored, item.name)
                                                else ->
                                                    UiStrings.get(R.string.restored_renamed, restoredName)
                                            }
                                        )
                                    }
                                },
                                onDelete = {
                                    deleteTarget = item
                                }
                            )
                        }
                    }
                }
            }
        }
        if (onClose != null) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(UiStrings.get(R.string.close))
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text(UiStrings.get(R.string.empty_trash)) },
            text = { Text(UiStrings.get(R.string.confirm_empty, items.size)) },
            confirmButton = {
                DialogDeleteButton(
                    onClick = {
                        confirmEmpty = false
                        scope.launch {
                            val count = repository.empty()
                            refresh(UiStrings.get(R.string.deleted_count, count))
                        }
                    },
                    text = UiStrings.get(R.string.empty)
                )
            },
            dismissButton = {
                DialogDismissButton(onClick = { confirmEmpty = false })
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(UiStrings.get(R.string.delete_forever)) },
            text = { Text(UiStrings.get(R.string.confirm_delete, target.name)) },
            confirmButton = {
                DialogDeleteButton(
                    onClick = {
                        deleteTarget = null
                        scope.launch {
                            val deleted = repository.deleteForever(target.id)
                            refresh(
                                if (deleted) {
                                    UiStrings.get(R.string.deleted_named, target.name)
                                } else {
                                    UiStrings.get(R.string.delete_failed)
                                }
                            )
                        }
                    },
                    text = UiStrings.get(R.string.delete_forever)
                )
            },
            dismissButton = {
                DialogDismissButton(onClick = { deleteTarget = null })
            }
        )
    }
}

@Composable
private fun TrashItemRow(
    item: TrashItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.originalPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            Text(
                listOf(
                    if (item.isDirectory) {
                        UiStrings.get(R.string.folder)
                    } else {
                        Formatter.formatShortFileSize(context, item.size)
                    },
                    DateUtils.getRelativeTimeSpanString(item.deletedAt).toString()
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Outlined.Restore, contentDescription = UiStrings.get(R.string.restore_named, item.name))
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteForever,
                contentDescription = UiStrings.get(R.string.delete_named, item.name),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
