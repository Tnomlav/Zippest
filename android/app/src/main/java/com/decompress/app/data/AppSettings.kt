package com.decompress.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    SYSTEM,
    CHINESE,
    ENGLISH
}

enum class DefaultDestinationMode {
    NEW_FOLDER,
    CURRENT_FOLDER,
    CUSTOM
}

enum class AppAccent(
    val lightPrimary: Long,
    val lightContainer: Long,
    val lightOnContainer: Long,
    val darkPrimary: Long,
    val darkContainer: Long,
    val darkOnContainer: Long
) {
    GREEN(0xFF0B7A52, 0xFFD3F3E3, 0xFF04291C, 0xFF5FE3B4, 0xFF14503B, 0xFFD6FBEC),
    BLUE(0xFF1668D6, 0xFFDCE9FF, 0xFF06264F, 0xFF8FC0FF, 0xFF163A66, 0xFFD6E7FF),
    PURPLE(0xFF7A3FD1, 0xFFEEE3FF, 0xFF291349, 0xFFCBA6FF, 0xFF3B2A5C, 0xFFEFE1FF),
    AMBER(0xFFB45309, 0xFFFFE8D2, 0xFF401B00, 0xFFFFB877, 0xFF4F2A0D, 0xFFFFE3CC),
    ROSE(0xFFC22A63, 0xFFFFE0EA, 0xFF400A1F, 0xFFFF9FC4, 0xFF521F33, 0xFFFFDDE8),
    GRAPHITE(0xFF3F4A45, 0xFFE2E7E4, 0xFF1B211E, 0xFFC2CFC8, 0xFF2B342F, 0xFFE6EDE9)
}

enum class ExtractSection {
    PASSWORD,
    NAME,
    DESTINATION,
    CONFLICT,
    ENCODING
}

enum class BrowserSortMode {
    NAME,
    DATE,
    SIZE,
    TYPE
}

enum class TrashRetention(val days: Int) {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90),
    FOREVER(0)
}

enum class TrashQuota(val bytes: Long) {
    MB_500(500L * 1024 * 1024),
    GB_1(1L * 1024 * 1024 * 1024),
    GB_2(2L * 1024 * 1024 * 1024),
    GB_5(5L * 1024 * 1024 * 1024),
    UNLIMITED(0L)
}

enum class RestoreConflictPolicy {
    RENAME,
    OVERWRITE
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AppAccent = AppAccent.GREEN,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val extractOrder: List<ExtractSection> = ExtractSection.entries.toList(),
    val hiddenExtractSections: Set<ExtractSection> = emptySet(),
    val showHiddenFiles: Boolean = false,
    val showThumbnails: Boolean = true,
    val showRecentExtractions: Boolean = true,
    val fillPasswordFromClipboard: Boolean = false,
    val notifyOnComplete: Boolean = true,
    val defaultDestinationMode: DefaultDestinationMode = DefaultDestinationMode.NEW_FOLDER,
    val defaultConflictPolicy: ConflictPolicy = ConflictPolicy.RENAME,
    val defaultEncoding: FileNameEncoding = FileNameEncoding.AUTO,
    val defaultSortMode: BrowserSortMode = BrowserSortMode.NAME,
    val sortAscending: Boolean = true,
    val useTrash: Boolean = true,
    val trashRetention: TrashRetention = TrashRetention.DAYS_30,
    val trashQuota: TrashQuota = TrashQuota.GB_2,
    val restoreConflictPolicy: RestoreConflictPolicy = RestoreConflictPolicy.RENAME,
    val customDestinationTreeUri: String? = null,
    val customDestinationRootUri: String? = null,
    val customDestinationName: String? = null
)

class AppSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE
    )

    var settings by mutableStateOf(readPreferences(preferences))
        private set

    init {
        if (!preferences.getBoolean(KEY_SORT_MIGRATED, false)) {
            val expected = settings.defaultSortMode == BrowserSortMode.NAME ||
                settings.defaultSortMode == BrowserSortMode.TYPE
            if (settings.sortAscending != expected) {
                update { it.copy(sortAscending = expected) }
            }
            preferences.edit { putBoolean(KEY_SORT_MIGRATED, true) }
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(settings)
        settings = updated
        preferences.edit {
            putString(KEY_THEME, updated.themeMode.name)
            putString(KEY_ACCENT, updated.accentColor.name)
            putString(KEY_LANGUAGE, updated.language.name)
            putString(KEY_EXTRACT_ORDER, updated.extractOrder.joinToString(",") { it.name })
            putString(
                KEY_EXTRACT_HIDDEN,
                updated.hiddenExtractSections.joinToString(",") { it.name }
            )
            putBoolean(KEY_SHOW_HIDDEN, updated.showHiddenFiles)
            putBoolean(KEY_SHOW_THUMBNAILS, updated.showThumbnails)
            putBoolean(KEY_SHOW_RECENT, updated.showRecentExtractions)
            putBoolean(KEY_FILL_CLIPBOARD_PASSWORD, updated.fillPasswordFromClipboard)
            putBoolean(KEY_NOTIFY_ON_COMPLETE, updated.notifyOnComplete)
            putString(KEY_DEFAULT_DESTINATION, updated.defaultDestinationMode.name)
            putString(KEY_DEFAULT_CONFLICT, updated.defaultConflictPolicy.name)
            putString(KEY_DEFAULT_ENCODING, updated.defaultEncoding.name)
            putString(KEY_DEFAULT_SORT, updated.defaultSortMode.name)
            putBoolean(KEY_SORT_ASCENDING, updated.sortAscending)
            putBoolean(KEY_USE_TRASH, updated.useTrash)
            putString(KEY_TRASH_RETENTION, updated.trashRetention.name)
            putString(KEY_TRASH_QUOTA, updated.trashQuota.name)
            putString(KEY_RESTORE_CONFLICT, updated.restoreConflictPolicy.name)
            putString(KEY_CUSTOM_TREE, updated.customDestinationTreeUri)
            putString(KEY_CUSTOM_ROOT, updated.customDestinationRootUri)
            putString(KEY_CUSTOM_NAME, updated.customDestinationName)
        }
    }

    fun reset() {
        update { AppSettings() }
    }

    companion object {
        private const val PREFERENCES = "zippest-settings"
        private const val KEY_THEME = "theme"
        private const val KEY_ACCENT = "accent"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_EXTRACT_ORDER = "extract_order"
        private const val KEY_EXTRACT_HIDDEN = "extract_hidden"
        private const val KEY_SHOW_HIDDEN = "show_hidden"
        private const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
        private const val KEY_SHOW_RECENT = "show_recent_extractions"
        private const val KEY_FILL_CLIPBOARD_PASSWORD = "fill_clipboard_password"
        private const val KEY_NOTIFY_ON_COMPLETE = "notify_on_complete"
        private const val KEY_DEFAULT_DESTINATION = "default_destination"
        private const val KEY_DEFAULT_CONFLICT = "default_conflict"
        private const val KEY_DEFAULT_ENCODING = "default_encoding"
        private const val KEY_DEFAULT_SORT = "default_sort"
        private const val KEY_SORT_ASCENDING = "sort_ascending"
        private const val KEY_SORT_MIGRATED = "sort_direction_migrated"
        private const val KEY_USE_TRASH = "use_trash"
        private const val KEY_TRASH_RETENTION = "trash_retention"
        private const val KEY_TRASH_QUOTA = "trash_quota"
        private const val KEY_RESTORE_CONFLICT = "restore_conflict"
        private const val KEY_CUSTOM_TREE = "custom_tree"
        private const val KEY_CUSTOM_ROOT = "custom_root"
        private const val KEY_CUSTOM_NAME = "custom_name"

        fun read(context: Context): AppSettings = readPreferences(
            context.applicationContext.getSharedPreferences(
                PREFERENCES,
                Context.MODE_PRIVATE
            )
        )

        private fun readPreferences(
            preferences: android.content.SharedPreferences
        ): AppSettings = AppSettings(
            themeMode = preferences.enumValue(KEY_THEME, ThemeMode.SYSTEM),
            accentColor = preferences.enumValue(KEY_ACCENT, AppAccent.GREEN),
            language = preferences.enumValue(KEY_LANGUAGE, AppLanguage.SYSTEM),
            extractOrder = preferences.getString(KEY_EXTRACT_ORDER, null)
                ?.split(',')
                ?.mapNotNull { name -> ExtractSection.entries.firstOrNull { it.name == name } }
                ?.takeIf { it.isNotEmpty() }
                ?: ExtractSection.entries.toList(),
            hiddenExtractSections = preferences.getString(KEY_EXTRACT_HIDDEN, null)
                ?.split(',')
                ?.mapNotNull { name -> ExtractSection.entries.firstOrNull { it.name == name } }
                ?.toSet()
                ?: emptySet(),
            showHiddenFiles = preferences.getBoolean(KEY_SHOW_HIDDEN, false),
            showThumbnails = preferences.getBoolean(KEY_SHOW_THUMBNAILS, true),
            showRecentExtractions = preferences.getBoolean(KEY_SHOW_RECENT, true),
            fillPasswordFromClipboard = preferences.getBoolean(
                KEY_FILL_CLIPBOARD_PASSWORD,
                false
            ),
            notifyOnComplete = preferences.getBoolean(KEY_NOTIFY_ON_COMPLETE, true),
            defaultDestinationMode = preferences.enumValue(
                KEY_DEFAULT_DESTINATION,
                DefaultDestinationMode.NEW_FOLDER
            ),
            defaultConflictPolicy = preferences.enumValue(
                KEY_DEFAULT_CONFLICT,
                ConflictPolicy.RENAME
            ),
            defaultEncoding = preferences.enumValue(
                KEY_DEFAULT_ENCODING,
                FileNameEncoding.AUTO
            ),
            defaultSortMode = preferences.enumValue(
                KEY_DEFAULT_SORT,
                BrowserSortMode.NAME
            ),
            sortAscending = preferences.getBoolean(KEY_SORT_ASCENDING, true),
            useTrash = preferences.getBoolean(KEY_USE_TRASH, true),
            trashRetention = preferences.enumValue(
                KEY_TRASH_RETENTION,
                TrashRetention.DAYS_30
            ),
            trashQuota = preferences.enumValue(KEY_TRASH_QUOTA, TrashQuota.GB_2),
            restoreConflictPolicy = preferences.enumValue(
                KEY_RESTORE_CONFLICT,
                RestoreConflictPolicy.RENAME
            ),
            customDestinationTreeUri = preferences.getString(KEY_CUSTOM_TREE, null),
            customDestinationRootUri = preferences.getString(KEY_CUSTOM_ROOT, null),
            customDestinationName = preferences.getString(KEY_CUSTOM_NAME, null)
        )

        private inline fun <reified T : Enum<T>> android.content.SharedPreferences.enumValue(
            key: String,
            default: T
        ): T {
            val value = getString(key, null) ?: return default
            return enumValues<T>().firstOrNull { it.name == value } ?: default
        }
    }
}
