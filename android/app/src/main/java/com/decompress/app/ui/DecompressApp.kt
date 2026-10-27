@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.decompress.app.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.FileBrowserController
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.ui.theme.ZippestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SortMode(val label: String) {
    NAME("名称"),
    SIZE("大小"),
    DATE("日期")
}

data class ArchiveTarget(
    val archive: BrowserEntry,
    val parentUri: Uri?,
    val parentTreeUri: Uri?,
    val parentPath: String,
    val parentName: String,
    val requiresCustomDestination: Boolean = false
)

data class ExtractTarget(
    val archives: List<BrowserEntry>,
    val parentUri: Uri?,
    val parentTreeUri: Uri?,
    val parentPath: String,
    val parentName: String,
    val requiresCustomDestination: Boolean = false,
    val initialPassword: String = ""
)

@Composable
fun ZippestApp(incomingArchiveUri: Uri? = null) {
    val context = LocalContext.current
    val controller = remember { FileBrowserController(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var restored by remember { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<ArchiveTarget?>(null) }
    var extractTarget by remember { mutableStateOf<ExtractTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        controller.restore()
        restored = true
    }

    LaunchedEffect(incomingArchiveUri, restored) {
        if (!restored || incomingArchiveUri == null) return@LaunchedEffect
        val entry = describeIncomingFile(context, incomingArchiveUri)
        archiveTarget = ArchiveTarget(
            archive = entry,
            parentUri = null,
            parentTreeUri = null,
            parentPath = "外部文件",
            parentName = "外部文件",
            requiresCustomDestination = true
        )
    }

    DisposableEffect(controller) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val successful = intent?.getBooleanExtra(
                    ExtractionScheduler.EXTRA_EXTRACTION_SUCCESS,
                    false
                ) == true
                val message = intent?.getStringExtra(
                    ExtractionScheduler.EXTRA_EXTRACTION_MESSAGE
                ) ?: if (successful) "解压完成" else "解压失败"
                archiveTarget = null
                scope.launch {
                    controller.refresh()
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ExtractionScheduler.ACTION_EXTRACTION_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    ZippestTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!restored) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val currentArchive = archiveTarget
                if (currentArchive != null) {
                    ArchiveScreen(
                        target = currentArchive,
                        onBack = { archiveTarget = null },
                        onExtract = { archive, password ->
                            extractTarget = ExtractTarget(
                                archives = listOf(archive),
                                parentUri = currentArchive.parentUri,
                                parentTreeUri = currentArchive.parentTreeUri,
                                parentPath = currentArchive.parentPath,
                                parentName = currentArchive.parentName,
                                requiresCustomDestination =
                                    currentArchive.requiresCustomDestination,
                                initialPassword = password
                            )
                        }
                    )
                } else {
                    FileBrowserScreen(
                        controller = controller,
                        onOpenArchive = { entry ->
                            val parentUri = controller.currentUri
                            val parentTreeUri = controller.treeUri
                            if (parentUri != null && parentTreeUri != null) {
                                archiveTarget = ArchiveTarget(
                                    archive = entry,
                                    parentUri = parentUri,
                                    parentTreeUri = parentTreeUri,
                                    parentPath = controller.path.joinToString(" / "),
                                    parentName = controller.folderName
                                )
                            }
                        },
                        onExtract = { archives, parentUri, parentTreeUri, parentPath, parentName ->
                            extractTarget = ExtractTarget(
                                archives = archives,
                                parentUri = parentUri,
                                parentTreeUri = parentTreeUri,
                                parentPath = parentPath,
                                parentName = parentName
                            )
                        }
                    )
                }

                extractTarget?.let { target ->
                    ExtractSheet(
                        target = target,
                        onDismiss = { extractTarget = null },
                        onExtractionStarted = {
                            extractTarget = null
                            archiveTarget = null
                            scope.launch {
                                snackbarHostState.showSnackbar("已加入后台任务队列")
                            }
                        }
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun FileBrowserScreen(
    controller: FileBrowserController,
    onOpenArchive: (BrowserEntry) -> Unit,
    onExtract: (List<BrowserEntry>, Uri, Uri, String, String) -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val searchFocus = remember { FocusRequester() }

    var searchMode by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.NAME) }
    var sortExpanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTaskQueue by remember { mutableStateOf(false) }
    var transferMove by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch { controller.chooseRoot(uri) }
        }
    }

    val transferPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val moving = transferMove
            scope.launch {
                val count = controller.transferSelected(uri, moving)
                val action = if (moving) "移动" else "复制"
                snackbarHostState.showSnackbar(
                    if (count > 0) "已${action} $count 项" else "${action}失败"
                )
            }
        }
    }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(controller.treeUri) {
        searchMode = false
        query = ""
    }
    LaunchedEffect(searchMode) {
        if (searchMode) searchFocus.requestFocus()
    }

    val visibleEntries = controller.entries
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedWith(
            compareByDescending<BrowserEntry> { it.isDirectory }.then(
                when (sortMode) {
                    SortMode.NAME -> compareBy { it.name.lowercase() }
                    SortMode.SIZE -> compareByDescending { it.size }
                    SortMode.DATE -> compareByDescending { it.lastModified }
                }
            )
        )
    val allSelected = visibleEntries.isNotEmpty() &&
        visibleEntries.all { it.uri in controller.selected }
    val selectedCount = controller.selected.size

    BackHandler {
        when {
            controller.selectionMode -> controller.clearSelection()
            searchMode -> {
                searchMode = false
                query = ""
            }
            else -> scope.launch {
                if (!controller.goUp()) activity?.finish()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when {
                controller.selectionMode -> {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = controller::clearSelection) {
                                Icon(Icons.Outlined.Close, contentDescription = "退出多选")
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    "已选 $selectedCount 项",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    if (selectedCount == 0) "取消选择以返回" else "点击项目继续选择",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = {
                                    if (allSelected) {
                                        controller.deselectAll()
                                    } else {
                                        controller.setSelection(visibleEntries.map { it.uri }.toSet())
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    )
                }
                searchMode -> {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                searchMode = false
                                query = ""
                            }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "退出搜索")
                            }
                        },
                        title = {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocus),
                                singleLine = true,
                                placeholder = { Text("搜索当前文件夹") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        },
                        actions = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "清除搜索")
                                }
                            }
                        }
                    )
                }
                else -> {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (!controller.goUp()) activity?.finish()
                                    }
                                },
                                enabled = controller.canGoUp
                            ) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回上一级")
                            }
                        },
                        title = {
                            if (controller.treeUri == null) {
                                Text("Zippest", style = MaterialTheme.typography.titleLarge)
                            } else {
                                Column {
                                    Text(
                                        controller.folderName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        controller.path.joinToString(" / "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            if (controller.treeUri != null) {
                                IconButton(onClick = { searchMode = true }) {
                                    Icon(Icons.Outlined.Search, contentDescription = "搜索")
                                }
                                Box {
                                    IconButton(onClick = { sortExpanded = true }) {
                                        Icon(Icons.Outlined.Sort, contentDescription = "排序")
                                    }
                                    DropdownMenu(
                                        expanded = sortExpanded,
                                        onDismissRequest = { sortExpanded = false }
                                    ) {
                                        SortMode.entries.forEach { mode ->
                                            DropdownMenuItem(
                                                text = { Text(mode.label) },
                                                onClick = {
                                                    sortMode = mode
                                                    sortExpanded = false
                                                },
                                                trailingIcon = {
                                                    if (sortMode == mode) {
                                                        Icon(
                                                            Icons.Outlined.Check,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    newFolderName = ""
                                    showNewFolderDialog = true
                                }) {
                                    Icon(Icons.Outlined.Add, contentDescription = "新建文件夹")
                                }
                                Box {
                                    IconButton(onClick = { moreExpanded = true }) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                                    }
                                    DropdownMenu(
                                        expanded = moreExpanded,
                                        onDismissRequest = { moreExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("后台任务") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Schedule, null)
                                            },
                                            onClick = {
                                                moreExpanded = false
                                                showTaskQueue = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("重新选择文件夹") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.FolderOpen, null)
                                            },
                                            onClick = {
                                                moreExpanded = false
                                                folderPicker.launch(null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("关于") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Info, null)
                                            },
                                            onClick = {
                                                moreExpanded = false
                                                notify("Zippest 1.0.0 · SAF 文件浏览")
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        },
        bottomBar = {
            when {
                selectedCount > 0 -> SelectionToolbar(
                    onExtract = {
                        val selectedArchives = controller.entries.filter {
                            it.uri in controller.selected && it.isArchive
                        }
                        val parentUri = controller.currentUri
                        val parentTreeUri = controller.treeUri
                        if (selectedArchives.isEmpty()) {
                            notify("请选择压缩包")
                        } else if (parentUri != null && parentTreeUri != null) {
                            onExtract(
                                selectedArchives,
                                parentUri,
                                parentTreeUri,
                                controller.path.joinToString(" / "),
                                controller.folderName
                            )
                        }
                    },
                    onCopy = {
                        transferMove = false
                        transferPicker.launch(null)
                    },
                    onMove = {
                        transferMove = true
                        transferPicker.launch(null)
                    },
                    onDelete = { showDeleteDialog = true },
                    onShare = {
                        val selectedEntries = controller.entries.filter {
                            it.uri in controller.selected
                        }
                        val shared = shareEntries(context, selectedEntries)
                        if (!shared) notify("系统中没有可用的分享应用")
                    }
                )
                controller.treeUri != null -> StorageBar(context)
            }
        }
    ) { padding ->
        when {
            controller.treeUri == null -> Onboarding(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onChooseFolder = { folderPicker.launch(null) }
            )
            controller.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            controller.error != null -> ErrorState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                message = controller.error.orEmpty(),
                onRetry = { scope.launch { controller.refresh() } },
                onChooseFolder = { folderPicker.launch(null) }
            )
            visibleEntries.isEmpty() -> EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                filtered = query.isNotBlank()
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(visibleEntries, key = { it.uri.toString() }) { entry ->
                    EntryRow(
                        entry = entry,
                        selectionMode = controller.selectionMode,
                        selected = entry.uri in controller.selected,
                        onClick = {
                            when {
                                controller.selectionMode -> controller.toggleSelection(entry.uri)
                                entry.isDirectory -> scope.launch { controller.open(entry) }
                                entry.isArchive -> onOpenArchive(entry)
                                else -> notify("暂不支持直接打开此文件")
                            }
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            controller.startSelection(entry.uri)
                        },
                        onCheckedChange = { controller.toggleSelection(entry.uri) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                    )
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { Text("名称") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newFolderName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val created = controller.createFolder(newFolderName.trim())
                            showNewFolderDialog = false
                            notify(if (created) "文件夹已创建" else "创建失败")
                        }
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除 $selectedCount 项？") },
            text = { Text("删除后不会保留副本。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val count = controller.deleteSelected()
                            showDeleteDialog = false
                            notify("已删除 $count 项")
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showTaskQueue) {
        TaskQueueDialog(onDismiss = { showTaskQueue = false })
    }
}

@Composable
private fun TaskQueueDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            tasks = runCatching {
                withContext(Dispatchers.IO) {
                    WorkManager.getInstance(context.applicationContext)
                        .getWorkInfosForUniqueWork(ExtractionScheduler.QUEUE_NAME)
                        .get()
                }
            }.getOrDefault(emptyList())
            loading = false
            delay(1000)
        }
    }

    val sortedTasks = tasks.sortedWith(
        compareBy {
            when (it.state) {
                WorkInfo.State.RUNNING -> 0
                WorkInfo.State.ENQUEUED -> 1
                WorkInfo.State.BLOCKED -> 2
                else -> 3
            }
        }
    )
    val hasActiveTasks = sortedTasks.any {
        it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.BLOCKED
    }
    val hasFinishedTasks = sortedTasks.any {
        it.state != WorkInfo.State.RUNNING &&
            it.state != WorkInfo.State.ENQUEUED &&
            it.state != WorkInfo.State.BLOCKED
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("后台任务") },
        text = {
            when {
                loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                sortedTasks.isEmpty() -> Text(
                    "暂无后台解压任务",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sortedTasks.forEach { task ->
                        TaskQueueRow(task)
                    }
                    if (hasFinishedTasks) {
                        TextButton(
                            onClick = {
                                WorkManager.getInstance(context.applicationContext)
                                    .pruneWork()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("清理历史")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasActiveTasks) {
                TextButton(
                    onClick = {
                        WorkManager.getInstance(context.applicationContext)
                            .cancelUniqueWork(ExtractionScheduler.QUEUE_NAME)
                    }
                ) {
                    Text("取消全部", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun TaskQueueRow(task: WorkInfo) {
    val title = task.tags
        .firstOrNull { it.startsWith(ExtractionScheduler.TAG_NAME_PREFIX) }
        ?.removePrefix(ExtractionScheduler.TAG_NAME_PREFIX)
        ?: "解压任务"
    val message = when (task.state) {
        WorkInfo.State.SUCCEEDED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: "解压完成"
        WorkInfo.State.FAILED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: "解压失败"
        WorkInfo.State.CANCELLED -> "已取消"
        WorkInfo.State.BLOCKED -> "等待前序任务"
        WorkInfo.State.ENQUEUED -> "等待执行"
        WorkInfo.State.RUNNING ->
            task.progress.getString(ExtractionScheduler.KEY_MESSAGE) ?: "正在解压"
    }
    val progress = when (task.state) {
        WorkInfo.State.SUCCEEDED -> 100
        WorkInfo.State.RUNNING ->
            task.progress.getInt(ExtractionScheduler.KEY_PROGRESS, 0)
        else -> 0
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (task.state == WorkInfo.State.RUNNING) {
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: BrowserEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(48.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        EntryLeading(entry)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = entryMeta(context, entry),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (entry.isDirectory || entry.isArchive) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntryLeading(entry: BrowserEntry) {
    if (entry.isImage || entry.isVideo) {
        MediaThumbnail(entry)
        return
    }

    val background = when {
        entry.isDirectory -> MaterialTheme.colorScheme.secondaryContainer
        entry.isArchive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = when {
        entry.isDirectory -> MaterialTheme.colorScheme.onSecondaryContainer
        entry.isArchive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when {
        entry.isDirectory -> Icons.Outlined.Folder
        entry.isArchive -> Icons.Outlined.Archive
        else -> Icons.Outlined.InsertDriveFile
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun MediaThumbnail(entry: BrowserEntry) {
    val context = LocalContext.current
    var bitmap by remember(entry.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(entry.uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.loadThumbnail(
                        entry.uri,
                        Size(96, 96),
                        null
                    )
                }.getOrNull()
            }?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
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

@Composable
private fun SelectionToolbar(
    onExtract: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionAction(Icons.Outlined.Unarchive, "解压", onExtract)
            SelectionAction(Icons.Outlined.ContentCopy, "复制", onCopy)
            SelectionAction(Icons.Outlined.DriveFileMove, "移动", onMove)
            SelectionAction(Icons.Outlined.Delete, "删除", onDelete)
            SelectionAction(Icons.Outlined.Share, "分享", onShare)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .combinedClickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StorageBar(context: Context) {
    val (used, total) = remember { readStorageStats() }
    val fraction = if (total > 0L) used.toFloat() / total.toFloat() else 0f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "本机存储 · 已用 ${Formatter.formatShortFileSize(context, used)} / " +
                        Formatter.formatShortFileSize(context, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "只访问已授权目录",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun Onboarding(modifier: Modifier, onChooseFolder: () -> Unit) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(92.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "选择要浏览的文件夹",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Android 通过系统文件授权访问。应用只会读取你选择的目录，不会扫描整台手机。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onChooseFolder,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("选择文件夹")
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, filtered: Boolean) {
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
    }
}

@Composable
private fun ErrorState(
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

private fun entryMeta(context: Context, entry: BrowserEntry): String {
    val date = if (entry.lastModified > 0L) {
        DateUtils.getRelativeTimeSpanString(
            entry.lastModified,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else {
        ""
    }
    return if (entry.isDirectory) {
        listOf("文件夹", date).filter { it.isNotBlank() }.joinToString(" · ")
    } else {
        listOf(
            Formatter.formatShortFileSize(context, entry.size),
            date
        ).filter { it.isNotBlank() }.joinToString(" · ")
    }
}

@Suppress("DEPRECATION")
private fun readStorageStats(): Pair<Long, Long> {
    return runCatching {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val available = stat.availableBlocksLong * stat.blockSizeLong
        (total - available) to total
    }.getOrDefault(0L to 0L)
}

private fun describeIncomingFile(context: Context, uri: Uri): BrowserEntry {
    var displayName: String? = null
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

private fun shareEntries(context: Context, entries: List<BrowserEntry>): Boolean {
    if (entries.isEmpty()) return false
    val uris = ArrayList(entries.map { it.uri })
    val mimeType = entries.singleOrNull()?.mimeType ?: "*/*"
    val intent = if (entries.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
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
    return runCatching {
        context.startActivity(Intent.createChooser(intent, "分享所选文件"))
    }.isSuccess
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
