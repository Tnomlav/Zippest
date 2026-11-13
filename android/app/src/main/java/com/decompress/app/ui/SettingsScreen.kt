@file:Suppress("LocalContextGetResourceValueCall")

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.decompress.app.ui

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.LocaleList
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import com.decompress.app.R
import com.decompress.app.data.ExtractSection
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.annotation.StringRes
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.decompress.app.data.AppLanguage
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
import com.decompress.app.data.UpdateChecker
import com.decompress.app.data.TrashQuota
import com.decompress.app.data.AppAccent
import com.decompress.app.data.RestoreConflictPolicy
import com.decompress.app.data.TrashRepository
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.service.CompressionScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.decompress.app.ui.theme.ZippestRadius

private enum class SettingsPopup {
    TASK_QUEUE,
    TRASH,
    QUICK_LOCATION_EDITOR,
    QUICK_LOCATION_DELETE,
    ABOUT_INFO,
    PRIVACY_POLICY,
    PROJECT_LICENSE,
    OPEN_SOURCE_LICENSES,
    FEEDBACK
}

private enum class SettingsCategory(@StringRes val titleRes: Int) {
    APPEARANCE(R.string.settings_appearance),
    BROWSING(R.string.settings_file_browsing),
    QUICK_ACCESS(R.string.settings_quick_access),
    EXTRACT_DEFAULTS(R.string.settings_default_extraction),
    PERMISSIONS(R.string.settings_notifications_permissions),
    FILES_TASKS(R.string.settings_files_tasks),
    ABOUT(R.string.settings_about)
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
    var checkingUpdate by remember { mutableStateOf(false) }
    var editingExtractLayout by remember { mutableStateOf(false) }

