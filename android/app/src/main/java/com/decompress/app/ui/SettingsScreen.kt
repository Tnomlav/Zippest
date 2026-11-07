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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.RadioButton
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
import com.decompress.app.data.TrashQuota
import com.decompress.app.data.RestoreConflictPolicy
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
    QUICK_LOCATION_DELETE,
    ABOUT_INFO,
    PRIVACY_POLICY,
    OPEN_SOURCE_LICENSES
}

private enum class SettingsCategory(val title: String) {
    APPEARANCE("外观"),
    BROWSING("文件浏览"),
    QUICK_ACCESS("快速访问"),
    EXTRACT_DEFAULTS("默认解压设置"),
    PERMISSIONS("通知与权限"),
    FILES_TASKS("文件与任务"),
    ABOUT("关于")
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
    var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    BackHandler(enabled = activeCategory != null) { activeCategory = null }
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

    fun openExternalLink(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            onNotify("没有可打开此链接的应用")
        }
    }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.6.1"
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeCategory != null) activeCategory = null else onBack()
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text(activeCategory?.title ?: "设置") },
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
            if (activeCategory == null) {
                item {
                    SettingsSection("分类") {
                        SettingsAction(title = "外观", detail = "主题、隐藏文件、缩略图") { activeCategory = SettingsCategory.APPEARANCE }
                        SettingsDivider()
                        SettingsAction(title = "文件浏览", detail = "排序、显示、打开方式") { activeCategory = SettingsCategory.BROWSING }
                        SettingsDivider()
                        SettingsAction(title = "快速访问", detail = "侧边栏的固定位置") { activeCategory = SettingsCategory.QUICK_ACCESS }
                        SettingsDivider()
                        SettingsAction(title = "默认解压设置", detail = "位置、同名文件、文件名编码") { activeCategory = SettingsCategory.EXTRACT_DEFAULTS }
                        SettingsDivider()
                        SettingsAction(title = "通知与权限", detail = "所有文件访问、通知、电池") { activeCategory = SettingsCategory.PERMISSIONS }
                        SettingsDivider()
                        SettingsAction(title = "文件与任务", detail = "回收站、清理与容量") { activeCategory = SettingsCategory.FILES_TASKS }
                        SettingsDivider()
                        SettingsAction(title = "关于", detail = "版本、隐私与许可") { activeCategory = SettingsCategory.ABOUT }
                    }
                }
            }
            if (activeCategory == SettingsCategory.APPEARANCE) {
            item {
                SettingsSection("外观", showHeader = false) {
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
            }
            if (activeCategory == SettingsCategory.BROWSING) {
            item {
                SettingsSection("文件浏览", showHeader = false) {
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
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }.onFailure {
                                batterySettingsLauncher.launch(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            }
                        }
                    )
                    if (
                        Build.MANUFACTURER.equals("vivo", ignoreCase = true) ||
                        Build.MANUFACTURER.equals("iqoo", ignoreCase = true)
                    ) {
                        SettingsDivider()
                        SettingsStatus(
                            title = "Vivo 后台运行权限",
                            detail = "在“自启动/后台启动”中允许 Zippest，避免熄屏后任务被系统冻结",
                            action = "打开",
                            enabled = true,
                            onClick = {
                                val vendorIntent = Intent(
                                    "com.iqoo.secure.BGSTARTUPMANAGER"
                                ).addCategory(Intent.CATEGORY_DEFAULT)
                                if (vendorIntent.resolveActivity(context.packageManager) != null) {
                                    batterySettingsLauncher.launch(vendorIntent)
                                } else {
                                    batterySettingsLauncher.launch(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            }
                        )
                    }
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
            }
            if (activeCategory == SettingsCategory.QUICK_ACCESS) {
            item {
                SettingsSection("快速访问", showHeader = false) {
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
                        title = "恢复默认快速访问",
                        detail = "恢复内部存储、下载、相册、相机等默认入口",
                        onClick = {
                            quickLocationRepository.reset()
                            onNotify("已恢复默认快捷位置")
                        }
                    )
                }
            }
            }
            if (activeCategory == SettingsCategory.EXTRACT_DEFAULTS) {
            item {
                SettingsSection("默认解压设置", showHeader = false) {
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
            }
            if (activeCategory == SettingsCategory.PERMISSIONS) {
            item {
                SettingsSection("通知与权限", showHeader = false) {
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
            }
            if (activeCategory == SettingsCategory.FILES_TASKS) {
            item {
                SettingsSection("文件与任务", showHeader = false) {
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
                    SettingsChoice(
                        title = "回收站容量上限",
                        options = TrashQuota.entries.map { it.label },
                        selectedIndex = TrashQuota.entries.indexOf(settings.trashQuota),
                        onSelected = { index ->
                            repository.update {
                                it.copy(trashQuota = TrashQuota.entries[index])
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = "恢复同名文件",
                        options = RestoreConflictPolicy.entries.map { it.label },
                        selectedIndex = RestoreConflictPolicy.entries.indexOf(
                            settings.restoreConflictPolicy
                        ),
                        onSelected = { index ->
                            repository.update {
                                it.copy(
                                    restoreConflictPolicy =
                                        RestoreConflictPolicy.entries[index]
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "只浏览某个文件夹…",
                        detail = if (browserPath.isBlank()) {
                            "当前：全部文件"
                        } else {
                            "当前：$browserPath"
                        },
                        onClick = onChooseBrowserFolder
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
            }
            if (activeCategory == SettingsCategory.ABOUT) {
            item {
                SettingsSection("关于", showHeader = false) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            "Zippest $versionName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Android 文件管理与压缩工具",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SettingsDivider()
                    SettingsAction(
                        title = "应用信息",
                        detail = "版本、包名、系统要求与项目地址",
                        onClick = { activePopup = SettingsPopup.ABOUT_INFO }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "隐私政策",
                        detail = "数据收集、本地处理、权限与剪贴板说明",
                        onClick = { activePopup = SettingsPopup.PRIVACY_POLICY }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "第三方开源许可",
                        detail = "查看压缩引擎、界面框架等组件许可证",
                        onClick = { activePopup = SettingsPopup.OPEN_SOURCE_LICENSES }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = "GitHub 项目",
                        detail = ZIPPEST_GITHUB_URL,
                        onClick = { openExternalLink(ZIPPEST_GITHUB_URL) }
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
        SettingsPopup.ABOUT_INFO -> AboutInfoDialog(
            versionName = versionName,
            packageName = context.packageName,
            onOpenLink = ::openExternalLink,
            onDismiss = { activePopup = null }
        )
        SettingsPopup.PRIVACY_POLICY -> PrivacyPolicyDialog(
            onDismiss = { activePopup = null }
        )
        SettingsPopup.OPEN_SOURCE_LICENSES -> OpenSourceLicensesDialog(
            onOpenLink = ::openExternalLink,
            onDismiss = { activePopup = null }
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
    showHeader: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        if (showHeader) {
            Text(
                title,
                modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
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
    var dialogOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { dialogOpen = true }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            options.getOrElse(selectedIndex) { "" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(index)
                                    dialogOpen = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIndex == index,
                                onClick = {
                                    onSelected(index)
                                    dialogOpen = false
                                }
                            )
                            Text(
                                option,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("取消")
                }
            }
        )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
