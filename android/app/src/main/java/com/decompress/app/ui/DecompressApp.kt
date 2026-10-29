@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

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
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.decompress.app.IncomingArchive
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.data.BrowserSortMode
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.FileBrowserController
import com.decompress.app.data.ThemeMode
import com.decompress.app.data.TrashRepository
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.service.CompressionScheduler
import com.decompress.app.service.ArchiveTaskControl
import com.decompress.app.ui.theme.ZippestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class StorageDestination(
    val label: String,
    val directory: String?
) {
    ROOT("目录首页", null),
    DOWNLOADS("下载", Environment.DIRECTORY_DOWNLOADS),
    PICTURES("相册", Environment.DIRECTORY_PICTURES),
    CAMERA("相机", Environment.DIRECTORY_DCIM),
    MOVIES("视频", Environment.DIRECTORY_MOVIES),
    MUSIC("音乐", Environment.DIRECTORY_MUSIC),
    DOCUMENTS("文档", Environment.DIRECTORY_DOCUMENTS)
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
fun ZippestApp(incomingArchive: IncomingArchive? = null) {
    val context = LocalContext.current
    val controller = remember { FileBrowserController(context.applicationContext) }
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var restored by remember { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<ArchiveTarget?>(null) }
    var extractTarget by remember { mutableStateOf<ExtractTarget?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var archiveTasks by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    var showGlobalTaskQueue by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val settingsFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                controller.chooseRoot(uri)
                showSettings = false
            }
        }
    }

    LaunchedEffect(Unit) {
        controller.restore()
        withContext(Dispatchers.IO) {
            TrashRepository(context.applicationContext)
                .purgeExpired(settingsRepository.settings.trashRetention.days)
        }
        restored = true
    }

    LaunchedEffect(Unit) {
        while (true) {
            archiveTasks = runCatching { loadArchiveWorkInfos(context) }
                .getOrDefault(emptyList())
            delay(700)
        }
    }

    LaunchedEffect(incomingArchive?.uri, incomingArchive?.displayName, restored) {
        if (!restored || incomingArchive == null) return@LaunchedEffect
        val entry = describeIncomingFile(
            context,
            incomingArchive.uri,
            incomingArchive.displayName
        )
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
                    if (intent?.action == CompressionScheduler.ACTION_COMPRESSION_FINISHED) {
                        CompressionScheduler.EXTRA_COMPRESSION_SUCCESS
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_SUCCESS
                    },
                    false
                ) == true
                val message = intent?.getStringExtra(
                    if (intent?.action == CompressionScheduler.ACTION_COMPRESSION_FINISHED) {
                        CompressionScheduler.EXTRA_COMPRESSION_MESSAGE
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_MESSAGE
                    }
                ) ?: if (successful) "后台任务完成" else "后台任务失败"
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
            IntentFilter().apply {
                addAction(ExtractionScheduler.ACTION_EXTRACTION_FINISHED)
                addAction(CompressionScheduler.ACTION_COMPRESSION_FINISHED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val appSettings = settingsRepository.settings
    val activeTasks = archiveTasks
        .filter {
            it.state == WorkInfo.State.RUNNING ||
                it.state == WorkInfo.State.ENQUEUED ||
                it.state == WorkInfo.State.BLOCKED
        }
        .sortedBy {
            when (it.state) {
                WorkInfo.State.RUNNING -> 0
                WorkInfo.State.ENQUEUED -> 1
                else -> 2
            }
        }
    val darkTheme = when (appSettings.themeMode) {
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    ZippestTheme(darkTheme = darkTheme) {
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
                if (showSettings) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(
                            repository = settingsRepository,
                            browserPath = controller.path.joinToString(" / "),
                            onBack = { showSettings = false },
                            onChooseBrowserFolder = { settingsFolderPicker.launch(null) },
                            onNotify = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                        EdgeSwipeBackZones(onBack = { showSettings = false })
                    }
                } else if (currentArchive != null) {
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
                        },
                        onNotify = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    )
                } else {
                    FileBrowserScreen(
                        controller = controller,
                        settingsRepository = settingsRepository,
                        archiveTaskActive = activeTasks.isNotEmpty(),
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
                        },
                        onOpenSettings = { showSettings = true },
                        onNotify = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    )
                }

                activeTasks.firstOrNull()?.let { task ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .zIndex(20f)
                    ) {
                        ActiveArchiveTaskBar(
                            task = task,
                            activeCount = activeTasks.size,
                            onClick = { showGlobalTaskQueue = true }
                        )
                    }
                }
                if (showGlobalTaskQueue) {
                    TaskQueueDialog(onDismiss = { showGlobalTaskQueue = false })
                }

                extractTarget?.let { target ->
                    ExtractSheet(
                        target = target,
                        settings = settingsRepository.settings,
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
            CenteredSnackbarHost(
                hostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun FileBrowserScreen(
    controller: FileBrowserController,
    settingsRepository: AppSettingsRepository,
    archiveTaskActive: Boolean,
    onOpenArchive: (BrowserEntry) -> Unit,
    onExtract: (List<BrowserEntry>, Uri, Uri, String, String) -> Unit,
    onOpenSettings: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val searchFocus = remember { FocusRequester() }
    val renameFocus = remember { FocusRequester() }

    var searchMode by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var storageExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(TextFieldValue()) }
    var showCustomSuffixDialog by remember { mutableStateOf(false) }
    var customSuffix by remember { mutableStateOf("") }
    var showTaskQueue by remember { mutableStateOf(false) }
    var transferTarget by remember { mutableStateOf<TransferTarget?>(null) }
    var compressTarget by remember { mutableStateOf<CompressTarget?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch { controller.chooseRoot(uri) }
        }
    }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (controller.hasDirectStorageAccess()) {
            scope.launch { controller.openInternalStorage() }
        } else {
            onNotify("尚未获得所有文件访问权限")
        }
    }

    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it } && controller.hasDirectStorageAccess()) {
            scope.launch { controller.openInternalStorage() }
        } else {
            onNotify("存储权限被拒绝，可改为选择一个文件夹")
        }
    }

    fun notify(message: String) {
        onNotify(message)
    }

    LaunchedEffect(controller.treeUri) {
        searchMode = false
        query = ""
    }
    LaunchedEffect(searchMode) {
        if (searchMode) searchFocus.requestFocus()
    }
    LaunchedEffect(showRenameDialog) {
        if (showRenameDialog) {
            delay(100)
            runCatching { renameFocus.requestFocus() }
        }
    }

    val appSettings = settingsRepository.settings
    val visibleEntries = controller.entries
        .filter { appSettings.showHiddenFiles || !it.name.startsWith(".") }
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedWith(
            compareByDescending<BrowserEntry> { it.isDirectory }.then(
                when (appSettings.defaultSortMode) {
                    BrowserSortMode.NAME -> compareBy { it.name.lowercase() }
                    BrowserSortMode.SIZE -> compareByDescending { it.size }
                    BrowserSortMode.DATE -> compareByDescending { it.lastModified }
                }
            )
        )
    val allSelected = visibleEntries.isNotEmpty() &&
        visibleEntries.all { it.uri in controller.selected }
    val selectedCount = controller.selected.size
    val selectedEntries = controller.entries.filter { it.uri in controller.selected }
    fun handleBack() {
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

    BackHandler { handleBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                                val directStorage =
                                    controller.storageMode ==
                                        FileBrowserController.StorageMode.DIRECT
                                Row(
                                    modifier = Modifier
                                        .clickable(enabled = directStorage) {
                                            storageExpanded = !storageExpanded
                                        }
                                        .padding(end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        controller.path.joinToString(" / "),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (directStorage) {
                                        Spacer(Modifier.size(2.dp))
                                        Icon(
                                            Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = if (storageExpanded) {
                                                "收起固定位置"
                                            } else {
                                                "展开固定位置"
                                            },
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
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
                                        BrowserSortMode.entries.forEach { mode ->
                                            DropdownMenuItem(
                                                text = { Text(mode.label) },
                                                onClick = {
                                                    settingsRepository.update {
                                                        it.copy(defaultSortMode = mode)
                                                    }
                                                    sortExpanded = false
                                                },
                                                trailingIcon = {
                                                    if (
                                                        appSettings.defaultSortMode == mode
                                                    ) {
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
                                            text = { Text("设置") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Tune, null)
                                            },
                                            onClick = {
                                                moreExpanded = false
                                                onOpenSettings()
                                            }
                                        )
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
                                                notify("Zippest 1.2.0 · 文件浏览与压缩")
                                            }
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Outlined.Tune, contentDescription = "设置")
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
            Column {
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
                        val mode = controller.storageMode
                        val treeUri = controller.treeUri
                        val rootUri = treeUri?.let {
                            if (mode == FileBrowserController.StorageMode.DIRECT) {
                                it
                            } else {
                                runCatching { treeRootDocumentUri(it) }.getOrNull()
                            }
                        }
                        if (mode == null || treeUri == null || rootUri == null) {
                            notify("当前存储位置不可用")
                        } else {
                            transferTarget = TransferTarget(
                                sources = selectedEntries,
                                storageMode = mode,
                                treeUri = treeUri,
                                rootUri = rootUri,
                                rootLabel = controller.path.firstOrNull() ?: "根目录",
                                move = false
                            )
                        }
                    },
                    onMove = {
                        val mode = controller.storageMode
                        val treeUri = controller.treeUri
                        val rootUri = treeUri?.let {
                            if (mode == FileBrowserController.StorageMode.DIRECT) {
                                it
                            } else {
                                runCatching { treeRootDocumentUri(it) }.getOrNull()
                            }
                        }
                        if (mode == null || treeUri == null || rootUri == null) {
                            notify("当前存储位置不可用")
                        } else {
                            transferTarget = TransferTarget(
                                sources = selectedEntries,
                                storageMode = mode,
                                treeUri = treeUri,
                                rootUri = rootUri,
                                rootLabel = controller.path.firstOrNull() ?: "根目录",
                                move = true
                            )
                        }
                    },
                    onRename = {
                        val entry = selectedEntries.singleOrNull()
                        if (entry == null) {
                            notify("请只选择一个项目")
                        } else {
                            renameValue = fileNameWithSuffixSelected(entry.name)
                            showRenameDialog = true
                        }
                    },
                    onDelete = {
                        scope.launch {
                            val count = controller.deleteSelected()
                            notify(
                                if (count > 0) {
                                    "已删除 $count 项"
                                } else {
                                    "删除失败，目标可能位于 Android 受保护目录"
                                }
                            )
                        }
                    },
                    onShare = {
                        val shared = shareEntries(context, selectedEntries)
                        if (!shared) notify("系统中没有可用的分享应用")
                    },
                    onCompress = {
                        val parentUri = controller.currentUri
                        val treeUri = controller.treeUri
                        if (selectedEntries.isEmpty()) {
                            notify("请选择要压缩的文件")
                        } else if (parentUri != null && treeUri != null) {
                            compressTarget = CompressTarget(
                                entries = selectedEntries,
                                sourceTreeUri = treeUri,
                                parentUri = parentUri,
                                parentTreeUri = treeUri,
                                parentPath = controller.path.joinToString(" / ")
                            )
                        }
                    }
                )
                    !archiveTaskActive && controller.treeUri != null -> StorageBar(
                        context = context,
                        hasFullAccess =
                            controller.storageMode == FileBrowserController.StorageMode.DIRECT,
                        onSwipeUpHome = {
                            scope.launch { controller.openInternalStorage() }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
            controller.treeUri == null -> Onboarding(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onRequestAccess = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val appSettings = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        runCatching {
                            allFilesAccessLauncher.launch(appSettings)
                        }.onFailure {
                            allFilesAccessLauncher.launch(
                                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            )
                        }
                    } else {
                        val permissions = buildList {
                            add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }.toTypedArray()
                        legacyStoragePermissionLauncher.launch(permissions)
                    }
                },
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
                        showThumbnail = appSettings.showThumbnails,
                        onClick = {
                            when {
                                controller.selectionMode -> controller.toggleSelection(entry.uri)
                                entry.isDirectory -> scope.launch { controller.open(entry) }
                                entry.isArchive -> {
                                    val parentUri = controller.currentUri
                                    val parentTreeUri = controller.treeUri
                                    if (parentUri != null && parentTreeUri != null) {
                                        onExtract(
                                            listOf(entry),
                                            parentUri,
                                            parentTreeUri,
                                            controller.path.joinToString(" / "),
                                            controller.folderName
                                        )
                                    }
                                }
                                else -> {
                                    val opened = openEntryExternally(context, entry)
                                    if (!opened) notify("没有可打开此文件的应用")
                                }
                            }
                        },
                        onPreviewArchive = { onOpenArchive(entry) },
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
            EdgeSwipeBackZones(
                onBack = ::handleBack,
                onHome = {
                    scope.launch {
                        storageExpanded = false
                        searchMode = false
                        query = ""
                        controller.clearSelection()
                        controller.openInternalStorage()
                    }
                }
            )
            if (
                storageExpanded &&
                controller.storageMode == FileBrowserController.StorageMode.DIRECT
            ) {
                StorageQuickLocationPanel(
                    topInset = padding.calculateTopPadding(),
                    currentUri = controller.currentUri,
                    onDismiss = { storageExpanded = false },
                    onOpen = { destination ->
                        storageExpanded = false
                        scope.launch {
                            val opened = controller.openFixedLocation(
                                destination.label,
                                destination.directory
                            )
                            if (!opened) {
                                notify("${destination.label}不可用")
                            }
                        }
                    }
                )
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

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(renameFocus),
                        singleLine = true,
                        label = { Text("新名称") }
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "后缀",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(".7z", ".zip", ".rar").forEach { suffix ->
                            FilterChip(
                                selected = selectedFileSuffix(renameValue)
                                    .equals(suffix, ignoreCase = true),
                                onClick = {
                                    renameValue = replaceSelectedFileSuffix(
                                        renameValue,
                                        suffix
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(suffix.uppercase(), maxLines = 1) }
                            )
                        }
                        FilterChip(
                            selected = selectedFileSuffix(renameValue).lowercase() !in
                                setOf(".7z", ".zip", ".rar"),
                            onClick = {
                                customSuffix = selectedFileSuffix(renameValue)
                                showCustomSuffixDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("自定义", maxLines = 1) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = renameValue.text.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val renamed = controller.renameSelected(
                                renameValue.text.trim()
                            )
                            showRenameDialog = false
                            notify(
                                if (renamed) {
                                    "重命名成功"
                                } else {
                                    "重命名失败，名称无效或已存在"
                                }
                            )
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCustomSuffixDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSuffixDialog = false },
            title = { Text("自定义后缀") },
            text = {
                OutlinedTextField(
                    value = customSuffix,
                    onValueChange = { customSuffix = it },
                    singleLine = true,
                    label = { Text("后缀") },
                    supportingText = { Text("统一使用一个前导点，如 .tar.gz") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = sanitizeFileSuffix(customSuffix).isNotEmpty(),
                    onClick = {
                        renameValue = replaceSelectedFileSuffix(
                            renameValue,
                            sanitizeFileSuffix(customSuffix)
                        )
                        showCustomSuffixDialog = false
                    }
                ) {
                    Text("应用")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSuffixDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showTaskQueue) {
        TaskQueueDialog(onDismiss = { showTaskQueue = false })
    }

    compressTarget?.let { target ->
        CompressionSheet(
            target = target,
            onDismiss = { compressTarget = null },
            onStarted = {
                compressTarget = null
                controller.clearSelection()
                notify("压缩任务已加入后台队列")
            }
        )
    }

    transferTarget?.let { target ->
        TransferDestinationSheet(
            target = target,
            onDismiss = { transferTarget = null },
            onChoose = { destinationUri, destinationTreeUri ->
                transferTarget = null
                scope.launch {
                    val summary = controller.transferSelectedToDirectory(
                        destinationDirectoryUri = destinationUri,
                        destinationTreeUri = destinationTreeUri,
                        move = target.move
                    )
                    val action = if (target.move) "移动" else "复制"
                    val message = when {
                        summary.completed > 0 &&
                            summary.sourceRetained == 0 &&
                            summary.failed == 0 ->
                            "已${action} ${summary.completed} 项"
                        summary.completed + summary.sourceRetained > 0 ->
                            "已复制 ${summary.completed + summary.sourceRetained} 项" +
                                if (summary.sourceRetained > 0) {
                                    "，${summary.sourceRetained} 项源文件未能删除"
                                } else {
                                    "，${summary.failed} 项失败"
                                }
                        else -> summary.firstError ?: "${action}失败"
                    }
                    notify(message)
                }
            }
        )
    }
}

@Composable
internal fun TaskQueueDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pausedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        while (true) {
            tasks = runCatching {
                loadArchiveWorkInfos(context)
            }.getOrDefault(emptyList())
            pausedTaskIds = tasks
                .filter { ArchiveTaskControl.isPaused(context, it.id) }
                .mapTo(mutableSetOf()) { it.id.toString() }
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
                    "暂无后台任务",
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
                        TaskQueueRow(
                            task = task,
                            paused = task.id.toString() in pausedTaskIds,
                            onTogglePause = {
                                val paused = task.id.toString() !in pausedTaskIds
                                ArchiveTaskControl.setPaused(context, task.id, paused)
                                pausedTaskIds = if (paused) {
                                    pausedTaskIds + task.id.toString()
                                } else {
                                    pausedTaskIds - task.id.toString()
                                }
                            }
                        )
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
                        WorkManager.getInstance(context.applicationContext).apply {
                            cancelUniqueWork(ExtractionScheduler.QUEUE_NAME)
                            cancelUniqueWork(CompressionScheduler.QUEUE_NAME)
                        }
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

private suspend fun loadArchiveWorkInfos(context: Context): List<WorkInfo> =
    withContext(Dispatchers.IO) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        buildList {
            addAll(
                workManager
                    .getWorkInfosForUniqueWork(ExtractionScheduler.QUEUE_NAME)
                    .get()
            )
            addAll(
                workManager
                    .getWorkInfosForUniqueWork(CompressionScheduler.QUEUE_NAME)
                    .get()
            )
        }
    }

@Composable
private fun ActiveArchiveTaskBar(
    task: WorkInfo,
    activeCount: Int,
    onClick: () -> Unit
) {
    val isCompression =
        task.tags.contains(CompressionScheduler.TAG_ALL_TASKS)
    val title = task.tags
        .firstOrNull { it.startsWith(ExtractionScheduler.TAG_NAME_PREFIX) }
        ?.removePrefix(ExtractionScheduler.TAG_NAME_PREFIX)
        ?: if (isCompression) "压缩任务" else "解压任务"
    val running = task.state == WorkInfo.State.RUNNING
    val paused = task.progress.getBoolean(ExtractionScheduler.KEY_PAUSED, false)
    val progress = task.progress.getInt(ExtractionScheduler.KEY_PROGRESS, 0)
        .coerceIn(0, 100)
    val message = when {
        paused -> "已暂停，点击任务队列继续"
        running -> task.progress.getString(ExtractionScheduler.KEY_MESSAGE)
            ?: if (isCompression) "正在压缩…" else "正在解压…"
        task.state == WorkInfo.State.BLOCKED -> "等待前序任务完成"
        else -> "等待后台任务开始"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isCompression) Icons.Outlined.Archive
                                else Icons.Outlined.Unarchive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (activeCount > 1) "$title · 另有 ${activeCount - 1} 项"
                            else title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            message,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        when {
                            paused -> "暂停"
                            running -> "$progress%"
                            else -> "等待"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (running) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskQueueRow(
    task: WorkInfo,
    paused: Boolean,
    onTogglePause: () -> Unit
) {
    val title = task.tags
        .firstOrNull { it.startsWith(ExtractionScheduler.TAG_NAME_PREFIX) }
        ?.removePrefix(ExtractionScheduler.TAG_NAME_PREFIX)
        ?: "后台任务"
    val message = when {
        paused -> "已暂停"
        task.state == WorkInfo.State.SUCCEEDED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: "任务完成"
        task.state == WorkInfo.State.FAILED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: "任务失败"
        task.state == WorkInfo.State.CANCELLED -> "已取消"
        task.state == WorkInfo.State.BLOCKED -> "等待前序任务"
        task.state == WorkInfo.State.ENQUEUED -> "等待执行"
        else -> task.progress.getString(ExtractionScheduler.KEY_MESSAGE) ?: "正在处理"
    }
    val progress = when {
        task.state == WorkInfo.State.SUCCEEDED -> 100
        task.state == WorkInfo.State.RUNNING ->
            task.progress.getInt(ExtractionScheduler.KEY_PROGRESS, 0)
        else -> 0
    }
    var previousSample by remember(task.id) {
        mutableStateOf<Pair<Long, Int>?>(null)
    }
    var percentPerSecond by remember(task.id) { mutableStateOf<Float?>(null) }
    LaunchedEffect(task.id, task.state, progress, paused) {
        if (task.state == WorkInfo.State.RUNNING && !paused) {
            val now = SystemClock.elapsedRealtime()
            previousSample?.let { (previousTime, previousProgress) ->
                val elapsed = now - previousTime
                val changed = progress - previousProgress
                if (elapsed in 250..10_000 && changed > 0) {
                    percentPerSecond = changed * 1000f / elapsed
                }
            }
            previousSample = now to progress
        } else {
            previousSample = null
        }
    }
    val etaSeconds = percentPerSecond?.let { rate ->
        if (rate > 0.01f) ((100 - progress) / rate).toInt() else null
    }
    val active = task.state == WorkInfo.State.RUNNING ||
        task.state == WorkInfo.State.ENQUEUED ||
        task.state == WorkInfo.State.BLOCKED

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (active) {
                    TextButton(onClick = onTogglePause) {
                        Icon(
                            if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (paused) "继续" else "暂停")
                    }
                }
            }
            if (task.state == WorkInfo.State.RUNNING && !paused) {
                Text(
                    buildString {
                        percentPerSecond?.let {
                            append("速度 ${"%.1f".format(it)}%/秒")
                        }
                        etaSeconds?.let {
                            if (isNotEmpty()) append(" · ")
                            append("约 ${formatEta(it)}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

private fun formatEta(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    return if (safeSeconds < 60) {
        "${safeSeconds} 秒"
    } else if (safeSeconds < 3600) {
        "${safeSeconds / 60} 分 ${safeSeconds % 60} 秒"
    } else {
        "${safeSeconds / 3600} 小时 ${safeSeconds % 3600 / 60} 分"
    }
}

@Composable
private fun EntryRow(
    entry: BrowserEntry,
    selectionMode: Boolean,
    selected: Boolean,
    showThumbnail: Boolean,
    onClick: () -> Unit,
    onPreviewArchive: () -> Unit,
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

        EntryLeading(entry, showThumbnail)

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

        if (entry.isDirectory) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (entry.isArchive) {
            IconButton(
                onClick = onPreviewArchive,
                enabled = !selectionMode
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "预览压缩包",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EntryLeading(entry: BrowserEntry, showThumbnail: Boolean) {
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

@Composable
private fun MediaThumbnail(entry: BrowserEntry) {
    val context = LocalContext.current
    var bitmap by remember(entry.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(entry.uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    if (entry.uri.scheme == "file") {
                        val file = File(entry.uri.path ?: return@runCatching null)
                        if (entry.isVideo) {
                            ThumbnailUtils.createVideoThumbnail(
                                file,
                                Size(96, 96),
                                null
                            )
                        } else {
                            ThumbnailUtils.createImageThumbnail(
                                file,
                                Size(96, 96),
                                null
                            )
                        }
                    } else {
                        context.contentResolver.loadThumbnail(
                            entry.uri,
                            Size(96, 96),
                            null
                        )
                    }
                }.getOrNull()
            }?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp)),
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
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onCompress: () -> Unit
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
            SelectionAction(Icons.Outlined.Archive, "压缩", onCompress)
            SelectionAction(Icons.Outlined.ContentCopy, "复制", onCopy)
            SelectionAction(Icons.Outlined.DriveFileMove, "移动", onMove)
            SelectionAction(Icons.Outlined.Edit, "重命名", onRename)
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
private fun StorageQuickLocationPanel(
    topInset: Dp,
    currentUri: Uri?,
    onDismiss: () -> Unit,
    onOpen: (StorageDestination) -> Unit
) {
    val root = Environment.getExternalStorageDirectory()
    val directoryLocations = StorageDestination.entries.filter {
        it != StorageDestination.ROOT
    }

    fun isCurrent(destination: StorageDestination): Boolean {
        val expected = destination.directory?.let { File(root, it) } ?: root
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
            shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    top = 12.dp,
                    end = 12.dp,
                    bottom = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StorageQuickLocation(
                    destination = StorageDestination.ROOT,
                    selected = isCurrent(StorageDestination.ROOT),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpen(StorageDestination.ROOT) }
                )
                directoryLocations.chunked(2).forEach { rowDestinations ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowDestinations.forEach { destination ->
                            StorageQuickLocation(
                                destination = destination,
                                selected = isCurrent(destination),
                                modifier = Modifier.weight(1f),
                                onClick = { onOpen(destination) }
                            )
                        }
                        if (rowDestinations.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageQuickLocation(
    destination: StorageDestination,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val icon = when (destination) {
        StorageDestination.ROOT -> Icons.Outlined.Home
        StorageDestination.DOWNLOADS -> Icons.Outlined.Download
        StorageDestination.PICTURES -> Icons.Outlined.PhotoLibrary
        StorageDestination.CAMERA -> Icons.Outlined.PhotoCamera
        StorageDestination.MOVIES -> Icons.Outlined.Movie
        StorageDestination.MUSIC -> Icons.Outlined.MusicNote
        StorageDestination.DOCUMENTS -> Icons.Outlined.Description
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(21.dp)
            )
            Text(
                destination.label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StorageBar(
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
                    if (hasFullAccess) "所有文件访问已授权" else "只访问已授权目录",
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
private fun Onboarding(
    modifier: Modifier,
    onRequestAccess: () -> Unit,
    onChooseFolder: () -> Unit
) {
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
            "访问手机文件",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "授予所有文件访问权限后，可以直接浏览内部存储、下载、相册和所有文件夹，体验更接近 ZArchiver。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestAccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("授予所有文件访问权限")
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onChooseFolder) {
            Text("或仅选择一个文件夹")
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

internal fun fileNameWithSuffixSelected(name: String): TextFieldValue {
    val dot = name.lastIndexOf('.')
    val start = if (dot >= 0) dot else name.length
    return TextFieldValue(
        text = name,
        selection = TextRange(start, name.length)
    )
}

private fun selectedFileSuffix(value: TextFieldValue): String {
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
private fun readStorageStats(): Pair<Long, Long> {
    return runCatching {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val available = stat.availableBlocksLong * stat.blockSizeLong
        (total - available) to total
    }.getOrDefault(0L to 0L)
}

private fun describeIncomingFile(
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

private fun shareEntries(context: Context, entries: List<BrowserEntry>): Boolean {
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

private fun openEntryExternally(context: Context, entry: BrowserEntry): Boolean =
    runCatching {
        val viewUri = if (entry.uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = entry.uri.path ?: error("本地文件路径无效")
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(path)
            )
        } else {
            entry.uri
        }
        val mimeType = entry.mimeType
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(entry.name.substringAfterLast('.', "").lowercase())
            ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(viewUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(entry.name, viewUri)
        }
        context.startActivity(intent)
    }.isSuccess

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
