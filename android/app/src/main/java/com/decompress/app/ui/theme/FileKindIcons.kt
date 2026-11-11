package com.decompress.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 文件类型配色（8.4）：颜色只取语义色，不用彩虹。
 * `ARCHIVE` 用 tertiary（蓝灰），`MEDIA` 用 secondary（绿灰），`DOC` / `OTHER` 用次要文字色。
 */
enum class FileKind(val label: String) {
    FOLDER("文件夹"),
    ARCHIVE("压缩包"),
    IMAGE("图片"),
    VIDEO("视频"),
    AUDIO("音频"),
    DOC("文档"),
    CODE("代码"),
    APK("APK"),
    OTHER("文件")
}

private val ARCHIVE_EXTENSIONS = setOf(
    "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz",
    "zst", "br", "lz4", "lzma", "cpio", "jar", "apk", "iso", "001", "z01"
)
private val IMAGE_EXTENSIONS = setOf(
    "png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "heif", "svg"
)
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts")
private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "ape")
private val DOC_EXTENSIONS = setOf(
    "txt", "md", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "epub", "rtf"
)
private val CODE_EXTENSIONS = setOf(
    "kt", "java", "js", "ts", "html", "css", "json", "xml", "yml", "yaml",
    "py", "sh", "bat", "c", "cpp", "h", "gradle", "properties", "ini", "toml"
)

/** 按文件名判断类型；目录单独传 `isDirectory`。 */
fun fileKindOf(name: String, isDirectory: Boolean = false): FileKind {
    if (isDirectory) return FileKind.FOLDER
    val lower = name.lowercase()
    val extension = lower.substringAfterLast('.', "")
    if (lower.endsWith(".tar.gz") || lower.endsWith(".tar.bz2") || lower.endsWith(".tar.xz") ||
        lower.endsWith(".tar.zst") || lower.endsWith(".tar.br")
    ) return FileKind.ARCHIVE
    return when {
        "apk" == extension -> FileKind.APK
        extension in ARCHIVE_EXTENSIONS -> FileKind.ARCHIVE
        extension in IMAGE_EXTENSIONS -> FileKind.IMAGE
        extension in VIDEO_EXTENSIONS -> FileKind.VIDEO
        extension in AUDIO_EXTENSIONS -> FileKind.AUDIO
        extension in DOC_EXTENSIONS -> FileKind.DOC
        extension in CODE_EXTENSIONS -> FileKind.CODE
        else -> FileKind.OTHER
    }
}

/** 每种类型一个色相 + 一个底色；不接受界面层写死颜色。 */
data class FileKindColors(
    val container: Color,
    val content: Color
)

fun fileKindColors(kind: FileKind, scheme: androidx.compose.material3.ColorScheme): FileKindColors =
    when (kind) {
        FileKind.FOLDER -> FileKindColors(
            container = scheme.primaryContainer,
            content = scheme.primary
        )
        FileKind.ARCHIVE -> FileKindColors(
            container = scheme.tertiaryContainer,
            content = scheme.tertiary
        )
        FileKind.IMAGE, FileKind.VIDEO, FileKind.AUDIO -> FileKindColors(
            container = scheme.secondaryContainer,
            content = scheme.secondary
        )
        FileKind.DOC, FileKind.CODE, FileKind.APK -> FileKindColors(
            container = scheme.surfaceVariant,
            content = scheme.onSurfaceVariant
        )
        FileKind.OTHER -> FileKindColors(
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurfaceVariant
        )
    }
