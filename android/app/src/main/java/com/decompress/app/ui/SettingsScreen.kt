@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.decompress.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.data.BrowserSortMode
import com.decompress.app.data.ConflictPolicy
import com.decompress.app.data.DefaultDestinationMode
import com.decompress.app.data.FileNameEncoding
import com.decompress.app.data.OpenWithPreferences
import com.decompress.app.data.QuickLocation
import com.decompress.app.data.QuickLocationRepository
import com.decompress.app.data.ThemeMode
import com.decompress.app.data.TrashRetention
import com.decompress.app.data.TrashRepository
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.service.CompressionScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPopup {
    TASK_QUEUE,
    TRASH,
    QUICK_LOCATION_EDITOR,
    QUICK_LOCATION_DELETE
}

@Composable
fun SettingsScreen(
    repository: AppSettingsRepository,
    quickLocationRepository: QuickLocationRepository,
    browserPath: String,
    onBack: () -> Unit,
    onChooseBrowserFolder: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = repository.settings
    var storageGranted by remember { mutableStateOf(hasAllFilesAccess(context)) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var batteryUnrestricted by remember {
        mutableStateOf(context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName))
    }
    var activePopup by remember { mutableStateOf<SettingsPopup?>(null) }
    var editingQuickLocation by remember { mutableStateOf<QuickLocation?>(null) }
    var deletingQuickLocation by remember { mutableStateOf<QuickLocation?>(null) }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        storageGranted = hasAllFilesAccess(context)
    }
    val legacyStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        storageGranted = hasAllFilesAccess(context)
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsEnabled =
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        notificationsEnabled =
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val batterySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
    }
    val customDestinationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val root = runCatching {
            val id = DocumentsContract.getTreeDocumentId(uri)
            DocumentsContract.buildDocumentUriUsingTree(uri, id)
        }.getOrNull()
        repository.update {
            it.copy(
                defaultDestinationMode = DefaultDestinationMode.CUSTOM,
                customDestinationTreeUri = uri.toString(),
                customDestinationRootUri = root?.toString(),
                customDestinationName = root?.let { queryFolderName(context, it) }
                    ?: "已选择位置"
            )
        }
    }

    fun openQuickLocationEditor(location: QuickLocation?) {
        editingQuickLocation = location
        activePopup = SettingsPopup.QUICK_LOCATION_EDITOR
    }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.5.4"
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsSection("外观") {
                    SettingsChoice(
                        title = "主题",
                        options = ThemeMode.entries.map { it.label },
                        selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
                        onSelected = { index ->
                            repository.update {
                                it.copy(themeMode = ThemeMode.entries[index])
                            }
                        }
                    )
                }
            }
            item {
                SettingsSection("文件浏览") {
                    SettingsSwitch(
                        title = "显示隐藏文件",
                        detail = "显示名称以点号开头的文件和文件夹",
                        checked = settings.showHiddenFiles,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(showHiddenFiles = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsStatus(
                        title = "熄屏继续运行",
                        detail = if (batteryUnrestricted) {
                            "已开启，后台任务不受电池优化限制"
                        } else {
                            "可能被系统节电策略暂停"
                        },
                        action = if (batteryUnrestricted) "管理" else "允许",
                        enabled = true,
                        onClick = {
                            runCatching {
                                batterySettingsLauncher.launch(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            }.onFailure {
                                batterySettingsLauncher.launch(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsSwitch(
                        title = "显示图片和视频缩略图",
                        detail = "关闭后使用统一文件图标，减少缩略图加载",
                        checked = settings.showThumbnails,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(showThumbnails = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = "默认排序",
                        options = BrowserSortMode.entries.map { it.label },
                        selectedIndex =
                            BrowserSortMode.entries.indexOf(settings.defaultSortMode),
                        onSelected = { index ->
                            repository.update {
                                it.copy(defaultSortMode = BrowserSortMode.entries[index])
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "文件浏览位置",
                        detail = browserPath.ifBlank { "尚未选择" },
                        onClick = onChooseBrowserFolder
                    )
                }
            }
            item {
                SettingsSection("内部存储快捷位置") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${quickLocationRepository.locations.size} 个位置",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { openQuickLocationEditor(null) }) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("新建", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    if (quickLocationRepository.locations.isEmpty()) {
                        Text(
                            "暂无快捷位置",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        quickLocationRepository.locations.forEachIndexed { index, location ->
                            if (index > 0) SettingsDivider()
                            QuickLocationSettingsRow(
                                location = location,
                                onEdit = { openQuickLocationEditor(location) },
                                onDelete = {
                                    deletingQuickLocation = location
                                    activePopup = SettingsPopup.QUICK_LOCATION_DELETE
                                }
                            )
                        }
                    }
                    SettingsDivider()
                    SettingsAction(
                        title = "恢复默认快捷位置",
                        detail = "恢复目录首页、下载、相册、相机等默认入口",
                        onClick = {
                            quickLocationRepository.reset()
                            onNotify("已恢复默认快捷位置")
                        }
                    )
                }
            }
            item {
                SettingsSection("默认解压设置") {
                    SettingsChoice(
                        title = "默认位置",
                        options = DefaultDestinationMode.entries.map { it.label },
                        selectedIndex = DefaultDestinationMode.entries.indexOf(
                            settings.defaultDestinationMode
                        ),
                        onSelected = { index ->
                            repository.update {
                                it.copy(
                                    defaultDestinationMode =
                                        DefaultDestinationMode.entries[index]
                                )
                            }
                        }
                    )
                    if (settings.defaultDestinationMode == DefaultDestinationMode.CUSTOM) {
                        SettingsDivider()
                        SettingsAction(
                            title = "固定解压位置",
                            detail = settings.customDestinationName ?: "选择手机中的任意文件夹",
                            onClick = { customDestinationLauncher.launch(null) }
                        )
                    }
                    SettingsDivider()
                    SettingsChoice(
                        title = "同名文件",
                        options = ConflictPolicy.entries.map { it.label },
                        selectedIndex = ConflictPolicy.entries.indexOf(
                            settings.defaultConflictPolicy
                        ),
                        onSelected = { index ->
                            repository.update {
                                it.copy(
                                    defaultConflictPolicy = ConflictPolicy.entries[index]
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = "文件名编码",
                        options = FileNameEncoding.entries.map { it.label },
                        selectedIndex = FileNameEncoding.entries.indexOf(
                            settings.defaultEncoding
                        ),
                        onSelected = { index ->
                            repository.update {
                                it.copy(defaultEncoding = FileNameEncoding.entries[index])
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsSwitch(
                        title = "从剪贴板填充密码",
                        detail = "打开解压面板时自动读取剪贴板文字",
                        checked = settings.fillPasswordFromClipboard,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(fillPasswordFromClipboard = it)
                            }
                        }
                    )
                }
            }
            item {
                SettingsSection("通知与权限") {
                    SettingsStatus(
                        title = "所有文件访问",
                        detail = if (storageGranted) "已授权" else "未授权",
                        action = if (storageGranted) "管理" else "授权",
                        enabled = true,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                runCatching {
                                    allFilesLauncher.launch(intent)
                                }.onFailure {
                                    allFilesLauncher.launch(
                                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    )
                                }
                            } else {
                                legacyStorageLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsStatus(
                        title = "系统通知",
                        detail = if (notificationsEnabled) "已开启" else "未开启",
                        action = if (notificationsEnabled) "管理" else "开启",
                        enabled = true,
                        onClick = {
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                val intent = Intent(
                                    Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                ).putExtra(
                                    Settings.EXTRA_APP_PACKAGE,
                                    context.packageName
                                )
                                notificationSettingsLauncher.launch(intent)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsSwitch(
                        title = "任务完成通知",
                        detail = "后台压缩或解压结束后发送系统通知",
                        checked = settings.notifyOnComplete,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(notifyOnComplete = it)
                            }
                        }
                    )
                }
            }
            item {
                SettingsSection("文件与任务") {
                    SettingsSwitch(
                        title = "删除时移入回收站",
                        detail = "关闭后删除会直接永久移除文件",
                        checked = settings.useTrash,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(useTrash = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = "回收站保留时间",
                        options = TrashRetention.entries.map { it.label },
                        selectedIndex = TrashRetention.entries.indexOf(
                            settings.trashRetention
                        ),
                        onSelected = { index ->
                            val retention = TrashRetention.entries[index]
                            repository.update {
                                it.copy(trashRetention = retention)
                            }
                            scope.launch {
                                val cleaned = withContext(Dispatchers.IO) {
                                    TrashRepository(context.applicationContext)
                                        .purgeExpired(retention.days)
                                }
                                if (cleaned > 0) {
                                    onNotify("已清理 $cleaned 项过期文件")
                                }
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "回收站",
                        detail = "查看、恢复或永久删除已移除的文件",
                        onClick = { activePopup = SettingsPopup.TRASH }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "清除打开方式偏好",
                        detail = "下次打开文件时重新选择应用",
                        onClick = {
                            OpenWithPreferences.clear(context)
                            onNotify("已清除打开方式偏好")
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "文件关联",
                        detail = "在系统应用设置中管理“用 Zippest 打开”",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "后台任务",
                        detail = "查看正在运行、等待和已结束的压缩、解压任务",
                        onClick = { activePopup = SettingsPopup.TASK_QUEUE }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "清理已结束任务",
                        detail = "删除任务中心里已完成、失败和取消的记录",
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    WorkManager.getInstance(context).pruneWork()
                                }
                                onNotify("已清理任务历史")
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "取消全部后台任务",
                        detail = "立即停止当前任务和等待中的任务",
                        destructive = true,
                        onClick = {
                            WorkManager.getInstance(context).apply {
                                cancelAllWorkByTag(ExtractionScheduler.TAG_ALL_TASKS)
                                cancelAllWorkByTag(CompressionScheduler.TAG_ALL_TASKS)
                            }
                            onNotify("已取消全部任务")
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "恢复默认设置",
                        detail = "主题、浏览、压缩和解压偏好恢复到初始值",
                        destructive = true,
                        onClick = {
                            repository.reset()
                            storageGranted = hasAllFilesAccess(context)
                            notificationsEnabled =
                                NotificationManagerCompat.from(context)
                                    .areNotificationsEnabled()
                            onNotify("已恢复默认设置")
                        }
                    )
                }
            }
            item {
                SettingsSection("关于") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            "Zippest $versionName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "解压：ZIP · 7Z · RAR · TAR · ZST · BR · " +
                                "GZ · BZ2 · XZ · LZMA · LZ4 · CPIO\n" +
                                "创建：ZIP · 7Z · TAR 系列 · ZST · " +
                                "GZ · BZ2 · XZ · LZMA · LZ4 · CPIO\n" +
                                "包名：${context.packageName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(10.dp).navigationBarsPadding()) }
        }
    }

    when (activePopup) {
        SettingsPopup.TASK_QUEUE -> TaskQueueDialog(
            onDismiss = { activePopup = null }
        )
        SettingsPopup.TRASH -> TrashSheet(
            onDismiss = { activePopup = null },
            onChanged = { message -> onNotify(message) }
        )
        SettingsPopup.QUICK_LOCATION_EDITOR -> QuickLocationEditorDialog(
            repository = quickLocationRepository,
            location = editingQuickLocation,
            onDismiss = { activePopup = null },
            onSaved = { message ->
                activePopup = null
                onNotify(message)
            }
        )
        SettingsPopup.QUICK_LOCATION_DELETE -> AlertDialog(
            onDismissRequest = { activePopup = null },
            title = { Text("删除快捷位置") },
            text = {
                Text(
                    "确定删除“${deletingQuickLocation?.label.orEmpty()}”？" +
                        "这不会删除对应文件夹或其中的文件。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingQuickLocation?.let {
                            quickLocationRepository.delete(it.id)
                        }
                        deletingQuickLocation = null
                        activePopup = null
                        onNotify("已删除快捷位置")
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { activePopup = null }) {
                    Text("取消")
                }
            }
        )
        null -> Unit
    }
}

@Composable
private fun QuickLocationSettingsRow(
    location: QuickLocation,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(vertical = 5.dp)
        ) {
            Text(
                location.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (location.path.isBlank()) {
                    "内部存储"
                } else {
                    "内部存储 / ${location.path}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "更换地址",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "删除快捷位置",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun SettingsChoice(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, option ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsAction(
    title: String,
    detail: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsStatus(
    title: String,
    detail: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                detail,
                style = MaterialTheme.typography.labelMedium,
                color = if (detail == "已授权" || detail == "已开启") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        TextButton(onClick = onClick, enabled = enabled) {
            Text(action)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    )
}

private fun hasAllFilesAccess(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
            (
                Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                )
    }

private fun queryFolderName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
