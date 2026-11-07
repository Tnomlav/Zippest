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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.data.TrashItem
import com.decompress.app.data.TrashRepository
import kotlinx.coroutines.launch

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
                    "回收站",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (items.isEmpty()) {
                        "删除的文件会先放在这里"
                    } else {
                        val quotaLabel = if (settings.trashQuota.bytes <= 0L) {
                            "不限"
                        } else {
                            settings.trashQuota.label
                        }
                        "${items.size} 项 · 已用 " +
                            Formatter.formatShortFileSize(context, usedBytes) +
                            " / $quotaLabel"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (items.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(18.dp),
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
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "回收站为空",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "删除文件时默认先移到这里，保留期可在设置里调整",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                restoredName == null -> "恢复失败"
                                                policy == com.decompress.app.data
                                                    .RestoreConflictPolicy.OVERWRITE ->
                                                    "已恢复 ${item.name}（覆盖了同名文件）"
                                                restoredName == item.name ->
                                                    "已恢复 ${item.name}"
                                                else ->
                                                    "原位置有同名文件，已恢复为 $restoredName"
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
                Text("关闭")
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("清空回收站") },
            text = { Text("确定永久删除回收站中的 ${items.size} 项吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmEmpty = false
                        scope.launch {
                            val count = repository.empty()
                            refresh("已永久删除 $count 项")
                        }
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) {
                    Text("取消")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("永久删除") },
            text = { Text("确定永久删除“${target.name}”吗？此操作无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        scope.launch {
                            val deleted = repository.deleteForever(target.id)
                            refresh(
                                if (deleted) {
                                    "已永久删除 ${target.name}"
                                } else {
                                    "删除失败"
                                }
                            )
                        }
                    }
                ) {
                    Text("永久删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
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
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
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
                        "文件夹"
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
            Icon(Icons.Outlined.Restore, contentDescription = "恢复 ${item.name}")
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteForever,
                contentDescription = "永久删除 ${item.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
