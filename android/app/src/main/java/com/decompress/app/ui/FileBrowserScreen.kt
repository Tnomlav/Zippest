@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

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

@Composable
internal fun FileBrowserScreen(
    controller: FileBrowserController,
    settingsRepository: AppSettingsRepository,
    quickLocationRepository: QuickLocationRepository,
    archiveTaskActive: Boolean,
    activeTaskCount: Int,
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
    var onboardingDenied by rememberSaveable { mutableStateOf(false) }
    val state = remember { FileBrowserState() }
    var entryPageSize by state._entryPageSize
    LaunchedEffect(controller.currentUri) {
        entryPageSize = 300
    }
    var query by rememberSaveable { mutableStateOf("") }
    var globalSearch by rememberSaveable { mutableStateOf(false) }
    var globalHits by state._globalHits
    var globalSearching by state._globalSearching
    var pullRefreshing by state._pullRefreshing
    var externalStorages by state._externalStorages
    var storageExpanded by state._storageExpanded
    var sortExpanded by state._sortExpanded
    var moreExpanded by state._moreExpanded
    var selectionMenuExpanded by state._selectionMenuExpanded
    var showNewFolderDialog by state._showNewFolderDialog
    var newFolderName by state._newFolderName
    var showRenameDialog by state._showRenameDialog
    var showBatchRename by state._showBatchRename
    var batchPattern by rememberSaveable { mutableStateOf("{name}_{001}") }
    var batchRenaming by state._batchRenaming
    var checksumTarget by state._checksumTarget
    var checksumResult by state._checksumResult
    var checksumBusy by state._checksumBusy
    var repairMessage by state._repairMessage
    var renameValue by state._renameValue
    var showCustomSuffixDialog by state._showCustomSuffixDialog
    var showDeleteConfirmation by state._showDeleteConfirmation
    var showTrash by state._showTrash
    var showQuickLocationEditor by state._showQuickLocationEditor
    var customSuffix by state._customSuffix
    var showTaskQueue by state._showTaskQueue
    var transferSession by state._transferSession
    var transferBusy by state._transferBusy
    var compressTarget by state._compressTarget
    var openWithRequest by state._openWithRequest

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
            onboardingDenied = false
            scope.launch { controller.openInternalStorage() }
        } else {
            onboardingDenied = true
            onNotify("尚未获得所有文件访问权限")
        }
    }

    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it } && controller.hasDirectStorageAccess()) {
            onboardingDenied = false
            scope.launch { controller.openInternalStorage() }
        } else {
            onboardingDenied = true
            onNotify("存储权限被拒绝，可改为选择一个文件夹")
        }
    }

    fun notify(message: String) {
        onNotify(message)
    }

    LaunchedEffect(controller.treeUri) {
        if (controller.treeUri == null) return@LaunchedEffect
        val hints = context.getSharedPreferences("zippest-hints", Context.MODE_PRIVATE)
        if (!hints.getBoolean("long_press_shown", false)) {
            hints.edit().putBoolean("long_press_shown", true).apply()
            delay(900)
            notify("长按文件可以多选，点按直接打开")
        }
    }

    fun openHistoryLocation(entry: TaskHistoryEntry) {
        val uri = entry.locationUri?.let(Uri::parse) ?: return
        scope.launch {
            val opened = runCatching {
                controller.openLocation(
                    location = uri,
                    name = entry.locationName,
                    treeUri = entry.treeUri?.let(Uri::parse),
                    pathHint = entry.path
                )
            }.getOrDefault(false)
            if (!opened) {
                notify("文件夹已不存在，可能已被删除或移动")
            }
        }
    }

    fun refreshFromPull() {
        if (pullRefreshing) return
        pullRefreshing = true
        scope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            try {
                controller.refresh()
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                if (elapsed < 900L) delay(900L - elapsed)
                pullRefreshing = false
            }
        }
    }

    fun refreshExternalStorages() {
        scope.launch {
            externalStorages = withContext(Dispatchers.IO) {
                discoverExternalStorageLocations(context)
            }
        }
    }

    fun closePanelPopups() {
        storageExpanded = false
        sortExpanded = false
        moreExpanded = false
        selectionMenuExpanded = false
    }

    fun dismissFileDialogs() {
        showNewFolderDialog = false
        showRenameDialog = false
        showCustomSuffixDialog = false
        showDeleteConfirmation = false
        showTrash = false
        showQuickLocationEditor = false
        showTaskQueue = false
        compressTarget = null
        openWithRequest = null
    }

    fun openWithFile(entry: BrowserEntry) {
        closePanelPopups()
        dismissFileDialogs()
        scope.launch {
            val viewIntent = createOpenWithIntent(context, entry)
            val options = viewIntent?.let {
                queryOpenWithOptions(context, it)
            }.orEmpty()
            if (viewIntent != null && options.isNotEmpty()) {
                val request = OpenWithRequest(
                    displayName = entry.name,
                    mimeType = viewIntent.type ?: "*/*",
                    extension = entry.name
                        .substringAfterLast('.', "")
                        .lowercase(),
                    intent = viewIntent,
                    options = options
                )
                if (!launchRememberedOpenWith(context, request)) {
                    openWithRequest = request
                }
            } else {
                notify("没有可打开此文件的应用")
            }
        }
    }

    LaunchedEffect(context) {
        externalStorages = withContext(Dispatchers.IO) {
            discoverExternalStorageLocations(context)
        }
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                refreshExternalStorages()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                addDataScheme("file")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
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
    LaunchedEffect(controller.selectionMode) {
        if (!controller.selectionMode) selectionMenuExpanded = false
    }

    val appSettings = settingsRepository.settings
    val sortComparator = when (appSettings.defaultSortMode) {
        BrowserSortMode.NAME -> compareBy<BrowserEntry> { it.name.lowercase() }
        BrowserSortMode.SIZE -> compareBy { it.size }
        BrowserSortMode.DATE -> compareBy { it.lastModified }
        BrowserSortMode.TYPE -> compareBy<BrowserEntry> {
            it.extension.ifBlank { "\uFFFF" }
        }.thenBy { it.name.lowercase() }
    }
    val visibleEntries = controller.entries
        .filter { appSettings.showHiddenFiles || !it.name.startsWith(".") }
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedWith(
            compareByDescending<BrowserEntry> { it.isDirectory }.then(
                if (appSettings.sortAscending) sortComparator else sortComparator.reversed()
            )
        )
    var recentExtracts by remember { mutableStateOf<List<TaskHistoryEntry>>(emptyList()) }
    LaunchedEffect(appSettings.showRecentExtractions) {
        if (!appSettings.showRecentExtractions) {
            recentExtracts = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            recentExtracts = runCatching {
                TaskHistoryStore(context.applicationContext).list()
                    .filter { it.kind == "extract" && it.success && it.locationUri != null }
                    .filter { entry ->
                        val uri = entry.locationUri?.let(Uri::parse)
                        uri == null || uri.scheme != "file" ||
                            File(uri.path.orEmpty()).isDirectory
                    }
                    .take(3)
            }.getOrDefault(emptyList())
            delay(5_000)
        }
    }
    val allSelected = visibleEntries.isNotEmpty() &&
        visibleEntries.all { it.uri in controller.selected }

    LaunchedEffect(searchMode, globalSearch, query) {
        if (!searchMode || !globalSearch || query.isBlank()) {
            globalHits = emptyList()
            globalSearching = false
            return@LaunchedEffect
        }
        delay(250)
        globalSearching = true
        globalHits = runCatching { controller.searchGlobal(query) }
            .getOrDefault(emptyList())
        globalSearching = false
    }
    val selectedCount = controller.selected.size
    val selectedEntries = controller.entries.filter { it.uri in controller.selected }

    fun startTransfer(move: Boolean) {
        if (selectedEntries.isEmpty()) {
            notify(if (move) "请选择要移动的文件" else "请选择要复制的文件")
            return
        }
        val sourceLocation = controller.captureLocation()
        if (sourceLocation == null) {
            notify("当前存储位置不可用")
            return
        }
        closePanelPopups()
        dismissFileDialogs()
        transferSession = TransferSession(
            sources = selectedEntries,
            move = move,
            sourceLocation = sourceLocation
        )
        controller.clearSelection()
    }

    fun cancelTransfer() {
        if (transferBusy) return
        val session = transferSession ?: return
        transferSession = null
        scope.launch {
            controller.restoreLocation(session.sourceLocation)
        }
    }

    fun confirmTransfer() {
        val session = transferSession ?: return
        if (transferBusy) return
        val destinationUri = controller.currentUri
        val destinationTreeUri = controller.treeUri
        if (destinationUri == null || destinationTreeUri == null) {
            notify("目标位置不可用")
            return
        }
        if (
            session.move &&
            destinationUri == session.sourceLocation.currentUri
        ) {
            notify("请选择其他文件夹")
            return
        }
        transferSession = null
        transferBusy = false
        notify(
            "正在${if (session.move) "移动" else "复制"} " +
                "${session.sources.size} 项…"
        )
        scope.launch {
            val action = if (session.move) "移动" else "复制"
            val summary = runCatching {
                controller.transferEntriesToDirectory(
                    sources = session.sources,
                    sourceDirectoryUri = session.sourceLocation.currentUri,
                    destinationDirectoryUri = destinationUri,
                    destinationTreeUri = destinationTreeUri,
                    move = session.move
                )
            }.getOrNull()
            val message = when {
                summary == null -> "${action}失败"
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
            controller.refresh()
        }
    }

    fun handleBack() {
        when {
            transferBusy -> Unit
            transferSession != null -> scope.launch {
                if (!controller.goUp()) cancelTransfer()
            }
            storageExpanded || sortExpanded || moreExpanded || selectionMenuExpanded ->
                closePanelPopups()
            showCustomSuffixDialog -> showCustomSuffixDialog = false
            showDeleteConfirmation -> showDeleteConfirmation = false
            showTrash -> {
                showTrash = false
                scope.launch { controller.refresh() }
            }
            showRenameDialog || showNewFolderDialog || showTaskQueue ||
                showQuickLocationEditor ||
                compressTarget != null ||
                openWithRequest != null -> dismissFileDialogs()
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

    fun extractSelected() {
        closePanelPopups()
        dismissFileDialogs()
        val selectedFiles = controller.entries.filter {
            it.uri in controller.selected && !it.isDirectory
        }
        val parentUri = controller.currentUri
        val parentTreeUri = controller.treeUri
        if (selectedFiles.isEmpty()) {
            notify("请选择文件")
        } else if (parentUri != null && parentTreeUri != null) {
            onExtract(
                selectedFiles,
                parentUri,
                parentTreeUri,
                controller.path.joinToString(" / "),
                controller.folderName
            )
        }
    }

    fun extractFile(entry: BrowserEntry) {
        val parentUri = controller.currentUri
        val parentTreeUri = controller.treeUri
        if (parentUri == null || parentTreeUri == null) {
            notify("当前存储位置不可用")
            return
        }
        onExtract(
            listOf(entry),
            parentUri,
            parentTreeUri,
            controller.path.joinToString(" / "),
            controller.folderName
        )
    }

    fun compressSelected() {
        closePanelPopups()
        dismissFileDialogs()
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

    val directStorage =
        controller.storageMode == FileBrowserController.StorageMode.DIRECT

    fun toggleQuickAccess() {
        val opening = !storageExpanded
        closePanelPopups()
        storageExpanded = opening
        if (opening) refreshExternalStorages()
    }

    BackHandler { handleBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FileBrowserTopBar(
                selectionMode = controller.selectionMode,
                searchMode = searchMode,
                transferActive = transferSession != null,
                directStorage = directStorage,
                hasTree = controller.treeUri != null,
                folderName = controller.folderName,
                storageExpanded = storageExpanded,
                query = query,
                globalSearch = globalSearch,
                selectedCount = selectedCount,
                allSelected = allSelected,
                canGoUp = controller.canGoUp,
                searchFocus = searchFocus,
                onClearSelection = controller::clearSelection,
                onSelectAll = { checked ->
                    if (checked) {
                        controller.setSelection(visibleEntries.map { it.uri }.toSet())
                    } else {
                        controller.deselectAll()
                    }
                },
                onExitSearch = {
                    searchMode = false
                    query = ""
                },
                onQueryChange = { query = it },
                onToggleGlobalSearch = {
                    if (!directStorage) {
                        notify("全局搜索需要「所有文件访问」权限")
                    } else {
                        globalSearch = !globalSearch
                    }
                },
                onGoUp = {
                    scope.launch {
                        if (!controller.goUp()) activity?.finish()
                    }
                },
                onToggleQuickAccess = ::toggleQuickAccess,
                onCancelTransfer = ::cancelTransfer,
                onSearch = {
                    closePanelPopups()
                    searchMode = true
                },
                onSort = {
                    closePanelPopups()
                    sortExpanded = true
                },
                onNewFolder = {
                    closePanelPopups()
                    dismissFileDialogs()
                    newFolderName = ""
                    showNewFolderDialog = true
                },
                onMore = {
                    closePanelPopups()
                    moreExpanded = true
                },
                onOpenSettings = onOpenSettings
            )
        },
        bottomBar = {
            FileBrowserBottomBar(
                state = state,
                controller = controller,
                selectedCount = selectedCount,
                archiveTaskActive = archiveTaskActive,
                scope = scope,
                onStartTransfer = { move -> startTransfer(move = move) },
                onCancelTransfer = ::cancelTransfer,
                onConfirmTransfer = ::confirmTransfer,
                onRename = {
                    closePanelPopups()
                    dismissFileDialogs()
                    val entry = selectedEntries.singleOrNull()
                    if (entry == null) {
                        batchPattern = "{name}_{001}"
                        showBatchRename = true
                    } else {
                        renameValue = fileNameWithSuffixSelected(entry.name)
                        showRenameDialog = true
                    }
                },
                onDelete = {
                    closePanelPopups()
                    showDeleteConfirmation = true
                },
                onMenuExpandedChange = { expanded ->
                    closePanelPopups()
                    selectionMenuExpanded = expanded
                }
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
            controller.treeUri == null -> Onboarding(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                deniedOnce = onboardingDenied,
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
            !pullRefreshing && controller.loading && controller.entries.isEmpty() -> Box(
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
            else -> PullToRefreshBox(
                isRefreshing = pullRefreshing || controller.loading,
                onRefresh = ::refreshFromPull,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                FileBrowserContent(
                    controller = controller,
                    settings = appSettings,
                    visibleEntries = visibleEntries,
                    query = query,
                    searchMode = searchMode,
                    globalSearch = globalSearch,
                    globalHits = globalHits,
                    globalSearching = globalSearching,
                    recentExtracts = recentExtracts,
                    pageSize = entryPageSize,
                    onLoadMore = { entryPageSize += 300 },
                    transferActive = transferSession != null,
                    transferBusy = transferBusy,
                    scope = scope,
                    onExtractFile = { entry -> extractFile(entry) },
                    onOpenArchive = { entry -> onOpenArchive(entry) },
                    onOpenWithFile = { entry -> openWithFile(entry) },
                    onGlobalHitOpen = { entry ->
                        openWithFile(entry)
                        searchMode = false
                        query = ""
                    },
                    onBrowseDownloads = {
                        scope.launch {
                            controller.openStorageRoot(
                                File(Environment.getExternalStorageDirectory(), "Download"),
                                "Download"
                            )
                        }
                    },
                    onOpenHistoryLocation = { entry -> openHistoryLocation(entry) }
                )
            }
            }
            if (selectedCount > 0 && !selectionMenuExpanded) {
                FloatingActionButton(
                    onClick = ::extractSelected,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 24.dp,
                            bottom = padding.calculateBottomPadding() + 16.dp
                        )
                        .size(58.dp)
                        .zIndex(2f),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Outlined.Archive,
                        contentDescription = "解压",
                        modifier = Modifier.size(28.dp)
                    )
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
                    locations = quickLocationRepository.locations,
                    externalStorages = externalStorages,
                    onDismiss = { storageExpanded = false },
                    onAdd = {
                        closePanelPopups()
                        dismissFileDialogs()
                        showQuickLocationEditor = true
                    },
                    onOpen = { location ->
                        storageExpanded = false
                        scope.launch {
                            val opened = controller.openFixedLocation(
                                location.label,
                                location.path
                            )
                            if (!opened) {
                                notify("${location.label}不可用")
                            }
                        }
                    },
                    onOpenExternal = { storage ->
                        storageExpanded = false
                        scope.launch {
                            val opened = controller.openStorageRoot(
                                File(storage.path),
                                storage.label
                            )
                            if (!opened) {
                                notify("${storage.label}不可用")
                                refreshExternalStorages()
                            }
                        }
                    }
                )
            }
            if (sortExpanded) {
                ZippestBarAttachedMenu(
                    alignment = Alignment.TopEnd,
                    panelModifier = Modifier.padding(
                        top = padding.calculateTopPadding(),
                        end = 4.dp
                    ),
                    onDismissRequest = { sortExpanded = false }
                ) {
                    BrowserSortMode.entries.forEach { mode ->
                        val active = appSettings.defaultSortMode == mode
                        ZippestMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                settingsRepository.update {
                                    if (it.defaultSortMode == mode) {
                                        it.copy(sortAscending = !it.sortAscending)
                                    } else {
                                        it.copy(
                                            defaultSortMode = mode,
                                            sortAscending = mode == BrowserSortMode.NAME ||
                                                mode == BrowserSortMode.TYPE
                                        )
                                    }
                                }
                                sortExpanded = false
                            },
                            selected = active,
                            trailingIcon = {
                                if (active) {
                                    Text(
                                        if (appSettings.sortAscending) "↑" else "↓",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
            if (moreExpanded) {
                ZippestBarAttachedMenu(
                    alignment = Alignment.TopEnd,
                    panelModifier = Modifier.padding(
                        top = padding.calculateTopPadding(),
                        end = 4.dp
                    ),
                    onDismissRequest = { moreExpanded = false }
                ) {
                    ZippestMenuItem(
                        text = { Text("设置") },
                        leadingIcon = { Icon(Icons.Outlined.Tune, null) },
                        onClick = {
                            moreExpanded = false
                            onOpenSettings()
                        }
                    )
                    ZippestMenuItem(
                        text = { Text("后台任务") },
                        leadingIcon = { Icon(Icons.Outlined.Schedule, null) },
                        onClick = {
                            moreExpanded = false
                            closePanelPopups()
                            dismissFileDialogs()
                            showTaskQueue = true
                        }
                    )
                    ZippestMenuItem(
                        text = { Text("回收站") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = {
                            moreExpanded = false
                            closePanelPopups()
                            dismissFileDialogs()
                            controller.clearSelection()
                            showTrash = true
                        }
                    )
                }
            }
            if (selectionMenuExpanded) {
                ZippestBarAttachedMenu(
                    alignment = Alignment.BottomEnd,
                    panelModifier = Modifier.padding(
                        bottom = padding.calculateBottomPadding(),
                        end = 4.dp
                    ),
                    onDismissRequest = { selectionMenuExpanded = false }
                ) {
                    ZippestMenuItem(
                        text = { Text("压缩") },
                        leadingIcon = { Icon(Icons.Outlined.Unarchive, null) },
                        onClick = {
                            selectionMenuExpanded = false
                            compressSelected()
                        }
                    )
                    ZippestMenuItem(
                        text = { Text("回收站") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = {
                            selectionMenuExpanded = false
                            controller.clearSelection()
                            showTrash = true
                        }
                    )
                    selectedEntries.singleOrNull()?.takeIf { !it.isDirectory }?.let { candidate ->
                        ZippestMenuItem(
                            text = { Text("检查完整性") },
                            leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                            onClick = {
                                selectionMenuExpanded = false
                                checksumTarget = candidate
                                checksumResult = null
                                checksumBusy = true
                                scope.launch {
                                    checksumResult = com.decompress.app.data.Checksums
                                        .compute(context, candidate.uri)
                                    checksumBusy = false
                                }
                            }
                        )
                    }
                    val repairCandidate = selectedEntries.singleOrNull()?.takeIf {
                        it.uri.scheme == "file" &&
                            (it.name.endsWith(".zip", true) || it.name.endsWith(".apk", true))
                    }
                    if (repairCandidate != null) {
                        ZippestMenuItem(
                            text = { Text("修复压缩包") },
                            leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                            onClick = {
                                selectionMenuExpanded = false
                                val source = repairCandidate.uri.path?.let(::File)
                                if (source == null || !source.exists()) {
                                    notify("无法访问该文件")
                                } else {
                                    notify("正在修复，完成后会提示")
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            val output = File(
                                                source.parentFile,
                                                source.nameWithoutExtension + ".repaired.zip"
                                            )
                                            com.decompress.app.data.ZipRepair.repair(source, output)
                                        }
                                        repairMessage = when {
                                            result == null -> "修复失败：没有找到可恢复的条目"
                                            result.skipped > 0 ->
                                                "已恢复 ${result.entries} 个条目，跳过 ${result.skipped} 段；保存为 ${result.file.name}"
                                            else ->
                                                "已恢复 ${result.entries} 个条目；保存为 ${result.file.name}"
                                        }
                                        controller.refresh()
                                    }
                                }
                            }
                        )
                    }
                    val canOpenWith = selectedEntries.singleOrNull()?.let {
                        !it.isDirectory && !it.isArchive
                    } == true
                    ZippestMenuItem(
                        text = { Text("打开方式") },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
                        enabled = canOpenWith,
                        onClick = {
                            selectionMenuExpanded = false
                            selectedEntries.singleOrNull()?.let { entry ->
                                if (!entry.isDirectory && !entry.isArchive) {
                                    openWithFile(entry)
                                }
                            }
                        }
                    )
                    ZippestMenuItem(
                        text = { Text("分享") },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                        onClick = {
                            selectionMenuExpanded = false
                            val shared = shareEntries(context, selectedEntries)
                            if (!shared) notify("系统中没有可用的分享应用")
                        }
                    )
                }
            }
        }
    }

    if (showBatchRename) {
        val previews = selectedEntries.sortedBy { it.name }
            .take(2)
            .mapIndexed { index, entry ->
                entry.name + " → " + batchNameFor(entry, index + 1, batchPattern)
            }
        AlertDialog(
            onDismissRequest = { showBatchRename = false },
            title = { Text("批量重命名 ${selectedEntries.size} 项") },
            text = {
                Column {
                    OutlinedTextField(
                        value = batchPattern,
                        onValueChange = { batchPattern = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("命名模板") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "预览：",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    previews.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "怎么填：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "{name} 原名 · {n} 1/2 · {001} 补零 · {ext} 后缀 · {date} 年月日 · {time} 时分秒",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "常用写法",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { batchPattern = "{name}_{001}" }) {
                            Text("{name}_{001}", maxLines = 1)
                        }
                        TextButton(onClick = { batchPattern = "{name}-{n}" }) {
                            Text("{name}-{n}", maxLines = 1)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { batchPattern = "{date}_{name}" }) {
                            Text("{date}_{name}", maxLines = 1)
                        }
                        TextButton(onClick = { batchPattern = "{name} ({n})" }) {
                            Text("{name} ({n})", maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !batchRenaming && batchPattern.isNotBlank(),
                    onClick = {
                        showBatchRename = false
                        val targets = selectedEntries.sortedBy { it.name }
                        scope.launch {
                            batchRenaming = true
                            var renamed = 0
                            targets.forEachIndexed { index, entry ->
                                val candidate = batchNameFor(entry, index + 1, batchPattern)
                                if (controller.renameEntry(entry, candidate) != null) renamed++
                            }
                            batchRenaming = false
                            controller.clearSelection()
                            notify("已重命名 $renamed/${targets.size} 项")
                        }
                    }
                ) {
                    Text("重命名")
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { showBatchRename = false }) { Text("取消") }
            }
        )
    }

    if (repairMessage != null) {
        AlertDialog(
            onDismissRequest = { repairMessage = null },
            title = { Text("压缩包修复") },
            text = { Text(repairMessage!!) },
            confirmButton = {
                Button(onClick = { repairMessage = null }) { Text("知道了") }
            }
        )
    }

    if (checksumTarget != null) {
        val entry = checksumTarget!!
        AlertDialog(
            onDismissRequest = { checksumTarget = null },
            title = { Text("校验和") },
            text = {
                Column {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    if (checksumBusy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("正在计算…", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (checksumResult == null) {
                        Text("算不出来：文件读不了", style = MaterialTheme.typography.labelMedium)
                    } else {
                        val (sha, md5) = checksumResult!!
                        Text("SHA-256（更保险）", style = MaterialTheme.typography.labelSmall)
                        Text(
                            sha,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(
                                android.content.ClipboardManager::class.java
                            )
                            clipboard?.setPrimaryClip(ClipData.newPlainText("SHA-256", sha))
                            notify("已复制 SHA-256")
                        }) {
                            Text("复制 SHA-256")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("MD5（更快）", style = MaterialTheme.typography.labelSmall)
                        Text(
                            md5,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(
                                android.content.ClipboardManager::class.java
                            )
                            clipboard?.setPrimaryClip(ClipData.newPlainText("MD5", md5))
                            notify("已复制 MD5")
                        }) {
                            Text("复制 MD5")
                        }
                        Text(
                            "算出来的值一样，就说明内容和对方完全一致。全程在本机算，不联网。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { checksumTarget = null }) { Text("关闭") }
            }
        )
    }

    if (showDeleteConfirmation) {
        val itemCount = selectedEntries.size
        DeleteConfirmationDialog(
            itemCount = itemCount,
            useTrash = appSettings.useTrash,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                val targets = selectedEntries.toList()
                if (targets.isNotEmpty()) {
                    controller.beginDelete(targets)
                    scope.launch {
                        val count = controller.commitDelete(targets)
                        notify(
                            when {
                                count == targets.size && appSettings.useTrash ->
                                    "已将 $count 项移入回收站"
                                count == targets.size ->
                                    "已永久删除 $count 项"
                                count <= 0 ->
                                    "删除失败，已恢复文件"
                                else ->
                                    "已删除 $count 项，${targets.size - count} 项已恢复"
                            }
                        )
                    }
                }
            }
        )
    }

    if (showTrash) {
        TrashSheet(
            onDismiss = {
                showTrash = false
                scope.launch { controller.refresh() }
            },
            onChanged = { message ->
                scope.launch {
                    controller.refresh()
                    notify(message)
                }
            }
        )
    }

    if (showQuickLocationEditor) {
        QuickLocationEditorDialog(
            repository = quickLocationRepository,
            location = null,
            onDismiss = { showQuickLocationEditor = false },
            onSaved = { message ->
                showQuickLocationEditor = false
                notify(message)
            }
        )
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
                Button(
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
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { showNewFolderDialog = false }) {
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
                Button(
                    enabled = renameValue.text.isNotBlank(),
                    onClick = {
                        val entry = selectedEntries.singleOrNull()
                        val name = renameValue.text.trim()
                        showRenameDialog = false
                        if (entry == null || !controller.beginRename(entry, name)) {
                            notify("重命名失败，名称无效或已存在")
                            return@Button
                        }
                        scope.launch {
                            val renamed = controller.commitRename(entry, name)
                            if (renamed == null) {
                                notify("重命名失败，文件名称已恢复")
                            }
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { showRenameDialog = false }) {
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
                Button(
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
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { showCustomSuffixDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    openWithRequest?.let { request ->
        OpenWithDialog(
            request = request,
            onDismiss = { openWithRequest = null },
            onLaunch = { option, rememberChoice ->
                val opened = launchOpenWith(
                    context,
                    request,
                    option,
                    rememberChoice
                )
                openWithRequest = null
                if (!opened) notify("无法打开此文件")
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

}


