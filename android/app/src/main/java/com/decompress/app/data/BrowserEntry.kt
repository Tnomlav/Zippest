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
        get() = canonicalArchiveName(name).substringAfterLast('.', "").lowercase()

    val isArchive: Boolean
        get() = isSupportedArchiveName(name)

    val isFirstSplitVolume: Boolean
        get() = com.decompress.app.data.isFirstSplitVolume(name)

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true

    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true
}
