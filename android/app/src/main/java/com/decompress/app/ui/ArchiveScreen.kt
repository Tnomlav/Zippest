@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.decompress.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.decompress.app.data.ArchiveItem
import com.decompress.app.data.ArchiveNode
import com.decompress.app.data.AppSettings
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.ConflictPolicy
import com.decompress.app.data.DefaultDestinationMode
import com.decompress.app.data.DestinationMode
import com.decompress.app.data.FileNameEncoding
import com.decompress.app.data.MultiArchiveEngine
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.service.ExtractionTask
import kotlinx.coroutines.launch

@Composable
fun ArchiveScreen(
    target: ArchiveTarget,
    onBack: () -> Unit,
    onExtract: (BrowserEntry, String) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { MultiArchiveEngine(context.applicationContext) }

    var items by remember { mutableStateOf<List<ArchiveItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentPath by rememberSaveable { mutableStateOf("") }
    var archivePassword by rememberSaveable(target.archive.uri.toString()) {
        mutableStateOf("")
    }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    LaunchedEffect(target.archive.uri, archivePassword) {
        loading = true
        error = null
        if (!engine.supports(target.archive.name)) {
            error = "暂不支持此格式。已接入 ZIP / 7Z / RAR / TAR / ZST / BR / " +
                "GZ / BZ2 / XZ / LZMA / LZ4 / CPIO"
            loading = false
            return@LaunchedEffect
        }
        runCatching {
            engine.list(
                archiveUri = target.archive.uri,
                archiveName = target.archive.name,
                password = archivePassword,
                archiveTreeUri = target.parentTreeUri,
                archiveParentUri = target.parentUri
            )
        }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: "无法读取压缩包" }
        loading = false
    }

    fun notify(message: String) {
        onNotify(message)
    }

    fun testArchive() {
        if (testing || loading) return
        scope.launch {
            testing = true
            runCatching {
                engine.test(
                    archiveUri = target.archive.uri,
                    archiveName = target.archive.name,
                    password = archivePassword,
                    onProgress = { _, _ -> },
                    archiveTreeUri = target.parentTreeUri,
                    archiveParentUri = target.parentUri
                )
            }.onSuccess { count ->
                notify("压缩包测试通过 · $count 项")
            }.onFailure {
                notify(it.message ?: "压缩包测试失败")
            }
            testing = false
        }
    }

    BackHandler {
        if (currentPath.isNotEmpty()) {
            currentPath = parentArchivePath(currentPath)
        } else {
            onBack()
        }
    }

    val nodes = remember(items, currentPath) {
        archiveChildren(items, currentPath)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPath.isNotEmpty()) {
                            currentPath = parentArchivePath(currentPath)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Column {
                        Text(
                            target.archive.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (currentPath.isEmpty()) "压缩包根目录" else currentPath.trimEnd('/'),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        passwordInput = archivePassword
                        passwordVisible = false
                        showPasswordDialog = true
                    }) {
                        Icon(
                            if (archivePassword.isEmpty()) {
                                Icons.Outlined.Lock
                            } else {
                                Icons.Outlined.LockOpen
                            },
                            contentDescription = "压缩包密码"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (error == null) {
                ArchiveBottomBar(
                    onExtract = { onExtract(target.archive, archivePassword) },
                    onTest = ::testArchive,
                    testing = testing,
                    onInfo = {
                        notify(
                            "${target.archive.extension.uppercase()} · ${items.size} 项 · " +
                                Formatter.formatShortFileSize(context, target.archive.size)
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                error != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        passwordInput = archivePassword
                        passwordVisible = false
                        showPasswordDialog = true
                    }) {
                        Text("输入密码")
                    }
                }
                nodes.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("压缩包为空", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 54.dp,
                        end = 12.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(nodes, key = { it.path }) { node ->
                        ArchiveRow(
                            node = node,
                            onClick = {
                                if (node.isDirectory) {
                                    currentPath = node.path
                                } else {
                                    onExtract(target.archive, archivePassword)
                                }
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "仅预览 · 解压后才能使用这些文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            DreamPreviewBorder(
                modifier = Modifier.matchParentSize()
            )
            EdgeSwipeBackZones(
                onBack = {
                    if (currentPath.isNotEmpty()) {
                        currentPath = parentArchivePath(currentPath)
                    } else {
                        onBack()
                    }
                }
            )
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("输入压缩包密码") },
            text = {
                Column {
                    Text(
                        target.archive.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("密码") },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                passwordVisible = !passwordVisible
                            }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff
                                    else Icons.Outlined.Visibility,
                                    contentDescription =
                                        if (passwordVisible) "隐藏密码" else "显示密码"
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        archivePassword = passwordInput
                        showPasswordDialog = false
                    }
                ) {
                    Text("解锁")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DreamPreviewBorder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "preview-border")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "preview-border-glow"
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier.padding(3.dp)) {
        val stroke = 2.dp.toPx()
        val inset = stroke / 2f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.28f + glow * 0.55f),
                    secondary.copy(alpha = 0.45f + glow * 0.35f),
                    tertiary.copy(alpha = 0.32f + glow * 0.5f),
                    primary.copy(alpha = 0.28f + glow * 0.55f)
                )
            ),
            topLeft = Offset(inset, inset),
            size = Size(
                width = size.width - stroke,
                height = size.height - stroke
            ),
            cornerRadius = CornerRadius(18.dp.toPx()),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
private fun ArchiveBottomBar(
    onExtract: () -> Unit,
    onTest: () -> Unit,
    testing: Boolean,
    onInfo: () -> Unit
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
            ArchiveAction(Icons.Outlined.Unarchive, "解压", true, onExtract)
            ArchiveAction(
                Icons.Outlined.CheckCircle,
                if (testing) "测试中" else "测试",
                false,
                onTest,
                enabled = !testing
            )
            ArchiveAction(Icons.Outlined.Info, "信息", false, onInfo)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ArchiveAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .combinedClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                accent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                accent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun ArchiveRow(node: ArchiveNode, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val background = if (node.isDirectory) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
        val tint = if (node.isDirectory) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (node.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(23.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                node.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!node.isDirectory) {
                Spacer(Modifier.height(3.dp))
                Text(
                    Formatter.formatShortFileSize(
                        context,
                        node.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (node.isDirectory) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExtractSheet(
    target: ExtractTarget,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onExtractionStarted: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { MultiArchiveEngine(context.applicationContext) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var password by rememberSaveable(
        target.archives.firstOrNull()?.uri,
        target.initialPassword
    ) {
        mutableStateOf(
            target.initialPassword.ifBlank {
                if (settings.fillPasswordFromClipboard) {
                    readClipboard(clipboardManager, context).orEmpty()
                } else {
                    ""
                }
            }
        )
    }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var clipboardText by remember { mutableStateOf(readClipboard(clipboardManager, context)) }
    var clipboardExpanded by remember { mutableStateOf(false) }
    var destinationMode by rememberSaveable {
        mutableStateOf(
            when {
                target.requiresCustomDestination -> DestinationMode.CUSTOM
                settings.defaultDestinationMode == DefaultDestinationMode.CURRENT_FOLDER ->
                    DestinationMode.CURRENT_FOLDER
                settings.defaultDestinationMode == DefaultDestinationMode.CUSTOM ->
                    DestinationMode.CUSTOM
                else -> DestinationMode.NEW_FOLDER
            }
        )
    }
    var customTreeUri by remember {
        mutableStateOf(settings.customDestinationTreeUri?.let(Uri::parse))
    }
    var customRootUri by remember {
        mutableStateOf(settings.customDestinationRootUri?.let(Uri::parse))
    }
    var customName by remember {
        mutableStateOf(settings.customDestinationName)
    }
    var conflictPolicy by rememberSaveable {
        mutableStateOf(settings.defaultConflictPolicy)
    }
    var encoding by rememberSaveable {
        mutableStateOf(settings.defaultEncoding)
    }
    var samePassword by rememberSaveable { mutableStateOf(false) }
    var perArchivePasswords by remember(target.archives) {
        mutableStateOf(List(target.archives.size) { "" })
    }
    var passwordDialogIndex by remember { mutableStateOf<Int?>(null) }
    var passwordDialogValue by remember { mutableStateOf("") }
    var pendingPasswords by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val customPicker = rememberLauncherForActivityResult(
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
            customTreeUri = uri
            customRootUri = treeRootDocumentUri(uri)
            customName = queryDisplayName(context, uri)
            destinationMode = DestinationMode.CUSTOM
        }
    }

    val launchExtraction: (List<String>) -> Unit = { passwords ->
        if (destinationMode == DestinationMode.CUSTOM && customTreeUri == null) {
            error = "请先选择自定义位置"
        } else {
            val unsupported = target.archives.firstOrNull { !engine.supports(it.name) }
            if (unsupported != null) {
                error = "暂不支持 ${unsupported.name}"
            } else {
                error = null
                val parentUri = if (destinationMode == DestinationMode.CUSTOM) {
                    customRootUri
                } else {
                    target.parentUri
                }
                val treeUri = if (destinationMode == DestinationMode.CUSTOM) {
                    customTreeUri
                } else {
                    target.parentTreeUri
                }
                if (parentUri == null || treeUri == null) {
                    error = "目标位置不可用"
                } else {
                    runCatching {
                        ExtractionScheduler.enqueue(
                            context,
                            ExtractionTask(
                                archiveUris = target.archives.map { it.uri },
                                archiveNames = target.archives.map { it.name },
                                parentUri = parentUri,
                                parentTreeUri = treeUri,
                                mode = destinationMode,
                                customTreeUri = customTreeUri,
                                customRootUri = customRootUri,
                                createFolder =
                                    destinationMode != DestinationMode.CURRENT_FOLDER,
                                conflictPolicy = conflictPolicy,
                                encoding = encoding,
                                passwords = passwords
                            )
                        )
                        onExtractionStarted()
                    }.onFailure {
                        error = it.message ?: "无法创建后台任务"
                    }
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pendingPasswords?.let { passwords ->
            pendingPasswords = null
            launchExtraction(passwords)
        }
    }

    fun requestPermissionAndLaunch(passwords: List<String>) {
        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingPasswords = passwords
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchExtraction(passwords)
        }
    }

    fun startExtraction() {
        if (destinationMode == DestinationMode.CUSTOM && customTreeUri == null) {
            error = "请先选择自定义位置"
            return
        }
        val unsupported = target.archives.firstOrNull { !engine.supports(it.name) }
        if (unsupported != null) {
            error = "暂不支持 ${unsupported.name}"
            return
        }
        error = null
        if (target.archives.size > 1 && !samePassword) {
            if (perArchivePasswords.size != target.archives.size) {
                perArchivePasswords = List(target.archives.size) { "" }
            }
            passwordDialogValue = ""
            passwordDialogIndex = 0
        } else {
            requestPermissionAndLaunch(target.archives.map { password })
        }
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
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "密码",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("压缩包加密时填写") },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val latest =
                                            readClipboard(clipboardManager, context)
                                        clipboardText = latest
                                        if (latest != null) password = latest
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    clipboardText ?: "剪贴板为空",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardText = readClipboard(
                                        clipboardManager,
                                        context
                                    )
                                    clipboardExpanded = true
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "打开剪贴板",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = clipboardExpanded,
                        onDismissRequest = { clipboardExpanded = false }
                    ) {
                        if (clipboardText != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        clipboardText.orEmpty(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    password = clipboardText.orEmpty()
                                    clipboardExpanded = false
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("剪贴板为空") },
                                onClick = { clipboardExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "解压到",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                DestinationChoice(
                    title = "当前文件夹",
                    detail = target.parentPath.ifBlank {
                        "请先通过“选择其他位置”授权保存目录"
                    },
                    selected = destinationMode == DestinationMode.CURRENT_FOLDER,
                    onClick = { destinationMode = DestinationMode.CURRENT_FOLDER }
                )
                Spacer(Modifier.height(8.dp))
                DestinationChoice(
                    title = "新建同名文件夹",
                    detail = if (target.archives.size == 1) {
                        listOf(
                            target.parentPath,
                            archiveBaseName(target.archives.first().name)
                        ).filter { it.isNotBlank() }.joinToString(" / ")
                    } else {
                        listOf(target.parentPath, "每个压缩包同名文件夹")
                            .filter { it.isNotBlank() }
                            .joinToString(" / ")
                    },
                    selected = destinationMode == DestinationMode.NEW_FOLDER,
                    onClick = { destinationMode = DestinationMode.NEW_FOLDER }
                )
                Spacer(Modifier.height(8.dp))
                DestinationChoice(
                    title = "选择其他位置",
                    detail = customName ?: "选择手机中的任意文件夹",
                    selected = destinationMode == DestinationMode.CUSTOM && customTreeUri != null,
                    onClick = { customPicker.launch(null) }
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    "同名文件",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConflictPolicy.entries.forEach { policy ->
                        FilterChip(
                            selected = conflictPolicy == policy,
                            onClick = { conflictPolicy = policy },
                            label = { Text(policy.label) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "文件名编码",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileNameEncoding.entries.forEach { item ->
                        FilterChip(
                            selected = encoding == item,
                            onClick = { encoding = item },
                            label = { Text(item.label) }
                        )
                    }
                }

                if (target.archives.size > 1) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { samePassword = !samePassword }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = samePassword,
                            onCheckedChange = { samePassword = it }
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                "使用相同密码",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (samePassword) {
                                    "本次选择的 ${target.archives.size} 个压缩包复用此密码"
                                } else {
                                    "关闭时，将逐个输入密码"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { startExtraction() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("开始解压")
                }
            }
        }
    }

    passwordDialogIndex?.let { index ->
        val archive = target.archives.getOrNull(index)
        if (archive != null) {
            AlertDialog(
                onDismissRequest = { passwordDialogIndex = null },
                title = { Text("输入第 ${index + 1}/${target.archives.size} 个密码") },
                text = {
                    Column {
                        Text(
                            archive.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "压缩包没有密码时可留空。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = passwordDialogValue,
                            onValueChange = { passwordDialogValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("密码") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val updated = perArchivePasswords.toMutableList().also {
                                it[index] = passwordDialogValue
                            }
                            perArchivePasswords = updated
                            val next = index + 1
                            if (next < target.archives.size) {
                                passwordDialogValue = ""
                                passwordDialogIndex = next
                            } else {
                                passwordDialogIndex = null
                                requestPermissionAndLaunch(updated)
                            }
                        }
                    ) {
                        Text(if (index + 1 == target.archives.size) "开始解压" else "下一个")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { passwordDialogIndex = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun DestinationChoice(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun archiveChildren(
    items: List<ArchiveItem>,
    prefix: String
): List<ArchiveNode> {
    val map = linkedMapOf<String, ArchiveNode>()
    items.forEach { item ->
        val path = item.path.replace('\\', '/').trimStart('/')
        if (!path.startsWith(prefix)) return@forEach
        val relative = path.removePrefix(prefix)
        if (relative.isBlank()) return@forEach
        val first = relative.substringBefore('/')
        val isDirectory = relative.contains('/') || item.isDirectory
        val fullPath = prefix + first + if (isDirectory) "/" else ""
        val existing = map[first]
        if (existing == null || (!existing.isDirectory && isDirectory)) {
            map[first] = ArchiveNode(
                name = first,
                path = fullPath,
                isDirectory = isDirectory,
                size = if (isDirectory) 0 else item.size
            )
        }
    }
    return map.values.sortedWith(
        compareByDescending<ArchiveNode> { it.isDirectory }
            .thenBy { it.name.lowercase() }
    )
}

private fun parentArchivePath(path: String): String {
    val trimmed = path.trimEnd('/')
    val index = trimmed.lastIndexOf('/')
    return if (index >= 0) trimmed.substring(0, index + 1) else ""
}

private fun archiveBaseName(name: String): String = name.substringBeforeLast('.', name)

private fun readClipboard(manager: ClipboardManager, context: Context): String? {
    val clip: ClipData = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.ifBlank { null }
}

internal fun treeRootDocumentUri(treeUri: Uri): Uri {
    val treeId = DocumentsContract.getTreeDocumentId(treeUri)
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
}

internal fun queryDisplayName(context: Context, treeUri: Uri): String {
    val root = treeRootDocumentUri(treeUri)
    context.contentResolver.query(
        root,
        arrayOf(Document.COLUMN_DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(0) ?: "所选文件夹"
        }
    }
    return "所选文件夹"
}