    BackHandler {
        if (activeCategory != null) activeCategory = null else onBack()
    }
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
                    ?: context.getString(R.string.settings_selected_location)
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
            onNotify(context.getString(R.string.settings_no_app_for_link))
        }
    }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.6.1"
    }
    val versionCode = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }.getOrNull() ?: 0L
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeCategory != null) activeCategory = null else onBack()
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = context.getString(R.string.settings_back))
                    }
                },
                title = { Text(
                    activeCategory?.let { context.getString(it.titleRes) }
                        ?: context.getString(R.string.settings_settings)
                ) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeCategory == null) {
                item {
                    SettingsSection(context.getString(R.string.settings_categories), showHeader = false) {
                        SettingsAction(title = context.getString(R.string.settings_appearance), detail = context.getString(R.string.settings_theme_hidden_thumbnails)) { activeCategory = SettingsCategory.APPEARANCE }
                        SettingsDivider()
                        SettingsAction(title = context.getString(R.string.settings_file_browsing), detail = context.getString(R.string.settings_sort_display_open_with)) { activeCategory = SettingsCategory.BROWSING }
                        SettingsDivider()
                        SettingsAction(title = context.getString(R.string.settings_quick_access), detail = context.getString(R.string.settings_sidebar_pins)) { activeCategory = SettingsCategory.QUICK_ACCESS }
                        SettingsDivider()
                        SettingsAction(title = context.getString(R.string.settings_default_extraction), detail = context.getString(R.string.settings_destination_conflict_encoding)) { activeCategory = SettingsCategory.EXTRACT_DEFAULTS }
                        SettingsDivider()
                        SettingsAction(title = context.getString(R.string.settings_notifications_permissions), detail = context.getString(R.string.settings_all_files_notifications_battery)) { activeCategory = SettingsCategory.PERMISSIONS }
                        SettingsDivider()
                        SettingsAction(title = context.getString(R.string.settings_files_tasks), detail = context.getString(R.string.settings_trash_cleanup_quota)) { activeCategory = SettingsCategory.FILES_TASKS }
                        SettingsDivider()
                        SettingsAction(title = context.getString(R.string.settings_about), detail = context.getString(R.string.settings_version_privacy_license)) { activeCategory = SettingsCategory.ABOUT }
                    }
                }
            }
            if (activeCategory == SettingsCategory.APPEARANCE) {
            item {
                SettingsSection(context.getString(R.string.settings_appearance), showHeader = false) {
                    SettingsChoice(
                        title = context.getString(R.string.settings_theme),
                        options = ThemeMode.entries.map { it.label() },
                        selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
                        onSelected = { index ->
                            repository.update {
                                it.copy(themeMode = ThemeMode.entries[index])
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = context.getString(R.string.settings_language),
                        options = AppLanguage.entries.map { it.label() },
                        selectedIndex = AppLanguage.entries.indexOf(settings.language),
                        onSelected = { index ->
                            val language = AppLanguage.entries[index]
                            repository.update { current ->
                                current.copy(language = language)
                            }
                            val activity = context as? Activity
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // 系统会按 applicationLocales 自己重建 Activity；
                                // 这里再调一次 recreate() 会和系统重建撞车，导致崩溃/闪回首页。
                                runCatching {
                                    context.getSystemService(LocaleManager::class.java)
                                        ?.applicationLocales = when (language) {
                                        AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
                                        AppLanguage.CHINESE ->
                                            LocaleList.forLanguageTags("zh-CN")
                                        AppLanguage.ENGLISH ->
                                            LocaleList.forLanguageTags("en")
                                    }
                                }.onFailure {
                                    activity?.takeUnless { it.isFinishing || it.isDestroyed }
                                        ?.recreate()
                                }
                            } else {
                                activity?.takeUnless { it.isFinishing || it.isDestroyed }
                                    ?.recreate()
                            }
                        }
                    )
                    SettingsDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Text(
                            context.getString(R.string.settings_accent_color),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AppAccent.entries.forEach { accent ->
                                val selected = settings.accentColor == accent
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(accent.lightPrimary))
                                        .border(
                                            width = if (selected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            repository.update {
                                                it.copy(accentColor = accent)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
            }
            if (activeCategory == SettingsCategory.BROWSING) {
            item {
                SettingsSection(context.getString(R.string.settings_file_browsing), showHeader = false) {
                    SettingsSwitch(
                        title = context.getString(R.string.settings_show_hidden),
                        detail = context.getString(R.string.settings_show_hidden_desc),
                        checked = settings.showHiddenFiles,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(showHiddenFiles = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsStatus(
                        title = context.getString(R.string.settings_keep_running),
                        detail = if (batteryUnrestricted) {
                            context.getString(R.string.settings_keep_running_on)
                        } else {
                            context.getString(R.string.settings_keep_running_off)
                        },
                        action = if (batteryUnrestricted) context.getString(R.string.settings_manage) else context.getString(R.string.settings_allow),
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
                        listOf(
                            "vivo", "iqoo", "xiaomi", "redmi", "oppo", "realme",
                            "huawei", "honor", "samsung", "oneplus", "meizu"
                        ).any { Build.MANUFACTURER.equals(it, ignoreCase = true) }
                    ) {
                        SettingsDivider()
                        SettingsStatus(
                            title = context.getString(R.string.settings_background_permission),
                            detail = context.getString(R.string.settings_background_permission_desc),
                            action = context.getString(R.string.settings_open),
                            enabled = true,
                            onClick = {
                                val vendorIntents = listOf(
                                    "com.iqoo.secure.BGSTARTUPMANAGER",
                                    "com.vivo.permissionmanager",
                                    "com.miui.securitycenter",
                                    "com.coloros.safecenter",
                                    "com.huawei.systemmanager"
                                )
                                val vendorIntent = vendorIntents
                                    .map { Intent(it).addCategory(Intent.CATEGORY_DEFAULT) }
                                    .firstOrNull {
                                        it.resolveActivity(context.packageManager) != null
                                    }
                                batterySettingsLauncher.launch(
                                    vendorIntent ?: Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        )
                    }
                    SettingsDivider()
                    SettingsSwitch(
                        title = context.getString(R.string.settings_show_thumbnails),
                        detail = context.getString(R.string.settings_show_thumbnails_desc),
                        checked = settings.showThumbnails,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(showThumbnails = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsSwitch(
                        title = context.getString(R.string.settings_show_recent),
                        detail = context.getString(R.string.settings_show_recent_desc),
                        checked = settings.showRecentExtractions,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(showRecentExtractions = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = context.getString(R.string.settings_default_sort),
                        options = BrowserSortMode.entries.map { it.label() },
                        selectedIndex =
                            BrowserSortMode.entries.indexOf(settings.defaultSortMode),
                        onSelected = { index ->
                            val mode = BrowserSortMode.entries[index]
                            repository.update {
                                it.copy(
                                    defaultSortMode = mode,
                                    sortAscending = mode == BrowserSortMode.NAME ||
                                        mode == BrowserSortMode.TYPE
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_browser_location),
                        detail = browserPath.ifBlank { context.getString(R.string.settings_not_selected) },
                        onClick = onChooseBrowserFolder
                    )
                }
            }
            }
            if (activeCategory == SettingsCategory.QUICK_ACCESS) {
            item {
                SettingsSection(context.getString(R.string.settings_quick_access), showHeader = false) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            context.getString(R.string.settings_quick_locations_count, quickLocationRepository.locations.size),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { openQuickLocationEditor(null) }) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(context.getString(R.string.settings_new), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    if (quickLocationRepository.locations.isEmpty()) {
                        Text(
                            context.getString(R.string.settings_no_quick_locations),
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
                        title = context.getString(R.string.settings_restore_quick_access),
                        detail = context.getString(R.string.settings_restore_quick_access_desc),
                        onClick = {
                            quickLocationRepository.reset()
                            onNotify(context.getString(R.string.settings_quick_access_restored))
                        }
                    )
                }
            }
            }
            if (activeCategory == SettingsCategory.EXTRACT_DEFAULTS) {
            item {
                SettingsSection(context.getString(R.string.settings_default_extraction), showHeader = false) {
                    SettingsChoice(
                        title = context.getString(R.string.settings_default_destination),
                        options = DefaultDestinationMode.entries.map { it.label() },
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
                            title = context.getString(R.string.settings_fixed_destination),
                            detail = settings.customDestinationName ?: context.getString(R.string.settings_choose_folder),
                            onClick = { customDestinationLauncher.launch(null) }
                        )
                    }
                    SettingsDivider()
                    SettingsChoice(
                        title = context.getString(R.string.settings_conflict),
                        options = ConflictPolicy.entries.map { it.label() },
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
                        title = context.getString(R.string.settings_filename_encoding),
                        options = FileNameEncoding.entries.map { it.label() },
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
                        title = context.getString(R.string.settings_fill_password_clipboard),
                        detail = context.getString(R.string.settings_fill_password_clipboard_desc),
                        checked = settings.fillPasswordFromClipboard,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(fillPasswordFromClipboard = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_customize_extract_sheet),
                        detail = context.getString(R.string.settings_customize_extract_sheet_desc),
                        onClick = { editingExtractLayout = true }
                    )
                }
            }
            }
            if (activeCategory == SettingsCategory.PERMISSIONS) {
            item {
                SettingsSection(context.getString(R.string.settings_notifications_permissions), showHeader = false) {
                    SettingsStatus(
                        title = context.getString(R.string.settings_all_files_access),
                        detail = if (storageGranted) context.getString(R.string.settings_granted) else context.getString(R.string.settings_not_granted),
                        action = if (storageGranted) context.getString(R.string.settings_manage) else context.getString(R.string.settings_grant),
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
                        title = context.getString(R.string.settings_notifications),
                        detail = if (notificationsEnabled) context.getString(R.string.settings_enabled) else context.getString(R.string.settings_disabled),
                        action = if (notificationsEnabled) context.getString(R.string.settings_manage) else context.getString(R.string.settings_enable),
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
                        title = context.getString(R.string.settings_task_notifications),
                        detail = context.getString(R.string.settings_task_notifications_desc),
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
                SettingsSection(context.getString(R.string.settings_files_tasks), showHeader = false) {
                    SettingsSwitch(
                        title = context.getString(R.string.settings_delete_to_trash),
                        detail = context.getString(R.string.settings_delete_to_trash_desc),
                        checked = settings.useTrash,
                        onCheckedChange = {
                            repository.update { current ->
                                current.copy(useTrash = it)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = context.getString(R.string.settings_trash_retention),
                        options = TrashRetention.entries.map { it.label() },
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
                                    onNotify(context.getString(R.string.settings_trash_cleaned, cleaned))
                                }
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = context.getString(R.string.settings_trash_quota),
                        options = TrashQuota.entries.map { it.label() },
                        selectedIndex = TrashQuota.entries.indexOf(settings.trashQuota),
                        onSelected = { index ->
                            repository.update {
                                it.copy(trashQuota = TrashQuota.entries[index])
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsChoice(
                        title = context.getString(R.string.settings_restore_conflict),
                        options = RestoreConflictPolicy.entries.map { it.label() },
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
                        title = context.getString(R.string.settings_browse_one_folder),
                        detail = if (browserPath.isBlank()) {
                            context.getString(R.string.settings_current_all_files)
                        } else {
                            context.getString(R.string.settings_current_path, browserPath)
                        },
                        onClick = onChooseBrowserFolder
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_trash),
                        detail = context.getString(R.string.settings_trash_desc),
                        onClick = { activePopup = SettingsPopup.TRASH }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_clear_vault),
                        detail = context.getString(R.string.settings_clear_vault_desc),
                        onClick = {
                            com.decompress.app.data.PasswordVault.clear(context)
                            onNotify(context.getString(R.string.settings_vault_cleared))
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_clear_open_with),
                        detail = context.getString(R.string.settings_clear_open_with_desc),
                        onClick = {
                            OpenWithPreferences.clear(context)
                            onNotify(context.getString(R.string.settings_open_with_cleared))
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_file_association),
                        detail = context.getString(R.string.settings_file_association_desc),
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
                        title = context.getString(R.string.settings_background_tasks),
                        detail = context.getString(R.string.settings_background_tasks_desc),
                        onClick = { activePopup = SettingsPopup.TASK_QUEUE }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_clear_finished_tasks),
                        detail = context.getString(R.string.settings_clear_finished_tasks_desc),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    WorkManager.getInstance(context).pruneWork()
                                }
                                onNotify(context.getString(R.string.settings_task_history_cleared))
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_cancel_all_tasks),
                        detail = context.getString(R.string.settings_cancel_all_tasks_desc),
                        destructive = true,
                        onClick = {
                            WorkManager.getInstance(context).apply {
                                cancelAllWorkByTag(ExtractionScheduler.TAG_ALL_TASKS)
                                cancelAllWorkByTag(CompressionScheduler.TAG_ALL_TASKS)
                            }
                            onNotify(context.getString(R.string.settings_tasks_cancelled))
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_reset_settings),
                        detail = context.getString(R.string.settings_reset_settings_desc),
                        destructive = true,
                        onClick = {
                            repository.reset()
                            storageGranted = hasAllFilesAccess(context)
                            notificationsEnabled =
                                NotificationManagerCompat.from(context)
                                    .areNotificationsEnabled()
                            onNotify(context.getString(R.string.settings_settings_reset))
                        }
                    )
                }
            }
            }
            if (activeCategory == SettingsCategory.ABOUT) {
            item {
                SettingsSection(context.getString(R.string.settings_about), showHeader = false) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checkingUpdate) return@clickable
                                checkingUpdate = true
                                scope.launch {
                                    onNotify(context.getString(R.string.settings_checking_update))
                                    UpdateChecker.check(versionName)
                                        .onSuccess { info ->
                                            if (info == null) {
                                                onNotify(context.getString(R.string.settings_latest_version, versionName))
                                            } else {
                                                onNotify(context.getString(R.string.settings_update_found, info.version))
                                                runCatching {
                                                    UpdateChecker.download(context, info)
                                                }.onSuccess { file ->
                                                    onNotify(
                                                    context.getString(
                                                        R.string.settings_download_done
                                                    )
                                                )
                                                    UpdateChecker.install(context, file)
                                                }.onFailure {
                                                    onNotify(
                                                        context.getString(
                                                        R.string.settings_download_failed,
                                                        it.message
                                                            ?: context.getString(R.string.settings_network_error)
                                                    )
                                                    )
                                                }
                                            }
                                        }
                                        .onFailure {
                                            onNotify(context.getString(
                                                R.string.settings_check_failed,
                                                it.message
                                                    ?: context.getString(R.string.settings_network_unavailable)
                                            ))
                                        }
                                    checkingUpdate = false
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            "Zippest $versionName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            context.getString(R.string.settings_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            context.getString(R.string.settings_check_updates_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_app_info),
                        detail = context.getString(R.string.settings_app_info_desc),
                        onClick = { activePopup = SettingsPopup.ABOUT_INFO }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_privacy_policy),
                        detail = context.getString(R.string.settings_privacy_policy_desc),
                        onClick = { activePopup = SettingsPopup.PRIVACY_POLICY }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_third_party),
                        detail = context.getString(R.string.settings_third_party_desc),
                        onClick = { activePopup = SettingsPopup.OPEN_SOURCE_LICENSES }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_feedback),
                        detail = context.getString(R.string.settings_feedback_desc),
                        onClick = { activePopup = SettingsPopup.FEEDBACK }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_diagnostics),
                        detail = context.getString(R.string.settings_diagnostics_desc),
                        onClick = {
                            com.decompress.app.data.DiagnosticsExport.share(context)
                        }
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = context.getString(R.string.settings_github_project),
                        detail = ZIPPEST_GITHUB_URL,
                        onClick = { openExternalLink(ZIPPEST_GITHUB_URL) }
                    )
                }
            }
            }
            item { Spacer(Modifier.height(12.dp).navigationBarsPadding()) }
        }
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
            onOpenLocalLicense = { activePopup = SettingsPopup.PROJECT_LICENSE },
            onDismiss = { activePopup = null }
        )
        SettingsPopup.PROJECT_LICENSE -> LegalTextDialog(
            title = context.getString(R.string.settings_license_desc),
            assetName = "legal/LICENSE",
            onDismiss = { activePopup = null }
        )
        SettingsPopup.PRIVACY_POLICY -> PrivacyPolicyDialog(
            onDismiss = { activePopup = null }
        )
        SettingsPopup.OPEN_SOURCE_LICENSES -> OpenSourceLicensesDialog(
            onOpenLink = ::openExternalLink,
            onDismiss = { activePopup = null }
        )
        SettingsPopup.FEEDBACK -> FeedbackDialog(
            versionName = versionName,
            versionCode = versionCode,
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
            title = { Text(context.getString(R.string.settings_delete_location_title)) },
            text = {
                Text(
                    context.getString(
                        R.string.settings_delete_location_question,
                        deletingQuickLocation?.displayLabel().orEmpty()
                    ) +
                        context.getString(R.string.settings_delete_location_body)
                )
            },
            confirmButton = {
                DialogDeleteButton(
                    onClick = {
                        deletingQuickLocation?.let {
                            quickLocationRepository.delete(it.id)
                        }
                        deletingQuickLocation = null
                        activePopup = null
                        onNotify(context.getString(R.string.settings_location_deleted))
                    },
                    text = context.getString(R.string.settings_delete)
                )
            },
            dismissButton = {
                DialogDismissButton(
                    onClick = { activePopup = null },
                    text = context.getString(R.string.settings_cancel)
                )
            }
        )
        null -> Unit
    }

    if (editingExtractLayout) {
        ExtractLayoutEditor(
            order = settings.extractOrder,
            hidden = settings.hiddenExtractSections,
            onOrderChange = { newOrder ->
                repository.update { it.copy(extractOrder = newOrder) }
            },
            onHiddenChange = { newHidden ->
                repository.update { it.copy(hiddenExtractSections = newHidden) }
            },
            onClose = { editingExtractLayout = false }
        )
    }
}

@Composable
private fun QuickLocationSettingsRow(
    location: QuickLocation,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
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
                location.displayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (location.path.isBlank()) {
                    context.getString(R.string.settings_internal_storage)
                } else {
                    context.getString(R.string.settings_internal_path, location.path)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = context.getString(R.string.settings_change_path),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = context.getString(R.string.settings_delete_location_title),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ExtractLayoutEditor(
    order: List<ExtractSection>,
    hidden: Set<ExtractSection>,
    onOrderChange: (List<ExtractSection>) -> Unit,
    onHiddenChange: (Set<ExtractSection>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 64.dp.toPx() }
    var dragging by remember { mutableStateOf<ExtractSection?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = context.getString(R.string.settings_back))
                    }
                },
                title = { Text(context.getString(R.string.settings_extract_sheet)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                context.getString(R.string.settings_extract_sheet_desc),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            order.filter { it !in hidden }.forEach { section ->
                val offset = if (dragging == section) dragOffset else 0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .offset { IntOffset(0, offset.roundToInt()) }
                        .clip(RoundedCornerShape(ZippestRadius.sm))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "≡",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .pointerInput(order) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragging = section
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                    },
                                    onDragEnd = {
                                        val start = order.indexOf(section)
                                        val shift = (dragOffset / rowHeightPx).roundToInt()
                                        val target = (start + shift)
                                            .coerceIn(0, order.lastIndex)
                                        if (target != start) {
                                            val updated = order.toMutableList().apply {
                                                removeAt(start)
                                                add(target, section)
                                            }
                                            onOrderChange(updated)
                                        }
                                        dragging = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        dragging = null
                                        dragOffset = 0f
                                    }
                                )
                            },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            section.label(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            section.detail(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onHiddenChange(hidden + section) }) {
                        Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = context.getString(R.string.settings_hide_section, section.label())
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (hidden.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    context.getString(R.string.settings_hidden),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                hidden.forEach { section ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(ZippestRadius.sm))
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            section.label(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onHiddenChange(hidden - section) }) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = context.getString(R.string.settings_restore_section, section.label())
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
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
            shape = RoundedCornerShape(ZippestRadius.md),
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
    val context = LocalContext.current
    var dialogOpen by remember { mutableStateOf(false) }
    var pendingIndex by remember(selectedIndex) { mutableIntStateOf(selectedIndex) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { dialogOpen = true }
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
        Dialog(onDismissRequest = { dialogOpen = false }) {
            Surface(
                shape = RoundedCornerShape(ZippestRadius.xl),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    WheelPicker(
                        options = options,
                        initialIndex = selectedIndex,
                        onCentered = { pendingIndex = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { dialogOpen = false }) {
                            Text(
                                context.getString(R.string.settings_cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSelected(pendingIndex)
                                dialogOpen = false
                            }
                        ) {
                            Text(context.getString(R.string.settings_confirm))
                        }
                    }
                }
            }
        }
    }
}

/** 滚轮选择：停在哪一项就选中哪一项，点某一行会吸附并居中。 */
@Composable
private fun WheelPicker(
    options: List<String>,
    initialIndex: Int,
    onCentered: (Int) -> Unit
) {
    if (options.isEmpty()) return
    val itemHeight = 44.dp
    val visibleCount = 5
    val pickerHeight = itemHeight * visibleCount
    val state = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, options.lastIndex)
    ) { options.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.currentPage) { onCentered(state.currentPage) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pickerHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    RoundedCornerShape(ZippestRadius.xs)
                )
        )
        VerticalPager(
            state = state,
            modifier = Modifier.fillMaxSize(),
            pageSize = PageSize.Fixed(itemHeight),
            contentPadding = PaddingValues(vertical = (pickerHeight - itemHeight) / 2)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        scope.launch { state.animateScrollToPage(page) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    options[page],
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (page == state.currentPage) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (page == state.currentPage) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(20.dp))
        Box(modifier = Modifier.scale(0.82f)) {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                color = if (detail == context.getString(R.string.settings_granted) || detail == context.getString(R.string.settings_enabled)) {
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
