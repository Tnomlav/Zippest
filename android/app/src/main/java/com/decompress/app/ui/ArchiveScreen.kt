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
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.decompress.app.data.ArchiveItem
import com.decompress.app.ui.theme.ZippestRadius
import com.decompress.app.ui.theme.ZippestSpacing
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ArchiveScreen(
    target: ArchiveTarget,
    onBack: () -> Unit,
    onExtract: (BrowserEntry, String, List<String>, FileNameEncoding) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { MultiArchiveEngine(context.applicationContext) }

    var items by remember { mutableStateOf<List<ArchiveItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentPath by rememberSaveable { mutableStateOf("") }
    val archiveListStates = remember { mutableMapOf<String, LazyListState>() }
    val archiveListState = archiveListStates.getOrPut(currentPath) { LazyListState() }
    var archivePassword by rememberSaveable(target.archive.uri.toString()) {
        mutableStateOf("")
    }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var openingEntry by remember { mutableStateOf(false) }
    var encoding by rememberSaveable { mutableStateOf(FileNameEncoding.AUTO) }
    var showEncodingDialog by remember { mutableStateOf(false) }
    var archiveQuery by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(target.archive.uri, archivePassword, encoding) {
        loading = true
        error = null
        items = emptyList()
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
                archiveParentUri = target.parentUri,
                encoding = encoding,
                onBatch = { batch, _ ->
                    if (batch.isNotEmpty()) {
                        items = items + batch
                    }
                }
            )
        }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: "无法读取压缩包" }
        loading = false
    }

    fun notify(message: String) {
        onNotify(message)
    }

    fun toggleSelection(path: String) {
        selectedPaths = if (path in selectedPaths) {
            selectedPaths - path
        } else {
            selectedPaths + path
        }
    }

    /** 把一个包内文件取到缓存，再用系统应用打开；只解这一个条目。 */
    fun openEntry(node: ArchiveNode) {
        if (openingEntry || node.isDirectory) return
        scope.launch {
            openingEntry = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val previewRoot = File(context.cacheDir, "preview").apply { mkdirs() }
                    val now = System.currentTimeMillis()
                    previewRoot.listFiles()?.forEach { stale ->
                        if (now - stale.lastModified() > 24L * 60 * 60 * 1000) {
                            stale.deleteRecursively()
                        }
                    }
                    val workDir = File(previewRoot, "entry-$now").apply { mkdirs() }
                    engine.extract(
                        archiveUri = target.archive.uri,
                        archiveName = target.archive.name,
                        settings = com.decompress.app.data.ExtractSettings(
                            mode = DestinationMode.CURRENT_FOLDER,
                            targetParentUri = Uri.fromFile(workDir),
                            targetTreeUri = Uri.fromFile(workDir),
                            createFolder = false,
                            conflictPolicy = ConflictPolicy.OVERWRITE,
                            encoding = FileNameEncoding.AUTO,
                            password = archivePassword,
                            entryFilter = { it == node.path }
                        ),
                        onProgress = { _, _ -> },
                        archiveTreeUri = target.parentTreeUri,
                        archiveParentUri = target.parentUri
                    )
                    workDir.walkTopDown()
                        .firstOrNull { it.isFile && it.name == node.name }
                        ?: throw IllegalStateException("未能取出该文件")
                }
            }.onSuccess { extracted ->
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    extracted
                )
                val mime = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extracted.extension.lowercase())
                    ?: "*/*"
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    )
                }.onFailure { notify("没有可以打开该类型的应用") }
            }.onFailure { notify(it.message ?: "无法打开该文件") }
            openingEntry = false
        }
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
        when {
            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
            currentPath.isNotEmpty() -> currentPath = parentArchivePath(currentPath)
            else -> onBack()
        }
    }

    LaunchedEffect(currentPath) {
        selectedPaths = emptySet()
    }

    val nodes = remember(items, currentPath) {
        archiveChildren(items, currentPath)
    }
    val searchResults = remember(items, archiveQuery) {
        val keyword = archiveQuery.trim()
        if (keyword.isBlank()) {
            emptyList()
        } else {
            items.filter { it.path.contains(keyword, ignoreCase = true) }
                .take(500)
                .map { item ->
                    ArchiveNode(
                        name = item.path.trimStart('/'),
                        path = item.path.trimStart('/'),
                        isDirectory = item.isDirectory,
                        size = item.size
                    )
                }
        }
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
                    IconButton(onClick = { showEncodingDialog = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                    }
                    IconButton(onClick = { searchOpen = !searchOpen }) {
                        Icon(Icons.Outlined.Search, contentDescription = "搜索压缩包内容")
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
                    selectionCount = selectedPaths.size,
                    onExtractAll = {
                        onExtract(target.archive, archivePassword, emptyList(), encoding)
                    },
                    onExtractSelected = {
                        onExtract(
                            target.archive,
                            archivePassword,
                            selectedPaths.toList(),
                            encoding
                        )
                    },
                    onClearSelection = { selectedPaths = emptySet() },
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
            Row(modifier = Modifier.fillMaxSize()) {
                if (rememberZippestWindowSize() != ZippestWindowSize.COMPACT) {
                    ArchivePathPane(
                        currentPath = currentPath,
                        onOpen = { currentPath = it },
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when {
                searchOpen -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = archiveQuery,
                        onValueChange = { archiveQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("搜索包内文件") },
                        trailingIcon = {
                            if (archiveQuery.isNotEmpty()) {
                                IconButton(onClick = { archiveQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "清空")
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        archiveQuery.isBlank() -> Text(
                            "输入关键字，搜索整个压缩包",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        searchResults.isEmpty() -> Text(
                            "没有匹配的文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> LazyColumn {
                            items(searchResults, key = { it.path }) { node ->
                                ArchiveRow(
                                    modifier = animateItemRespectingMotion(),
                                    node = node,
                                    selected = false,
                                    onClick = {
                                        if (!node.isDirectory) openEntry(node)
                                    },
                                    onLongClick = {}
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.32f
                                    )
                                )
                            }
                        }
                    }
                }
                loading && items.isEmpty() -> ArchiveSkeleton()
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
                nodes.isEmpty() -> StatePanel(modifier = Modifier.fillMaxSize()) {
                    EmptyIllustration(EmptyArt.FOLDER)
                    Text(
                        "压缩包是空的",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "换一个包，或先确认下载是否完整",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(
                    state = archiveListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 54.dp,
                        end = 12.dp,
                        bottom = 8.dp
                    )
                ) {
                    if (loading) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "正在读取… 已读取 ${items.size} 项",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(nodes, key = { it.path }) { node ->
                        ArchiveRow(
                            modifier = animateItemRespectingMotion(),
                            node = node,
                            selected = node.path in selectedPaths,
                            onClick = {
                                when {
                                    selectedPaths.isNotEmpty() -> toggleSelection(node.path)
                                    node.isDirectory -> currentPath = node.path
                                    else -> openEntry(node)
                                }
                            },
                            onLongClick = {
                                if (!node.isDirectory) toggleSelection(node.path)
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                        )
                    }
                }
            }
                }
                if (rememberZippestWindowSize() == ZippestWindowSize.EXPANDED) {
                    ArchiveEntryPane(
                        archiveName = target.archive.name,
                        sizeText = Formatter.formatShortFileSize(context, target.archive.size),
                        entryCount = items.size,
                        encodingLabel = encoding.label,
                        entry = if (selectedPaths.size == 1) {
                            items.firstOrNull { it.path in selectedPaths }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .padding(padding)
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(ZippestRadius.pill),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (selectedPaths.isEmpty()) {
                            "点按文件直接打开 · 长按可多选解压"
                        } else {
                            "已选 ${selectedPaths.size} 项 · 点按继续选择"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            PreviewBorder(
                modifier = Modifier.matchParentSize()
            )
            EdgeSwipeBackZones(
                onBack = {
                    when {
                        selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
                        currentPath.isNotEmpty() ->
                            currentPath = parentArchivePath(currentPath)
                        else -> onBack()
                    }
                }
            )
        }
    }

    if (showEncodingDialog) {
        AlertDialog(
            onDismissRequest = { showEncodingDialog = false },
            title = { Text("文件名编码") },
            text = {
                Column {
                    Text(
                        "预览里文件名乱码时换一个编码试试；只影响显示和解压后的文件名，不改文件内容。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FileNameEncoding.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    encoding = option
                                    showEncodingDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = encoding == option,
                                onClick = {
                                    encoding = option
                                    showEncodingDialog = false
                                }
                            )
                            Text(
                                option.label,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showEncodingDialog = false }) {
                    Text("关闭")
                }
            }
        )
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
                    Spacer(Modifier.height(12.dp))
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
                Button(
                    onClick = {
                        archivePassword = passwordInput
                        showPasswordDialog = false
                    }
                ) {
                    Text("解锁")
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { showPasswordDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PreviewBorder(modifier: Modifier = Modifier) {
    // 静态 1dp 描边：预览页不需要无限呼吸动画（费电，也和工具气质相反）。
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    Canvas(modifier = modifier.padding(3.dp)) {
        val stroke = 1.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(
                width = size.width - stroke,
                height = size.height - stroke
            ),
            cornerRadius = CornerRadius(ZippestRadius.md.toPx()),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
private fun ArchiveSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            "正在读取压缩包目录…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "条目多的大包会先显示前面的部分",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        repeat(7) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(ZippestRadius.xs))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ArchiveBottomBar(
    selectionCount: Int,
    onExtractAll: () -> Unit,
    onExtractSelected: () -> Unit,
    onClearSelection: () -> Unit,
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
            if (selectionCount > 0) {
                ArchiveAction(Icons.Outlined.Close, "取消", false, onClearSelection)
                ArchiveAction(
                    Icons.Outlined.Archive,
                    "解压所选 $selectionCount",
                    true,
                    onExtractSelected
                )
            } else {
                ArchiveAction(Icons.Outlined.Archive, "全部解压", true, onExtractAll)
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
            .padding(vertical = 8.dp),
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
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
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
private fun ArchiveRow(
    node: ArchiveNode,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    Color.Transparent
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                val kind = if (node.isDirectory) {
                    "文件夹"
                } else {
                    Formatter.formatShortFileSize(context, node.size)
                }
                contentDescription = "${node.name}，$kind"
                if (selected) stateDescription = "已选中"
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileKindIcon(
            kind = com.decompress.app.ui.theme.fileKindOf(node.name, node.isDirectory),
            size = 40.dp
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
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
        if (selected) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary
            )
        } else if (node.isDirectory) {
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
    onRenameArchive: suspend (BrowserEntry, String) -> BrowserEntry?,
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
    var savePassword by remember { mutableStateOf(false) }
    var clipboardText by remember { mutableStateOf(readClipboard(clipboardManager, context)) }
    LaunchedEffect(target.archives) {
        val single = target.archives.singleOrNull() ?: return@LaunchedEffect
        if (password.isEmpty()) {
            val saved = com.decompress.app.data.PasswordVault
                .load(context, single.name)
                .orEmpty()
            if (saved.isNotEmpty()) {
                password = saved
                savePassword = true
            }
        }
    }
    var clipboardExpanded by remember { mutableStateOf(false) }

    DisposableEffect(clipboardManager) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            clipboardText = readClipboard(clipboardManager, context)
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        onDispose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }
    var destinationMode by rememberSaveable {
        mutableStateOf(
            when {
                target.requiresCustomDestination -> DestinationMode.CUSTOM
                target.entryPaths.isNotEmpty() -> DestinationMode.CURRENT_FOLDER
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
        mutableStateOf(
            if (target.initialEncoding == FileNameEncoding.AUTO) {
                settings.defaultEncoding
            } else {
                target.initialEncoding
            }
        )
    }
    var perArchivePasswords by remember(target.archives) {
        mutableStateOf(List(target.archives.size) { "" })
    }
    var pendingPasswordQueue by remember { mutableStateOf<List<Int>>(emptyList()) }
    var passwordDialogIndex by remember { mutableStateOf<Int?>(null) }
    var passwordDialogValue by remember { mutableStateOf("") }
    var pendingPasswords by remember { mutableStateOf<List<String>?>(null) }
    var pendingArchives by remember { mutableStateOf<List<BrowserEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var archives by remember(target.archives) { mutableStateOf(target.archives) }
    val destinationHint = when (destinationMode) {
        DestinationMode.CURRENT_FOLDER ->
            "将解压到 ${target.parentPath.ifBlank { target.parentName }}"
        DestinationMode.NEW_FOLDER -> if (archives.size > 1) {
            "将解压到 ${target.parentPath} 下，每个压缩包建同名文件夹"
        } else {
            val folder = archives.firstOrNull()?.name?.substringBeforeLast('.') ?: "新文件夹"
            "将解压到 ${target.parentPath} / $folder"
        }
        DestinationMode.CUSTOM ->
            "将解压到 ${customName?.ifBlank { "自定义位置" } ?: "自定义位置"}"
    }
    var archiveNameValue by remember(target.archives) {
        mutableStateOf(
            fileNameWithSuffixSelected(
                target.archives.singleOrNull()?.name.orEmpty()
            )
        )
    }
    var showCustomSuffixDialog by remember { mutableStateOf(false) }
    var customSuffix by remember { mutableStateOf("") }
    var renamingArchive by remember { mutableStateOf(false) }
    val canRenameArchive =
        archives.size == 1 && target.parentUri != null && target.parentTreeUri != null

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

    val launchExtraction: (List<BrowserEntry>, List<String>) -> Unit = { selectedArchives, passwords ->
        if (destinationMode == DestinationMode.CUSTOM && customTreeUri == null) {
            error = "请先选择自定义位置"
        } else {
            val unsupported = selectedArchives.firstOrNull { !engine.supports(it.name) }
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
                                archiveUris = selectedArchives.map { it.uri },
                                archiveNames = selectedArchives.map { it.name },
                                parentUri = parentUri,
                                parentTreeUri = treeUri,
                                mode = destinationMode,
                                customTreeUri = customTreeUri,
                                customRootUri = customRootUri,
                                createFolder =
                                    destinationMode != DestinationMode.CURRENT_FOLDER,
                                conflictPolicy = conflictPolicy,
                                encoding = encoding,
                                passwords = passwords,
                                parentPath = target.parentPath,
                                parentName = target.parentName,
                                entryPaths = target.entryPaths
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
            val selectedArchives = pendingArchives ?: archives
            pendingArchives = null
            pendingPasswords = null
            launchExtraction(selectedArchives, passwords)
        }
    }

    fun requestPermissionAndLaunch(
        selectedArchives: List<BrowserEntry>,
        passwords: List<String>
    ) {
        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingArchives = selectedArchives
            pendingPasswords = passwords
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchExtraction(selectedArchives, passwords)
        }
    }

    fun continueExtraction(selectedArchives: List<BrowserEntry>) {
        val unsupported = selectedArchives.firstOrNull { !engine.supports(it.name) }
        if (unsupported != null) {
            error = "暂不支持 ${unsupported.name}"
            return
        }
        error = null
        if (savePassword && password.isNotEmpty()) {
            selectedArchives.forEach { archive ->
                com.decompress.app.data.PasswordVault.save(context, archive.name, password)
            }
        }
        if (selectedArchives.size > 1 && password.isNotEmpty()) {
            // 多包默认全部复用同一个密码；先用列表探测哪些包的密码不同，只补问这些。
            scope.launch {
                val mismatched = selectedArchives.mapIndexedNotNull { index, archive ->
                    val failure = runCatching {
                        engine.list(archive.uri, archive.name, password)
                    }.exceptionOrNull()
                    if (failure != null && failure.isPasswordProblem()) index else null
                }
                if (mismatched.isEmpty()) {
                    requestPermissionAndLaunch(
                        selectedArchives,
                        selectedArchives.map { password }
                    )
                } else {
                    perArchivePasswords = List(selectedArchives.size) { password }
                    pendingPasswordQueue = mismatched
                    passwordDialogValue = ""
                    passwordDialogIndex = mismatched.first()
                }
            }
        } else {
            requestPermissionAndLaunch(selectedArchives, selectedArchives.map { password })
        }
    }

    fun startExtraction() {
        if (destinationMode == DestinationMode.CUSTOM && customTreeUri == null) {
            error = "请先选择自定义位置"
            return
        }
        val entry = archives.singleOrNull()
        val requestedName = archiveNameValue.text.trim()
        if (
            canRenameArchive &&
            entry != null &&
            requestedName != entry.name &&
            !engine.supports(requestedName)
        ) {
            error = "新名称需要使用受支持的压缩包后缀"
            return
        }
        if (
            canRenameArchive &&
            entry != null &&
            !requestedName.isNullOrBlank() &&
            requestedName != entry.name
        ) {
            renamingArchive = true
            scope.launch {
                val renamed = onRenameArchive(entry, requestedName)
                renamingArchive = false
                if (renamed == null) {
                    error = "重命名失败，名称无效或已存在"
                } else {
                    archives = listOf(renamed)
                    archiveNameValue = fileNameWithSuffixSelected(renamed.name)
                    continueExtraction(listOf(renamed))
                }
            }
        } else {
            continueExtraction(archives)
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
                val blockPassword: @Composable () -> Unit = {
                Text(
                    "密码",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None
                    ),
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

                if (archives.size == 1 && password.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { savePassword = !savePassword },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = savePassword,
                            onCheckedChange = { savePassword = it }
                        )
                        Text(
                            "保存密码到本机（Keystore 加密）",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(ZippestRadius.sm),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
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
                                    modifier = Modifier.size(20.dp)
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
                }
                }

                val blockName: @Composable () -> Unit = {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "压缩包名称",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = archiveNameValue,
                        onValueChange = { archiveNameValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("文件名") },
                        trailingIcon = {
                            if (archiveNameValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        archiveNameValue = clearNameField(archiveNameValue)
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "清空文件名"
                                    )
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(".7z", ".zip", ".rar").forEach { suffix ->
                            FilterChip(
                                selected = selectedFileSuffix(archiveNameValue)
                                    .equals(suffix, ignoreCase = true),
                                onClick = {
                                    archiveNameValue = replaceSelectedFileSuffix(
                                        archiveNameValue,
                                        suffix
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(suffix.uppercase(), maxLines = 1) }
                            )
                        }
                        FilterChip(
                            selected = selectedFileSuffix(archiveNameValue).lowercase() !in
                                setOf(".7z", ".zip", ".rar"),
                            onClick = {
                                customSuffix = selectedFileSuffix(archiveNameValue)
                                showCustomSuffixDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("自定义", maxLines = 1) }
                        )
                    }
                    Text(
                        "点击“开始解压”时会先保存新名称",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val blockDestination: @Composable () -> Unit = {
                Spacer(Modifier.height(20.dp))
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
                    detail = if (archives.size == 1) {
                        listOf(
                            target.parentPath,
                            archiveBaseName(archives.first().name)
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
                }

                val blockConflict: @Composable () -> Unit = {
                Spacer(Modifier.height(20.dp))
                Text(
                    "遇到同名文件",
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

                }

                val blockEncoding: @Composable () -> Unit = {
                Spacer(Modifier.height(16.dp))
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

                }
                val totalBytes = target.archives.sumOf { it.size }
                if (totalBytes > 2L * 1024 * 1024 * 1024) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(ZippestRadius.sm),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "压缩包约 %.1f GB，解压时建议保持应用在前台；系统会限制后台运行时长，超时后任务可能被暂停。"
                                .format(totalBytes / 1073741824.0),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                val extractBlocks = mapOf(
                    com.decompress.app.data.ExtractSection.PASSWORD to blockPassword,
                    com.decompress.app.data.ExtractSection.NAME to blockName,
                    com.decompress.app.data.ExtractSection.DESTINATION to blockDestination,
                    com.decompress.app.data.ExtractSection.CONFLICT to blockConflict,
                    com.decompress.app.data.ExtractSection.ENCODING to blockEncoding
                )
                settings.extractOrder
                    .filter { section ->
                        section !in settings.hiddenExtractSections &&
                            (
                                section != com.decompress.app.data.ExtractSection.NAME ||
                                    canRenameArchive
                                )
                    }
                    .forEach { section -> extractBlocks[section]?.invoke() }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                destinationHint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { startExtraction() },
                    enabled = !renamingArchive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (renamingArchive) "正在重命名…" else "开始解压")
                }
            }
        }
    }

    if (clipboardExpanded) {
        AlertDialog(
            onDismissRequest = { clipboardExpanded = false },
            title = { Text("剪贴板") },
            text = {
                val text = clipboardText
                if (text == null) {
                    Column {
                        Text("系统剪贴板为空")
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "可以直接长按密码框，或使用输入法顶部的剪贴板/粘贴按钮。" +
                                "应用无法读取输入法私有的剪贴板历史。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = clipboardText != null,
                    onClick = {
                        password = clipboardText.orEmpty()
                        clipboardExpanded = false
                    }
                ) {
                    Text("使用")
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { clipboardExpanded = false }) {
                    Text("关闭")
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
                    supportingText = { Text("使用一个前导点，如 .tar.gz") }
                )
            },
            confirmButton = {
                Button(
                    enabled = sanitizeFileSuffix(customSuffix).isNotEmpty(),
                    onClick = {
                        archiveNameValue = replaceSelectedFileSuffix(
                            archiveNameValue,
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

    passwordDialogIndex?.let { index ->
        val archive = archives.getOrNull(index)
        if (archive != null) {
            AlertDialog(
                onDismissRequest = { passwordDialogIndex = null },
                title = { Text("「" + archive.name + "」的密码") },
                text = {
                    Column {
                        Text(
                            archive.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "第 " + (index + 1) + "/" + archives.size +
                                " 个 · 压缩包没有密码时可留空。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passwordDialogValue,
                            onValueChange = { passwordDialogValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = perArchivePasswords.toMutableList().also {
                                it[index] = passwordDialogValue
                            }
                            perArchivePasswords = updated
                            val next = pendingPasswordQueue
                                .dropWhile { it <= index }
                                .firstOrNull()
                            if (next != null) {
                                passwordDialogValue = ""
                                passwordDialogIndex = next
                            } else {
                                passwordDialogIndex = null
                                pendingPasswordQueue = emptyList()
                                requestPermissionAndLaunch(archives, updated)
                            }
                        }
                    ) {
                        Text(if (index + 1 == archives.size) "开始解压" else "下一个")
                    }
                },
                dismissButton = {
                    TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { passwordDialogIndex = null }) {
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
        shape = RoundedCornerShape(ZippestRadius.sm),
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                Spacer(Modifier.height(4.dp))
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
    val parts = (0 until clip.itemCount).mapNotNull { index ->
        val item = clip.getItemAt(index)
        runCatching {
            item.text?.toString()?.takeIf { it.isNotEmpty() }
                ?: item.htmlText?.toString()?.takeIf { it.isNotEmpty() }
                ?: item.coerceToText(context)?.toString()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
    if (parts.isEmpty() || parts.all { it.isBlank() }) return null
    return parts.joinToString("\n")
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

/** 宽屏（≥600dp）时显示在左侧的包内位置栏：面包屑式，点一下跳到该层。 */
@Composable
private fun ArchivePathPane(
    currentPath: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(currentPath) {
        if (currentPath.isBlank()) emptyList() else currentPath.split('/')
    }
    Column(
        modifier = modifier.padding(
            start = ZippestSpacing.md,
            end = ZippestSpacing.sm,
            top = ZippestSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "包内位置",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(ZippestSpacing.sm))
        ArchivePathEntry("根目录", currentPath.isBlank()) { onOpen("") }
        segments.forEachIndexed { index, name ->
            val path = segments.take(index + 1).joinToString("/")
            ArchivePathEntry(name, path == currentPath) { onOpen(path) }
        }
    }
}

@Composable
private fun ArchivePathEntry(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ZippestRadius.sm),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Throwable.isPasswordProblem(): Boolean {
    val text = (message ?: "").lowercase()
    return "密码" in text || "password" in text || "encrypted" in text
}
