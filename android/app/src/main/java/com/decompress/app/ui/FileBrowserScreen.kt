@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.decompress.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.data.BrowserSortMode
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.collapseSplitSelections
import com.decompress.app.data.FileBrowserController
import com.decompress.app.data.QuickLocationRepository
import com.decompress.app.data.TaskHistoryEntry
import com.decompress.app.data.TaskHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.launch

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
    var batchPattern by state._batchPattern
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

    val listStates = remember { mutableMapOf<String, LazyListState>() }
    val listKey = controller.currentUri?.toString() ?: controller.path.joinToString("/")
    val fileListState = listStates.getOrPut(listKey) { LazyListState() }

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
        val selectedFiles = collapseSplitSelections(
            controller.entries.filter { it.uri in controller.selected && !it.isDirectory }
        )
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
            // 批量解压交给解压面板后收回多选态，避免解压完还停在选择模式。
            controller.clearSelection()
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
                onExtract = { extractSelected() },
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
            Row(modifier = Modifier.fillMaxSize()) {
                if (rememberZippestWindowSize() != ZippestWindowSize.COMPACT &&
                    controller.treeUri != null &&
                    controller.error == null &&
                    !searchMode
                ) {
                    BrowserNavPane(
                        path = controller.path,
                        folders = visibleEntries.filter { it.isDirectory },
                        onJumpTo = { index ->
                            scope.launch {
                                var steps = controller.path.lastIndex - index
                                while (steps > 0) {
                                    if (!controller.goUp()) break
                                    steps -= 1
                                }
                            }
                        },
                        onOpenFolder = { entry -> scope.launch { controller.open(entry) } },
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .padding(padding)
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
            !pullRefreshing && controller.loading && controller.entries.isEmpty() ->
                LoadingSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
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
                    listState = fileListState,
                    transferActive = transferSession != null,
                    transferBusy = transferBusy,
                    scope = scope,
                    onExtractFile = { entry -> extractFile(entry) },
                    onExtractArchive = { entry -> extractFile(entry) },
                    onOpenArchive = { entry -> onOpenArchive(entry) },
                    onOpenWithFile = { entry -> openWithFile(entry) },
                    onGlobalHitOpen = { entry ->
                        if (entry.isArchive) {
                            onOpenArchive(entry)
                        } else {
                            openWithFile(entry)
                        }
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
                }
                if (rememberZippestWindowSize() == ZippestWindowSize.EXPANDED) {
                    BrowserPreviewPane(
                        entry = selectedEntries.singleOrNull(),
                        folderName = controller.folderName,
                        path = controller.path,
                        itemCount = visibleEntries.size,
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .padding(padding)
                    )
                }
            }
            FileBrowserOverlays(
                state = state,
                controller = controller,
                quickLocationRepository = quickLocationRepository,
                padding = padding,
                selectedCount = selectedCount,
                scope = scope,
                onExtractSelected = { extractSelected() },
                onBack = ::handleBack,
                onNotify = { message -> notify(message) },
                onRefreshExternalStorages = { refreshExternalStorages() },
                onClosePanels = { closePanelPopups() },
                onDismissDialogs = { dismissFileDialogs() }
            )
            FileBrowserMenus(
                state = state,
                controller = controller,
                settings = appSettings,
                settingsRepository = settingsRepository,
                padding = padding,
                scope = scope,
                onOpenSettings = onOpenSettings,
                onCompressSelected = { compressSelected() },
                onOpenWithFile = { entry -> openWithFile(entry) },
                onOpenArchive = { entry -> onOpenArchive(entry) },
                onNotify = { message -> notify(message) },
                onClosePanels = { closePanelPopups() },
                onDismissDialogs = { dismissFileDialogs() }
            )
        }
    }


    FileBrowserDialogs(
        state = state,
        controller = controller,
        settings = appSettings,
        settingsRepository = settingsRepository,
        quickLocationRepository = quickLocationRepository,
        scope = scope,
        renameFocus = renameFocus,
        notify = { message -> notify(message) }
    )
}


