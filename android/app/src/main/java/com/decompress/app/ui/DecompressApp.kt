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
    val initialPassword: String = "",
    val initialEncoding: FileNameEncoding = FileNameEncoding.AUTO,
    /** 非空时只解压这些包内条目。 */
    val entryPaths: List<String> = emptyList()
)

private data class TransferSession(
    val sources: List<BrowserEntry>,
    val move: Boolean,
    val sourceLocation: FileBrowserController.LocationSnapshot
)

private data class ExternalStorageLocation(
    val label: String,
    val path: String
)

@Composable
fun ZippestApp(
    incomingArchive: IncomingArchive? = null,
    openLocation: OpenLocationRequest? = null,
    onOpenLocationHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val controller = remember { FileBrowserController(context.applicationContext) }
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val quickLocationRepository = remember {
        QuickLocationRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var restored by remember { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<ArchiveTarget?>(null) }
    var extractTarget by remember { mutableStateOf<ExtractTarget?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var archiveTasks by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    var showGlobalTaskQueue by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var crashReport by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        crashReport = com.decompress.app.data.CrashLog.read(context)
    }

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

    LaunchedEffect(openLocation, restored) {
        val request = openLocation ?: return@LaunchedEffect
        if (!restored) return@LaunchedEffect
        archiveTarget = null
        extractTarget = null
        showSettings = false
        controller.openLocation(
            location = request.uri,
            name = request.name,
            treeUri = request.treeUri,
            pathHint = request.path
        )
        onOpenLocationHandled()
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
                val finishedTaskName = intent?.getStringExtra(
                    if (intent?.action == CompressionScheduler.ACTION_COMPRESSION_FINISHED) {
                        CompressionScheduler.EXTRA_COMPRESSION_MESSAGE
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_MESSAGE
                    }
                )
                val successful = intent?.getBooleanExtra(
                    if (intent?.action == CompressionScheduler.ACTION_COMPRESSION_FINISHED) {
                        CompressionScheduler.EXTRA_COMPRESSION_SUCCESS
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_SUCCESS
                    },
                    false
                ) == true
                val message = finishedTaskName ?: if (successful) "后台任务完成" else "后台任务失败"
                val isCompression =
                    intent?.action == CompressionScheduler.ACTION_COMPRESSION_FINISHED
                val location = intent?.getStringExtra(
                    if (isCompression) {
                        CompressionScheduler.EXTRA_COMPRESSION_LOCATION
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_LOCATION
                    }
                )?.let(Uri::parse)
                val locationName = intent?.getStringExtra(
                    if (isCompression) {
                        CompressionScheduler.EXTRA_COMPRESSION_LOCATION_NAME
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_LOCATION_NAME
                    }
                ).orEmpty()
                val locationTree = intent?.getStringExtra(
                    if (isCompression) {
                        CompressionScheduler.EXTRA_COMPRESSION_TREE_URI
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_TREE_URI
                    }
                )?.let(Uri::parse)
                val locationPath = intent?.getStringExtra(
                    if (isCompression) {
                        CompressionScheduler.EXTRA_COMPRESSION_PATH
                    } else {
                        ExtractionScheduler.EXTRA_EXTRACTION_PATH
                    }
                ).orEmpty()
                archiveTarget = null
                scope.launch {
                    controller.refresh()
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = if (location != null) "打开" else null,
                        duration = if (location != null) {
                            SnackbarDuration.Long
                        } else {
                            SnackbarDuration.Short
                        }
                    )
                    if (result == SnackbarResult.ActionPerformed && location != null) {
                        controller.openLocation(
                            location = location,
                            name = locationName,
                            treeUri = locationTree,
                            pathHint = locationPath
                        )
                    }
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
    ZippestTheme(darkTheme = darkTheme, accent = settingsRepository.settings.accentColor) {
        if (crashReport != null) {
            AlertDialog(
                onDismissRequest = {
                    com.decompress.app.data.CrashLog.clear(context)
                    crashReport = null
                },
                title = { Text("上次运行异常退出") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "崩溃信息只保存在本机，不会上传。可以复制给开发者排查。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            crashReport!!,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val clipboard = context.getSystemService(
                            android.content.ClipboardManager::class.java
                        )
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("崩溃信息", crashReport!!)
                        )
                        com.decompress.app.data.CrashLog.clear(context)
                        crashReport = null
                        scope.launch { snackbarHostState.showSnackbar("已复制崩溃信息") }
                    }) {
                        Text("复制详情")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        com.decompress.app.data.CrashLog.clear(context)
                        crashReport = null
                    }) {
                        Text(
                            "关闭",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
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
                            quickLocationRepository = quickLocationRepository,
                            browserPath = controller.path.joinToString(" / "),
                            onBack = { showSettings = false },
                            onChooseBrowserFolder = { settingsFolderPicker.launch(null) },
                            onNotify = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    }
                } else if (currentArchive != null) {
                    ArchiveScreen(
                        target = currentArchive,
                        onBack = { archiveTarget = null },
                        onExtract = { archive, password, entryPaths, archiveEncoding ->
                            extractTarget = ExtractTarget(
                                archives = listOf(archive),
                                parentUri = currentArchive.parentUri,
                                parentTreeUri = currentArchive.parentTreeUri,
                                parentPath = currentArchive.parentPath,
                                parentName = currentArchive.parentName,
                                requiresCustomDestination =
                                    currentArchive.requiresCustomDestination,
                                initialPassword = password,
                                initialEncoding = archiveEncoding,
                                entryPaths = entryPaths
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
                        quickLocationRepository = quickLocationRepository,
                        archiveTaskActive = activeTasks.isNotEmpty(),
                        activeTaskCount = activeTasks.size,
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
                        onRenameArchive = { entry, name ->
                            controller.renameEntry(entry, name)
                        },
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
    var entryPageSize by remember { mutableStateOf(300) }
    LaunchedEffect(controller.currentUri) {
        entryPageSize = 300
    }
    var query by rememberSaveable { mutableStateOf("") }
    var globalSearch by rememberSaveable { mutableStateOf(false) }
    var globalHits by remember { mutableStateOf<List<GlobalSearchHit>>(emptyList()) }
    var globalSearching by remember { mutableStateOf(false) }
    var pullRefreshing by remember { mutableStateOf(false) }
    var externalStorages by remember {
        mutableStateOf<List<ExternalStorageLocation>>(emptyList())
    }
    var storageExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    var selectionMenuExpanded by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showBatchRename by remember { mutableStateOf(false) }
    var batchPattern by rememberSaveable { mutableStateOf("{name}_{001}") }
    var batchRenaming by remember { mutableStateOf(false) }
    var checksumTarget by remember { mutableStateOf<BrowserEntry?>(null) }
    var checksumResult by remember { mutableStateOf<Pair<String, String>?>(null) }
    var checksumBusy by remember { mutableStateOf(false) }
    var repairMessage by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf(TextFieldValue()) }
    var showCustomSuffixDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var showQuickLocationEditor by remember { mutableStateOf(false) }
    var customSuffix by remember { mutableStateOf("") }
    var showTaskQueue by remember { mutableStateOf(false) }
    var transferSession by remember { mutableStateOf<TransferSession?>(null) }
    var transferBusy by remember { mutableStateOf(false) }
    var compressTarget by remember { mutableStateOf<CompressTarget?>(null) }
    var openWithRequest by remember { mutableStateOf<OpenWithRequest?>(null) }

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
                                placeholder = {
                                    Text(if (globalSearch) "搜索全部存储" else "搜索当前文件夹")
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    if (!directStorage) {
                                        notify("全局搜索需要「所有文件访问」权限")
                                    } else {
                                        globalSearch = !globalSearch
                                    }
                                }
                            ) {
                                Text(if (globalSearch) "全部" else "当前")
                            }
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
                            if (transferSession != null) {
                                QuickAccessTopTitle(
                                    directStorage = directStorage,
                                    folderName = controller.folderName,
                                    expanded = storageExpanded,
                                    onClick = ::toggleQuickAccess
                                )
                            } else if (controller.treeUri == null) {
                                Text("Zippest", style = MaterialTheme.typography.titleLarge)
                            } else {
                                QuickAccessTopTitle(
                                    directStorage = directStorage,
                                    folderName = controller.folderName,
                                    expanded = storageExpanded,
                                    onClick = ::toggleQuickAccess
                                )
                            }
                        },
                        actions = {
                            if (transferSession != null) {
                                IconButton(
                                    onClick = ::cancelTransfer,
                                    enabled = !transferBusy
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "取消复制或移动"
                                    )
                                }
                            } else if (controller.treeUri != null) {
                                IconButton(onClick = {
                                    closePanelPopups()
                                    searchMode = true
                                }) {
                                    Icon(Icons.Outlined.Search, contentDescription = "搜索")
                                }
                                Box {
                                    IconButton(onClick = {
                                        closePanelPopups()
                                        sortExpanded = true
                                    }) {
                                        Icon(Icons.Outlined.Sort, contentDescription = "排序")
                                    }
                                }
                                IconButton(onClick = {
                                    closePanelPopups()
                                    dismissFileDialogs()
                                    newFolderName = ""
                                    showNewFolderDialog = true
                                }) {
                                    Icon(Icons.Outlined.Add, contentDescription = "新建文件夹")
                                }
                                Box {
                                    IconButton(onClick = {
                                        closePanelPopups()
                                        moreExpanded = true
                                    }) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
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
                    transferSession != null -> TransferToolbar(
                        move = transferSession?.move == true,
                        itemCount = transferSession?.sources?.size ?: 0,
                        path = controller.path.joinToString(" / "),
                        busy = transferBusy,
                        canConfirm = controller.currentUri != null &&
                            controller.treeUri != null &&
                            (
                                transferSession?.move != true ||
                                    controller.currentUri !=
                                        transferSession?.sourceLocation?.currentUri
                                ),
                        onCancel = ::cancelTransfer,
                        onConfirm = ::confirmTransfer
                    )
                    selectedCount > 0 -> SelectionToolbar(
                    onCopy = {
                        startTransfer(move = false)
                    },
                    onMove = {
                        startTransfer(move = true)
                    },
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
                    menuExpanded = selectionMenuExpanded,
                    onMenuExpandedChange = { expanded ->
                        closePanelPopups()
                        selectionMenuExpanded = expanded
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
                val showGlobalResults = searchMode && globalSearch && query.isNotBlank()
                if (showGlobalResults) {
                    when {
                        globalSearching -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        globalHits.isEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                EmptyState(
                                    modifier = Modifier.fillParentMaxSize(),
                                    filtered = true
                                )
                            }
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                top = 8.dp,
                                end = 12.dp,
                                bottom = 8.dp
                            )
                        ) {
                            items(globalHits, key = { it.entry.uri.toString() }) { hit ->
                                GlobalSearchRow(
                                    hit = hit,
                                    onClick = {
                                        if (hit.entry.isDirectory) {
                                            hit.entry.uri.path?.let { path ->
                                                scope.launch {
                                                    controller.openStorageRoot(
                                                        File(path),
                                                        hit.entry.name
                                                    )
                                                }
                                            }
                                        } else {
                                            openWithFile(hit.entry)
                                        }
                                        searchMode = false
                                        query = ""
                                    }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                                )
                            }
                        }
                    }
                } else if (visibleEntries.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            EmptyState(
                                modifier = Modifier.fillParentMaxSize(),
                                filtered = query.isNotBlank(),
                                onBrowseDownloads = {
                                    scope.launch {
                                        controller.openStorageRoot(
                                            File(
                                                Environment.getExternalStorageDirectory(),
                                                "Download"
                                            ),
                                            "Download"
                                        )
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = 8.dp,
                            end = 12.dp,
                            bottom = if (selectedCount > 0) 82.dp else 8.dp
                        )
                    ) {
                        if (!searchMode &&
                            appSettings.showRecentExtractions &&
                            controller.path.size <= 1 &&
                            recentExtracts.isNotEmpty()
                        ) {
                            item {
                                RecentExtractionStrip(
                                    entries = recentExtracts,
                                    onOpen = { entry -> openHistoryLocation(entry) }
                                )
                            }
                        }
                        if (visibleEntries.size > entryPageSize) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    androidx.compose.material3.TextButton(
                                        onClick = { entryPageSize += 300 }
                                    ) {
                                        Text(
                                            "再显示 300 项（剩余 ${
                                                visibleEntries.size - entryPageSize
                                            }）"
                                        )
                                    }
                                }
                            }
                        }
                        val shownEntries = visibleEntries.take(entryPageSize)
                        items(shownEntries, key = { it.uri.toString() }) { entry ->
                            EntryRow(
                                entry = entry,
                                selectionMode = controller.selectionMode,
                                selected = entry.uri in controller.selected,
                                showThumbnail = appSettings.showThumbnails,
                                showArchivePreview = transferSession == null,
                                onClick = {
                                    when {
                                        transferBusy -> Unit
                                        transferSession != null -> {
                                            if (entry.isDirectory) {
                                                scope.launch { controller.open(entry) }
                                            }
                                        }
                                        controller.selectionMode ->
                                            controller.toggleSelection(entry.uri)
                                        entry.isDirectory ->
                                            scope.launch { controller.open(entry) }
                                        entry.isArchive -> extractFile(entry)
                                        else -> openWithFile(entry)
                                    }
                                },
                                onPreviewArchive = {
                                    if (transferSession == null && !transferBusy) {
                                        onOpenArchive(entry)
                                    }
                                },
                                onExtractFile = if (
                                    !entry.isDirectory &&
                                    !entry.isArchive &&
                                    transferSession == null
                                ) {
                                    { extractFile(entry) }
                                } else {
                                    null
                                },
                                onLongClick = {
                                    if (transferSession == null && !transferBusy) {
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                        controller.startSelection(entry.uri)
                                    }
                                },
                                onCheckedChange = {
                                    if (transferSession == null && !transferBusy) {
                                        controller.toggleSelection(entry.uri)
                                    }
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                            )
                        }
                    }
                }
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
                Button(
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
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
internal fun TaskCenterContent(
    modifier: Modifier = Modifier,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pausedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var history by remember { mutableStateOf<List<TaskHistoryEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            tasks = runCatching { loadArchiveWorkInfos(context) }.getOrDefault(emptyList())
            pausedTaskIds = tasks
                .filter { ArchiveTaskControl.isPaused(context, it.id) }
                .mapTo(mutableSetOf()) { it.id.toString() }
            history = runCatching {
                TaskHistoryStore(context.applicationContext).list()
            }.getOrDefault(emptyList())
            loading = false
            delay(1000)
        }
    }

    val runningStates = setOf(
        WorkInfo.State.RUNNING,
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.BLOCKED
    )
    val activeTasks = tasks.filter { it.state in runningStates }
    val finishedTasks = tasks.filterNot { it.state in runningStates }

    if (loading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (tasks.isEmpty()) {
        Column(
            modifier = modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text("还没有任务", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "压缩或解压后会出现在这里，可以暂停、继续或取消",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeTasks.isNotEmpty()) {
                item { TaskSectionLabel("进行中", "${activeTasks.size} 项") }
                items(activeTasks, key = { "active-${it.id}" }) { task ->
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
                        },
                        onCancel = {
                            ArchiveTaskControl.clear(context, task.id)
                            WorkManager.getInstance(context.applicationContext)
                                .cancelWorkById(task.id)
                            onNotify("已取消任务")
                        }
                    )
                }
            }
            if (finishedTasks.isNotEmpty()) {
                item { TaskSectionLabel("已结束", "${finishedTasks.size} 项") }
                items(finishedTasks, key = { "done-${it.id}" }) { task ->
                    TaskQueueRow(
                        task = task,
                        paused = false,
                        onTogglePause = {},
                        onResumeTimedOut = {
                            val remembered = ExtractionTaskRegistry.find(task.id)
                            if (remembered != null) {
                                ExtractionScheduler.enqueue(context.applicationContext, remembered)
                                onNotify("已重新加入队列")
                            } else {
                                onNotify("任务参数已释放，请重新选择文件解压")
                            }
                        }
                    )
                }
            }
            if (history.isNotEmpty()) {
                item { TaskSectionLabel("历史", "${history.size} 条") }
                items(history, key = { "history-${it.id}" }) { entry ->
                    TaskHistoryRow(
                        entry = entry,
                        onOpen = {
                            entry.locationUri?.let { uri ->
                                val intent = android.content.Intent(
                                    context,
                                    com.decompress.app.MainActivity::class.java
                                ).apply {
                                    action = TaskNavigation.ACTION_OPEN_LOCATION
                                    putExtra(TaskNavigation.EXTRA_LOCATION_URI, uri)
                                    putExtra(TaskNavigation.EXTRA_LOCATION_NAME, entry.locationName)
                                    putExtra(TaskNavigation.EXTRA_LOCATION_TREE_URI, entry.treeUri)
                                    putExtra(TaskNavigation.EXTRA_LOCATION_PATH, entry.path)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onRetry = {
                            val queued = entry.retryPayload?.let { payload ->
                                when (entry.kind) {
                                    "extract" -> TaskHistoryCodec.decodeExtraction(payload)
                                        ?.let {
                                            ExtractionScheduler.enqueue(
                                                context.applicationContext,
                                                it
                                            )
                                        }
                                    "compress" -> TaskHistoryCodec.decodeCompression(payload)
                                        ?.let {
                                            CompressionScheduler.enqueue(
                                                context.applicationContext,
                                                it
                                            )
                                        }
                                    else -> null
                                }
                            }
                            if (queued != null) {
                                onNotify("已重新加入队列")
                            } else {
                                onNotify("任务参数已失效，请重新发起")
                            }
                        },
                        onDelete = {
                            TaskHistoryStore(context.applicationContext).delete(entry.id)
                            history = history.filterNot { it.id == entry.id }
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (finishedTasks.isNotEmpty()) {
                TextButton(
                    onClick = {
                        WorkManager.getInstance(context.applicationContext).pruneWork()
                    }
                ) {
                    Text("清理历史")
                }
            }
            if (activeTasks.isNotEmpty()) {
                TextButton(
                    onClick = {
                        WorkManager.getInstance(context.applicationContext).apply {
                            cancelUniqueWork(ExtractionScheduler.QUEUE_NAME)
                            cancelUniqueWork(CompressionScheduler.QUEUE_NAME)
                        }
                        onNotify("已取消全部任务")
                    }
                ) {
                    Text("取消全部", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun RecentExtractionStrip(
    entries: List<TaskHistoryEntry>,
    onOpen: (TaskHistoryEntry) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            "最近解压",
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .width(190.dp)
                        .clickable { onOpen(entry) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            entry.locationName.ifBlank { "结果文件夹" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
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
                                if (isCompression) Icons.Outlined.Unarchive
                                else Icons.Outlined.Archive,
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
    onTogglePause: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onResumeTimedOut: (() -> Unit)? = null
) {
    val title = task.tags
        .firstOrNull { it.startsWith(ExtractionScheduler.TAG_NAME_PREFIX) }
        ?.removePrefix(ExtractionScheduler.TAG_NAME_PREFIX)
        ?: "后台任务"
    val foregroundTimeout =
        task.stopReason == WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT
    val message = when {
        foregroundTimeout -> "系统限制了后台运行时长，点「继续」接着解压"
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
                if (foregroundTimeout && onResumeTimedOut != null) {
                    TextButton(onClick = onResumeTimedOut) {
                        Text("继续")
                    }
                }
                if (active) {
                    if (onCancel != null) {
                        IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "取消任务",
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
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
    showArchivePreview: Boolean,
    onClick: () -> Unit,
    onPreviewArchive: () -> Unit,
    onExtractFile: (() -> Unit)?,
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
        } else if (entry.isArchive && showArchivePreview) {
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
        } else if (onExtractFile != null) {
            IconButton(onClick = onExtractFile) {
                Icon(
                    Icons.Outlined.Archive,
                    contentDescription = "解压 ${entry.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickAccessTopTitle(
    directStorage: Boolean,
    folderName: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(enabled = directStorage, onClick = onClick)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (directStorage) "快速访问" else folderName.ifBlank { "文件" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (directStorage) {
            Spacer(Modifier.size(2.dp))
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) {
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

@Composable
private fun StorageQuickLocationPanel(
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
            shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp,
            tonalElevation = 0.dp
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
                if (locations.isEmpty()) {
                    Text(
                        "暂无快捷位置，可在设置中新建",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
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
private fun StorageExternalQuickLocation(
    storage: ExternalStorageLocation,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                modifier = Modifier.size(21.dp)
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
private fun StorageQuickLocationAdd(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
            Text(
                "新增",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StorageQuickLocation(
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
        shape = RoundedCornerShape(12.dp),
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                modifier = Modifier.size(21.dp)
            )
            Text(
                location.label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun discoverExternalStorageLocations(
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
        val baseLabel = label.ifBlank { "外部存储" }
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
            if (index == 0) "外部存储" else "外部存储 $index"
        )
    }
    return roots.entries
        .map { (path, label) -> ExternalStorageLocation(label, path) }
        .sortedBy { it.label.lowercase() }
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
    deniedOnce: Boolean,
    onRequestAccess: () -> Unit,
    onChooseFolder: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "访问手机文件",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "解压、压缩和整理文件都在这台手机上完成，过程中不需要联网。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        OnboardingBenefit("打开所有文件夹，不用每次选目录")
        OnboardingBenefit("只在本机读写，不联网、不上传")
        OnboardingBenefit("随时可以在系统设置里撤销")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestAccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("授予所有文件访问权限")
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onChooseFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("只选一个文件夹（更保守）")
        }
        if (deniedOnce) {
            Spacer(Modifier.height(6.dp))
            Text(
                "上次没有授权成功。可以再试一次，或先用文件夹模式——解压、压缩、搜索都能用。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Zippest 不申请网络权限，可在 系统设置 → 应用 → Zippest → 权限 里核实。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingBenefit(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GlobalSearchRow(hit: GlobalSearchHit, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (hit.entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = if (hit.entry.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                hit.entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                hit.parentPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (hit.entry.isDirectory) {
                "文件夹"
            } else {
                Formatter.formatShortFileSize(context, hit.entry.size)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun batchNameFor(entry: BrowserEntry, index: Int, pattern: String): String {
    val name = entry.name
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0) name.substring(0, dot) else name
    val extension = if (dot > 0) name.substring(dot) else ""
    val extOnly = extension.removePrefix(".")
    val now = java.util.Date(System.currentTimeMillis())
    val locale = java.util.Locale.US
    var usedExtensionToken = false
    val result = Regex("\\{(name|ext|n|num|date|time|\\d+)\\}").replace(pattern) { match ->
        when (val token = match.groupValues[1]) {
            "name" -> base
            "ext" -> {
                usedExtensionToken = true
                extOnly
            }
            "n", "num" -> index.toString()
            "date" -> java.text.SimpleDateFormat("yyyyMMdd", locale).format(now)
            "time" -> java.text.SimpleDateFormat("HHmmss", locale).format(now)
            else -> "%0${token.length}d".format(index)
        }
    }
    return if (usedExtensionToken || extension.isEmpty()) result else result + extension
}

@Composable
private fun EmptyState(
    modifier: Modifier,
    filtered: Boolean,
    onBrowseDownloads: (() -> Unit)? = null
) {
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
        if (!filtered && onBrowseDownloads != null) {
            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.OutlinedButton(onClick = onBrowseDownloads) {
                Text("去下载目录看看")
            }
        }
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

internal fun selectedFileSuffix(value: TextFieldValue): String {
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
