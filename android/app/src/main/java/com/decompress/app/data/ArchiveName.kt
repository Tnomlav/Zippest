package com.decompress.app.data

private val SPLIT_VOLUME_PATTERN = Regex(""".*\.(\d{3})$""")
private val NATIVE_ZIP_VOLUME_PATTERN =
    Regex("""(.*)\.z(\d{2,})$""", RegexOption.IGNORE_CASE)
private val RAR_PART_VOLUME_PATTERN =
    Regex("""^(.*)\.part(\d+)\.rar$""", RegexOption.IGNORE_CASE)
private val RAR_NUMBERED_VOLUME_PATTERN =
    Regex("""^(.*)\.(\d{3,})\.rar$""", RegexOption.IGNORE_CASE)
private val RAR_LEGACY_VOLUME_PATTERN =
    Regex("""^(.*)\.r(\d{2,})$""", RegexOption.IGNORE_CASE)
private val ARCHIVE_SUFFIXES = listOf(
    ".tar.zst",
    ".tar.br",
    ".tar.gz",
    ".tar.bz2",
    ".tar.xz",
    ".tbz2",
    ".tgz",
    ".txz",
    ".zip",
    ".jar",
    ".apk",
    ".iso",
    ".7z",
    ".rar",
    ".tar",
    ".gz",
    ".bz2",
    ".xz",
    ".lzma",
    ".lz4",
    ".zst",
    ".br",
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

/** 分卷的组名（去掉卷号）；不是分卷返回 null。 */
fun splitVolumeGroup(name: String): String? {
    SPLIT_VOLUME_PATTERN.matchEntire(name)?.let { return name.dropLast(4) }
    nativeZipBaseName(name)?.let { return it + ".zip" }
    parseRarPartVolume(name)?.let { return it.baseName + ".rar" }
    parseLegacyRarVolume(name)?.let { return it.baseName + ".rar" }
    return null
}

fun isFirstRarVolume(name: String): Boolean {
    parseRarPartVolume(name)?.let { return if (it.zeroBased) it.number == 0 else it.number == 1 }
    parseLegacyRarVolume(name)?.let { return it.number == 0 }
    return false
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

internal data class RarVolumeName(
    val baseName: String,
    val number: Int,
    val zeroBased: Boolean = false
)

internal fun parseRarPartVolume(name: String): RarVolumeName? {
    RAR_PART_VOLUME_PATTERN.matchEntire(name)?.let { match ->
        return RarVolumeName(
            baseName = match.groupValues[1],
            number = match.groupValues[2].toIntOrNull() ?: return null
        )
    }
    RAR_NUMBERED_VOLUME_PATTERN.matchEntire(name)?.let { match ->
        return RarVolumeName(
            baseName = match.groupValues[1],
            number = match.groupValues[2].toIntOrNull() ?: return null,
            zeroBased = true
        )
    }
    return null
}

internal fun parseLegacyRarVolume(name: String): RarVolumeName? {
    val match = RAR_LEGACY_VOLUME_PATTERN.matchEntire(name) ?: return null
    return RarVolumeName(
        baseName = match.groupValues[1],
        number = match.groupValues[2].toIntOrNull() ?: return null,
        zeroBased = true
    )
}

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

/**
 * 多选里同时勾了第一分卷和同名后续分卷时只保留第一分卷：
 * 引擎会自己收集同目录的其余分卷，重复传进去会互相覆盖。
 * 没有第一分卷时原样返回，让引擎给出「缺少第一卷」的提示。
 */
fun collapseSplitSelections(entries: List<BrowserEntry>): List<BrowserEntry> {
    val keep = splitSelectionKeepIndices(entries.map { it.name }).toSet()
    return entries.filterIndexed { index, _ -> index in keep }
}

/** 返回应该保留的下标；纯字符串实现，方便单元测试。 */
fun splitSelectionKeepIndices(names: List<String>): List<Int> {
    if (names.size <= 1) return names.indices.toList()
    val groups = names.map { splitVolumeGroup(it) }
    val zipVolumeGroups = names
        .filter { isFirstNativeZipVolume(it) || NATIVE_ZIP_VOLUME_PATTERN.matches(it) }
        .mapNotNull { splitVolumeGroup(it) }
        .toSet()
    fun groupOf(index: Int): String? =
        groups[index] ?: names[index].takeIf { name ->
            name.endsWith(".zip", ignoreCase = true) && name in zipVolumeGroups
        }
    val members = names.indices.groupBy { groupOf(it) }
    return names.indices.filter { index ->
        val group = groupOf(index) ?: return@filter true
        val first = members[group].orEmpty().firstOrNull { i ->
            isFirstSplitVolume(names[i]) ||
                isFirstNativeZipVolume(names[i]) ||
                isFirstRarVolume(names[i])
        }
        first == null || first == index
    }
}
