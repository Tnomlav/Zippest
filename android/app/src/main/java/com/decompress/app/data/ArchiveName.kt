package com.decompress.app.data

private val SPLIT_VOLUME_PATTERN = Regex(""".*\.(\d{3})$""")
private val NATIVE_ZIP_VOLUME_PATTERN =
    Regex("""(.*)\.z(\d{2,})$""", RegexOption.IGNORE_CASE)
private val ARCHIVE_SUFFIXES = listOf(
    ".tar.gz",
    ".tar.bz2",
    ".tar.xz",
    ".tbz2",
    ".tgz",
    ".txz",
    ".zip",
    ".jar",
    ".7z",
    ".rar",
    ".tar",
    ".gz",
    ".bz2",
    ".xz",
    ".lzma",
    ".lz4",
    ".cpio"
)

fun canonicalArchiveName(name: String): String = when {
    isFirstSplitVolume(name) -> name.dropLast(4)
    isFirstNativeZipVolume(name) -> nativeZipBaseName(name)?.plus(".zip") ?: name
    else -> name
}

fun isFirstSplitVolume(name: String): Boolean {
    val match = SPLIT_VOLUME_PATTERN.matchEntire(name) ?: return false
    return match.groupValues[1] == "001"
}

fun isSplitVolume(name: String): Boolean =
    SPLIT_VOLUME_PATTERN.matches(name) ||
        NATIVE_ZIP_VOLUME_PATTERN.matches(name)

fun isFirstNativeZipVolume(name: String): Boolean {
    val match = NATIVE_ZIP_VOLUME_PATTERN.matchEntire(name) ?: return false
    return match.groupValues[2] == "01"
}

fun nativeZipBaseName(name: String): String? =
    NATIVE_ZIP_VOLUME_PATTERN.matchEntire(name)?.groupValues?.get(1)

fun isSupportedArchiveName(name: String): Boolean {
    val lower = canonicalArchiveName(name).lowercase()
    return ARCHIVE_SUFFIXES.any(lower::endsWith) || isFirstNativeZipVolume(name)
}

fun archiveNameWithoutExtension(name: String): String {
    val canonical = canonicalArchiveName(name)
    val lower = canonical.lowercase()
    val suffix = ARCHIVE_SUFFIXES.firstOrNull(lower::endsWith)
        ?: return canonical.substringBeforeLast('.', canonical)
    return canonical.dropLast(suffix.length).ifBlank { "压缩包" }
}
