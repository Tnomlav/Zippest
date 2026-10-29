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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Delete
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

@Composable
fun TrashSheet(
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TrashRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var items by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshVersion) {
        loading = true
        items = repository.list()
        loading = false
    }

    fun refresh(message: String) {
        refreshVersion++
        onChanged(message)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
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
                        "${items.size} 项",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (items.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val count = repository.empty()
                                refresh("已永久删除 $count 项")
                            }
                        }
                    ) {
                        Text("清空", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    when {
                        loading -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        items.isEmpty() -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
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
                        }
                        else -> LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                        ) {
                            items(items, key = { it.id }) { item ->
                                TrashItemRow(
                                    item = item,
                                    onRestore = {
                                        scope.launch {
                                            val restored = repository.restore(item.id)
                                            refresh(
                                                if (restored) "已恢复 ${item.name}" else "恢复失败"
                                            )
                                        }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            val deleted = repository.deleteForever(item.id)
                                            refresh(
                                                if (deleted) {
                                                    "已永久删除 ${item.name}"
                                                } else {
                                                    "删除失败"
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text("关闭")
            }
        }
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
                    Formatter.formatShortFileSize(context, item.size),
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
