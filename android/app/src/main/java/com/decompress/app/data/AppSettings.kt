package com.decompress.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

enum class DefaultDestinationMode(val label: String) {
    NEW_FOLDER("同名文件夹"),
    CURRENT_FOLDER("当前文件夹"),
    CUSTOM("固定位置")
}

enum class BrowserSortMode(val label: String) {
    NAME("名称"),
    SIZE("大小"),
    DATE("日期")
}

enum class TrashRetention(val label: String, val days: Int) {
    DAYS_7("7 天", 7),
    DAYS_30("30 天", 30),
    DAYS_90("90 天", 90),
    FOREVER("永久保留", 0)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showHiddenFiles: Boolean = false,
    val showThumbnails: Boolean = true,
    val fillPasswordFromClipboard: Boolean = false,
    val notifyOnComplete: Boolean = true,
    val defaultDestinationMode: DefaultDestinationMode = DefaultDestinationMode.NEW_FOLDER,
    val defaultConflictPolicy: ConflictPolicy = ConflictPolicy.RENAME,
    val defaultEncoding: FileNameEncoding = FileNameEncoding.AUTO,
    val defaultSortMode: BrowserSortMode = BrowserSortMode.NAME,
    val useTrash: Boolean = true,
    val trashRetention: TrashRetention = TrashRetention.DAYS_30,
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

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(settings)
        settings = updated
        preferences.edit {
            putString(KEY_THEME, updated.themeMode.name)
            putBoolean(KEY_SHOW_HIDDEN, updated.showHiddenFiles)
            putBoolean(KEY_SHOW_THUMBNAILS, updated.showThumbnails)
            putBoolean(KEY_FILL_CLIPBOARD_PASSWORD, updated.fillPasswordFromClipboard)
            putBoolean(KEY_NOTIFY_ON_COMPLETE, updated.notifyOnComplete)
            putString(KEY_DEFAULT_DESTINATION, updated.defaultDestinationMode.name)
            putString(KEY_DEFAULT_CONFLICT, updated.defaultConflictPolicy.name)
            putString(KEY_DEFAULT_ENCODING, updated.defaultEncoding.name)
            putString(KEY_DEFAULT_SORT, updated.defaultSortMode.name)
            putBoolean(KEY_USE_TRASH, updated.useTrash)
            putString(KEY_TRASH_RETENTION, updated.trashRetention.name)
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
        private const val KEY_SHOW_HIDDEN = "show_hidden"
        private const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
        private const val KEY_FILL_CLIPBOARD_PASSWORD = "fill_clipboard_password"
        private const val KEY_NOTIFY_ON_COMPLETE = "notify_on_complete"
        private const val KEY_DEFAULT_DESTINATION = "default_destination"
        private const val KEY_DEFAULT_CONFLICT = "default_conflict"
        private const val KEY_DEFAULT_ENCODING = "default_encoding"
        private const val KEY_DEFAULT_SORT = "default_sort"
        private const val KEY_USE_TRASH = "use_trash"
        private const val KEY_TRASH_RETENTION = "trash_retention"
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
            showHiddenFiles = preferences.getBoolean(KEY_SHOW_HIDDEN, false),
            showThumbnails = preferences.getBoolean(KEY_SHOW_THUMBNAILS, true),
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
            useTrash = preferences.getBoolean(KEY_USE_TRASH, true),
            trashRetention = preferences.enumValue(
                KEY_TRASH_RETENTION,
                TrashRetention.DAYS_30
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
