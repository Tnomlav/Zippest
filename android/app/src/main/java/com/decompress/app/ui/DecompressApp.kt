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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.decompress.app.R
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
import com.decompress.app.data.PendingPasswordRequest
import com.decompress.app.data.PendingPasswordStore
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
import com.decompress.app.service.ExtractionTask
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

internal data class TransferSession(
    val sources: List<BrowserEntry>,
    val move: Boolean,
    val sourceLocation: FileBrowserController.LocationSnapshot
)

internal data class ExternalStorageLocation(
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
    var pendingPasswordRequest by remember { mutableStateOf<PendingPasswordRequest?>(null) }
    var pendingPasswordInput by remember { mutableStateOf("") }
    var showSettings by rememberSaveable { mutableStateOf(false) }
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
            parentPath = UiStrings.get(R.string.app_external),
            parentName = UiStrings.get(R.string.app_external),
            requiresCustomDestination = true
        )
    }

    LaunchedEffect(Unit) {
        pendingPasswordRequest = PendingPasswordStore.all(context).firstOrNull()
    }

    fun enqueuePendingPassword(request: PendingPasswordRequest, password: String) {
        runCatching {
            ExtractionScheduler.enqueue(
                context,
                ExtractionTask(
                    archiveUris = listOf(Uri.parse(request.archiveUri)),
                    archiveNames = listOf(request.archiveName),
                    parentUri = request.parentUri?.let(Uri::parse),
                    parentTreeUri = request.parentTreeUri?.let(Uri::parse),
                    mode = request.mode,
                    customTreeUri = request.customTreeUri?.let(Uri::parse),
                    customRootUri = request.customRootUri?.let(Uri::parse),
                    createFolder = request.createFolder,
                    conflictPolicy = request.conflictPolicy,
                    encoding = request.encoding,
                    passwords = listOf(password),
                    parentPath = request.parentPath,
                    parentName = request.parentName,
                    entryPaths = request.entryPaths
                )
            )
        }
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
                val message = finishedTaskName ?: if (successful) UiStrings.get(R.string.app_task_done) else UiStrings.get(R.string.app_task_failed)
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
                if (intent?.action == ExtractionScheduler.ACTION_EXTRACTION_FINISHED) {
                    context?.let { ctx ->
                        pendingPasswordRequest = PendingPasswordStore.all(ctx).firstOrNull()
                    }
                }
                scope.launch {
                    controller.refresh()
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = if (location != null) UiStrings.get(R.string.app_open) else null,
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
                title = { Text(UiStrings.get(R.string.app_crashed)) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            UiStrings.get(R.string.app_crash_hint),
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
                            ClipData.newPlainText(UiStrings.get(R.string.app_crash_info), crashReport!!)
                        )
                        com.decompress.app.data.CrashLog.clear(context)
                        crashReport = null
                        scope.launch { snackbarHostState.showSnackbar(UiStrings.get(R.string.app_crash_copied)) }
                    }) {
                        Text(UiStrings.get(R.string.app_copy_details))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        com.decompress.app.data.CrashLog.clear(context)
                        crashReport = null
                    }) {
                        Text(
                            UiStrings.get(R.string.app_close),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
        pendingPasswordRequest?.let { request ->
            AlertDialog(
                onDismissRequest = { pendingPasswordRequest = null },
                title = { Text(UiStrings.get(R.string.app_need_password)) },
                text = {
                    Column {
                        Text(
                            request.archiveName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            UiStrings.get(R.string.app_need_password_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pendingPasswordInput,
                            onValueChange = { pendingPasswordInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(UiStrings.get(R.string.app_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = pendingPasswordInput.isNotEmpty(),
                        onClick = {
                            enqueuePendingPassword(request, pendingPasswordInput)
                            PendingPasswordStore.remove(context, request.id)
                            pendingPasswordInput = ""
                            pendingPasswordRequest = PendingPasswordStore.all(context).firstOrNull()
                        }
                    ) {
                        Text(UiStrings.get(R.string.app_start_extract))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // 只收起，不丢请求：下次进应用或任务结束时还会再问。
                        pendingPasswordRequest = null
                        pendingPasswordInput = ""
                    }) {
                        Text(UiStrings.get(R.string.app_later))
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
                                snackbarHostState.showSnackbar(UiStrings.get(R.string.app_queued))
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