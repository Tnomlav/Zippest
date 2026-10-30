package com.decompress.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

enum class QuickLocationIcon {
    HOME,
    DOWNLOAD,
    PICTURES,
    CAMERA,
    MOVIES,
    MUSIC,
    DOCUMENTS,
    FOLDER
}

data class QuickLocation(
    val id: String,
    val label: String,
    val path: String,
    val icon: QuickLocationIcon = QuickLocationIcon.FOLDER
)

class QuickLocationRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE
    )

    var locations by mutableStateOf(
        preferences.getString(KEY_LOCATIONS, null)
            ?.let(::decodeQuickLocations)
            ?: defaultQuickLocations()
    )
        private set

    fun add(label: String, path: String): QuickLocation {
        val location = QuickLocation(
            id = "custom-${UUID.randomUUID()}",
            label = label.trim(),
            path = path,
            icon = QuickLocationIcon.FOLDER
        )
        persist(locations + location)
        return location
    }

    fun update(location: QuickLocation) {
        persist(locations.map { if (it.id == location.id) location else it })
    }

    fun delete(id: String) {
        persist(locations.filterNot { it.id == id })
    }

    fun reset() {
        persist(defaultQuickLocations())
    }

    private fun persist(updated: List<QuickLocation>) {
        locations = updated
        preferences.edit {
            putString(KEY_LOCATIONS, encodeQuickLocations(updated))
        }
    }

    companion object {
        private const val PREFERENCES = "zippest-quick-locations"
        private const val KEY_LOCATIONS = "locations"
    }
}

internal fun defaultQuickLocations(): List<QuickLocation> = listOf(
    QuickLocation("root", "目录首页", "", QuickLocationIcon.HOME),
    QuickLocation("downloads", "下载", "Download", QuickLocationIcon.DOWNLOAD),
    QuickLocation("pictures", "相册", "Pictures", QuickLocationIcon.PICTURES),
    QuickLocation("camera", "相机", "DCIM", QuickLocationIcon.CAMERA),
    QuickLocation("movies", "视频", "Movies", QuickLocationIcon.MOVIES),
    QuickLocation("documents", "文档", "Documents", QuickLocationIcon.DOCUMENTS)
)

internal fun normalizeQuickLocationPath(value: String): String? {
    var normalized = value.trim().replace('\\', '/')
    if (normalized.startsWith("primary:")) {
        normalized = normalized.removePrefix("primary:")
    }
    val absolutePrefixes = listOf("/storage/emulated/0", "/sdcard")
    absolutePrefixes.firstOrNull { prefix ->
        normalized == prefix || normalized.startsWith("$prefix/")
    }?.let { prefix ->
        normalized = normalized.removePrefix(prefix)
    }
    normalized = normalized.trim('/')
    if (normalized.isBlank()) return ""
    val parts = normalized.split('/').filter { it.isNotBlank() }
    if (parts.any { it == "." || it == ".." }) return null
    return parts.joinToString("/")
}

internal fun relativePathFromInternalTreeDocumentId(documentId: String): String? {
    if (!documentId.startsWith("primary:")) return null
    return normalizeQuickLocationPath(documentId.removePrefix("primary:"))
}

internal fun encodeQuickLocations(locations: List<QuickLocation>): String =
    locations.joinToString("\n") { location ->
        listOf(
            location.id,
            location.label,
            location.path,
            location.icon.name
        ).joinToString("|") { encodeField(it) }
    }

internal fun decodeQuickLocations(value: String): List<QuickLocation> =
    value.lineSequence().mapNotNull { line ->
        val fields = line.split('|').mapNotNull(::decodeField)
        if (fields.size != 4) return@mapNotNull null
        QuickLocation(
            id = fields[0],
            label = fields[1],
            path = fields[2],
            icon = QuickLocationIcon.entries.firstOrNull {
                it.name == fields[3]
            } ?: QuickLocationIcon.FOLDER
        )
    }.toList()

private fun encodeField(value: String): String = Base64.getUrlEncoder()
    .withoutPadding()
    .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

private fun decodeField(value: String): String? = runCatching {
    String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}.getOrNull()
