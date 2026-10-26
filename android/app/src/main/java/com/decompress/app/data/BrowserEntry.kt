package com.decompress.app.data

import android.net.Uri

data class BrowserEntry(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isArchive: Boolean
        get() = extension in ARCHIVE_EXTENSIONS

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true

    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true

    companion object {
        val ARCHIVE_EXTENSIONS = setOf(
            "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "tbz2",
            "xz", "txz", "zst", "lz4", "cab", "iso", "jar"
        )
    }
}
