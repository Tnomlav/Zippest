@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.decompress.app.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.FileBrowserController
import com.decompress.app.data.SafFileRepository
import com.decompress.app.data.localFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class TransferTarget(
    val sources: List<BrowserEntry>,
    val storageMode: FileBrowserController.StorageMode,
    val treeUri: Uri,
    val rootUri: Uri,
    val rootLabel: String,
    val move: Boolean
)

private data class PickerFolder(
    val uri: Uri,
    val name: String,
    val treeUri: Uri
)

private data class StorageRoot(
    val uri: Uri,
    val name: String,
    val treeUri: Uri
)

@Composable
fun TransferDestinationSheet(
    target: TransferTarget,
    onDismiss: () -> Unit,
    onChoose: (directoryUri: Uri, treeUri: Uri) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SafFileRepository(context.applicationContext) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val storageRoots = remember(target) {
        discoverStorageRoots(context, target)
    }
    val folders = remember(target) {
        mutableStateListOf(
            PickerFolder(target.rootUri, target.rootLabel, target.treeUri)
        )
    }
    var directories by remember { mutableStateOf<List<BrowserEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val current = folders.last()

    LaunchedEffect(current.uri) {
        loading = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) {
                val children = current.uri.localFile()?.let { folder ->
                    if (!folder.isDirectory) {
                        throw IllegalStateException("目标文件夹不可用")
                    }
                    repository.listDirectChildren(folder)
                } ?: repository.listChildren(current.treeUri, current.uri)
                children.filter { it.isDirectory }
                    .sortedBy { it.name.lowercase() }
            }
        }.onSuccess {
            directories = it
        }.onFailure {
            directories = emptyList()
            error = it.message ?: "无法读取目标文件夹"
        }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (folders.size > 1) folders.removeAt(folders.lastIndex)
                    },
                    enabled = folders.size > 1
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "上一级")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (target.move) "移动到" else "复制到",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        folders.joinToString(" / ") { it.name },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                storageRoots.forEach { root ->
                    FilterChip(
                        selected = current.treeUri == root.treeUri &&
                            current.uri == root.uri,
                        onClick = {
                            folders.clear()
                            folders += PickerFolder(root.uri, root.name, root.treeUri)
                        },
                        label = { Text(root.name) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    error != null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            error.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    directories.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "没有子文件夹",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(directories, key = { it.uri.toString() }) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        folders += PickerFolder(
                                            folder.uri,
                                            folder.name,
                                            current.treeUri
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(25.dp)
                                )
                                Text(
                                    folder.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 50.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = { onChoose(current.uri, current.treeUri) },
                    enabled = !loading && error == null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (target.move) "移动到这里" else "复制到这里")
                }
            }
        }
    }
}

private fun discoverStorageRoots(
    context: Context,
    target: TransferTarget
): List<StorageRoot> {
    val roots = linkedMapOf<String, StorageRoot>()

    fun addRoot(file: File?, name: String) {
        val directory = file ?: return
        if (!directory.isDirectory) return
        val canonical = runCatching { directory.canonicalFile }.getOrDefault(directory)
        roots.putIfAbsent(
            canonical.absolutePath,
            StorageRoot(Uri.fromFile(canonical), name, Uri.fromFile(canonical))
        )
    }

    addRoot(Environment.getExternalStorageDirectory(), "内部存储")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val manager = context.getSystemService(StorageManager::class.java)
        manager.storageVolumes.forEachIndexed { index, volume ->
            val label = volume.getDescription(context)
                .takeIf { it.isNotBlank() && index > 0 }
                ?: "存储卡 ${index}"
            addRoot(volume.directory, label)
        }
    }
    context.getExternalFilesDirs(null).forEachIndexed { index, appDirectory ->
        val appDirectoryPath = appDirectory?.absolutePath ?: return@forEachIndexed
        val rootPath = appDirectoryPath.substringBefore("/Android/", appDirectoryPath)
        addRoot(File(rootPath), if (index == 0) "内部存储" else "存储卡 $index")
    }

    val targetFile = target.rootUri.localFile()
    if (targetFile != null) addRoot(targetFile, target.rootLabel)
    roots.putIfAbsent(
        target.rootUri.toString(),
        StorageRoot(target.rootUri, target.rootLabel, target.treeUri)
    )
    return roots.values.toList()
}
