package com.decompress.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.text.format.Formatter
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.model.FileHeader as Zip4jFileHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ArchiveItem(
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)

data class ArchiveNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)

enum class DestinationMode {
    CURRENT_FOLDER,
    NEW_FOLDER,
    CUSTOM
}

enum class ConflictPolicy(val label: String) {
    OVERWRITE("覆盖"),
    SKIP("跳过"),
    RENAME("重命名")
}

enum class FileNameEncoding(val label: String, val charsetName: String) {
    AUTO("自动", "UTF-8"),
    GBK("GBK", "GBK"),
    UTF8("UTF-8", "UTF-8")
}

data class ExtractSettings(
    val mode: DestinationMode,
    val targetParentUri: Uri,
    val targetTreeUri: Uri,
    val createFolder: Boolean,
    val conflictPolicy: ConflictPolicy,
    val encoding: FileNameEncoding,
    val password: String,
    val onFileCreated: (Uri) -> Unit = {}
)

class ArchiveException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal fun ensureLocalFreeSpace(context: Context, treeUri: Uri, expectedBytes: Long) {
    if (expectedBytes <= 0L) return
    if (
        treeUri.authority != "com.android.externalstorage.documents" &&
        treeUri.scheme != "file"
    ) return
    val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
    val available = stat.availableBlocksLong * stat.blockSizeLong
    val reserve = 64L * 1024 * 1024
    if (expectedBytes > (available - reserve).coerceAtLeast(0L)) {
        throw ArchiveException(
            "可用空间不足：需要约 ${Formatter.formatShortFileSize(context, expectedBytes)}，" +
                "当前可用 ${Formatter.formatShortFileSize(context, available)}"
        )
    }
}

internal fun Uri.localFile(): File? =
    if (scheme == "file") path?.let(::File) else null

internal fun ensureSafeExpansion(unpackedBytes: Long, packedBytes: Long) {
    if (unpackedBytes > MAX_ARCHIVE_UNPACKED_BYTES) {
        throw ArchiveException("解压后体积超过安全上限")
    }
    if (
        unpackedBytes > SAFE_RATIO_BYTES &&
        packedBytes > 0L &&
        unpackedBytes / packedBytes > MAX_ARCHIVE_RATIO
    ) {
        throw ArchiveException("压缩比异常，已阻止可能的压缩炸弹")
    }
}

internal fun copyWithLimit(
    input: InputStream,
    output: OutputStream,
    remainingBytes: Long
): Long {
    if (remainingBytes < 0L) throw ArchiveException("解压后体积超过安全上限")
    val buffer = ByteArray(DEFAULT_ARCHIVE_BUFFER_SIZE)
    var written = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        written += read
        if (written > remainingBytes) {
            throw ArchiveException("解压后体积超过安全上限")
        }
        output.write(buffer, 0, read)
    }
    return written
}

internal fun Throwable.archiveMessage(): String {
    val detail = message?.trim().orEmpty()
    val lower = detail.lowercase()
    return when {
        "without a password" in lower ||
            "missing password" in lower ||
            "wrong password" in lower ||
            javaClass.simpleName == "WrongPasswordException" -> "密码错误或密码缺失"
        detail.isNotEmpty() -> detail
        else -> when (javaClass.simpleName) {
            "UnsupportedRarV5Exception" -> "当前 RAR5 压缩包不受支持"
            else -> javaClass.simpleName
        }
    }
}

internal object DiscardOutputStream : OutputStream() {
    override fun write(byte: Int) = Unit
    override fun write(bytes: ByteArray, offset: Int, length: Int) = Unit
}

private const val MAX_ARCHIVE_UNPACKED_BYTES = 20L * 1024 * 1024 * 1024
private const val SAFE_RATIO_BYTES = 1024L * 1024 * 1024
private const val MAX_ARCHIVE_RATIO = 1_000L
private const val DEFAULT_ARCHIVE_BUFFER_SIZE = 64 * 1024

class ZipArchiveEngine(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun list(archiveUri: Uri): List<ArchiveItem> = withContext(Dispatchers.IO) {
        runCatching {
            val input = resolver.openInputStream(archiveUri)
                ?: throw ArchiveException("无法读取压缩包")
            BufferedInputStream(input).use { buffered ->
                ZipInputStream(buffered).use { zip ->
                    val result = mutableListOf<ArchiveItem>()
                    var entry: ZipEntry? = zip.nextEntry
                    var count = 0
                    while (entry != null) {
                        if (++count > MAX_ENTRIES) {
                            throw ArchiveException("压缩包条目过多，已停止读取")
                        }
                        result += ArchiveItem(
                            path = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size.coerceAtLeast(0L)
                        )
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    result
                }
            }
        }.getOrElse { streamingError ->
            runCatching { listWithCentralDirectory(archiveUri) }
                .getOrElse { directoryError ->
                    throw ArchiveException(
                        "无法读取 ZIP：${directoryError.message ?: streamingError.message}",
                        directoryError
                    )
                }
        }
    }

    private fun listWithCentralDirectory(archiveUri: Uri): List<ArchiveItem> {
        val temp = File.createTempFile("zip-index-", ".zip", context.cacheDir)
        try {
            resolver.openInputStream(archiveUri)?.use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            } ?: throw ArchiveException("无法读取压缩包")
            val headers = Zip4jFile(temp).fileHeaders
            if (headers.size > MAX_ENTRIES) {
                throw ArchiveException("压缩包条目过多，已停止读取")
            }
            return headers.map { header ->
                ArchiveItem(
                    path = header.fileName,
                    isDirectory = header.isDirectory,
                    size = header.uncompressedSize.coerceAtLeast(0L)
                )
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun extract(
        archiveUri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit
    ) {
        if (settings.password.isNotEmpty()) {
            extractPasswordProtected(archiveUri, archiveName, settings, onProgress)
            return
        }

        withContext(Dispatchers.IO) {
            val input = resolver.openInputStream(archiveUri)
                ?: throw ArchiveException("无法读取压缩包")
            val zipCharset = Charset.forName(settings.encoding.charsetName)

            BufferedInputStream(input).use { buffered ->
                ZipInputStream(buffered, zipCharset).use { zip ->
                    val output = SafOutput(
                        resolver = resolver,
                        treeUri = settings.targetTreeUri,
                        rootDocumentUri = settings.targetParentUri
                    )
                    val destination = createDestination(output, archiveName, settings)

                    var entry: ZipEntry? = zip.nextEntry
                    var processed = 0
                    var declaredBytes = 0L
                    var writtenBytes = 0L
                    var totalCompressed = 0L
                    var entryCount = 0

                    while (entry != null) {
                        entryCount++
                        if (entryCount > MAX_ENTRIES) {
                            throw ArchiveException("压缩包条目过多，已停止解压")
                        }

                        declaredBytes += entry.size.coerceAtLeast(0L)
                        if (entry.compressedSize > 0) totalCompressed += entry.compressedSize
                        ensureSafeExpansion(declaredBytes, totalCompressed)

                        val segments = sanitizePath(entry.name)
                        if (segments.isNotEmpty()) {
                            var parent = destination
                            for (part in segments.dropLast(1)) {
                                parent = output.ensureDirectory(parent, part)
                                    ?: throw ArchiveException("无法创建目录：$part")
                            }
                            if (entry.isDirectory) {
                                output.ensureDirectory(parent, segments.last())
                            } else {
                                val fileUri = output.createFile(
                                    parentUri = parent,
                                    name = segments.last(),
                                    conflictPolicy = settings.conflictPolicy
                                )
                                if (fileUri != null) {
                                    output.openOutputStream(fileUri)?.use { out ->
                                        writtenBytes += copyWithLimit(
                                            input = zip,
                                            output = out,
                                            remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                                        )
                                    } ?: throw ArchiveException(
                                        "无法写入文件：${segments.last()}"
                                    )
                                    settings.onFileCreated(fileUri)
                                }
                            }
                        }

                        zip.closeEntry()
                        processed++
                        onProgress(processed, processed)
                        entry = zip.nextEntry
                    }
                }
            }
        }
    }

    suspend fun test(
        archiveUri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        if (password.isNotEmpty()) {
            return@withContext testPasswordProtected(archiveUri, password, onProgress)
        }
        val input = resolver.openInputStream(archiveUri)
            ?: throw ArchiveException("无法读取压缩包")
        BufferedInputStream(input).use { buffered ->
            ZipInputStream(buffered).use { zip ->
                var entry = zip.nextEntry
                var processed = 0
                var declaredBytes = 0L
                var compressedBytes = 0L
                while (entry != null) {
                    if (processed >= MAX_ENTRIES) {
                        throw ArchiveException("压缩包条目过多，已停止测试")
                    }
                    declaredBytes += entry.size.coerceAtLeast(0L)
                    compressedBytes += entry.compressedSize.coerceAtLeast(0L)
                    ensureSafeExpansion(declaredBytes, compressedBytes)
                    if (!entry.isDirectory) {
                        copyWithLimit(
                            input = zip,
                            output = DiscardOutputStream,
                            remainingBytes = MAX_TOTAL_BYTES - declaredBytes
                        )
                    }
                    zip.closeEntry()
                    processed++
                    onProgress(processed, processed)
                    entry = zip.nextEntry
                }
                processed
            }
        }
    }

    private fun testPasswordProtected(
        archiveUri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int {
        val temp = File.createTempFile("zip-test-", ".zip", context.cacheDir)
        try {
            resolver.openInputStream(archiveUri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw ArchiveException("无法读取压缩包")
            return runCatching {
                val archive = Zip4jFile(temp, password.toCharArray())
                val headers = archive.fileHeaders
                if (headers.size > MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止测试")
                }
                val declaredBytes = headers.sumOf {
                    it.uncompressedSize.coerceAtLeast(0L)
                }
                ensureSafeExpansion(declaredBytes, temp.length())
                var writtenBytes = 0L
                headers.forEachIndexed { index, header ->
                    if (!header.isDirectory) {
                        archive.getInputStream(header).use { input ->
                            writtenBytes += copyWithLimit(
                                input = input,
                                output = DiscardOutputStream,
                                remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                            )
                        }
                    }
                    onProgress(index + 1, headers.size)
                }
                headers.size
            }.getOrElse { error ->
                if (error is ArchiveException) throw error
                throw ArchiveException(
                    "ZIP 测试失败：${error.archiveMessage()}",
                    error
                )
            }
        } finally {
            temp.delete()
        }
    }

    private suspend fun extractPasswordProtected(
        archiveUri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("encrypted-zip-", ".zip", context.cacheDir)
        try {
            resolver.openInputStream(archiveUri)?.use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            } ?: throw ArchiveException("无法读取压缩包")

            runCatching {
                val archive = Zip4jFile(temp, settings.password.toCharArray())
                val headers = archive.fileHeaders
                if (headers.size > MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止解压")
                }
                val declaredBytes = headers.sumOf {
                    it.uncompressedSize.coerceAtLeast(0L)
                }
                ensureSafeExpansion(declaredBytes, temp.length())
                ensureLocalFreeSpace(context, settings.targetTreeUri, declaredBytes)

                val output = SafOutput(
                    resolver = resolver,
                    treeUri = settings.targetTreeUri,
                    rootDocumentUri = settings.targetParentUri
                )
                val destination = createDestination(output, archiveName, settings)
                var writtenBytes = 0L
                headers.forEachIndexed { index, header ->
                    writePasswordProtectedEntry(
                        archive = archive,
                        header = header,
                        output = output,
                        destination = destination,
                        settings = settings,
                        writtenBytes = writtenBytes
                    ).let { written ->
                        writtenBytes += written
                    }
                    onProgress(index + 1, headers.size)
                }
            }.getOrElse { error ->
                if (error is ArchiveException) throw error
                throw ArchiveException(
                    "ZIP 解压失败：${error.archiveMessage()}",
                    error
                )
            }
        } finally {
            temp.delete()
        }
    }

    private fun writePasswordProtectedEntry(
        archive: Zip4jFile,
        header: Zip4jFileHeader,
        output: SafOutput,
        destination: Uri,
        settings: ExtractSettings,
        writtenBytes: Long
    ): Long {
        val segments = sanitizePath(header.fileName)
        if (segments.isEmpty()) return 0L
        var parent = destination
        for (part in segments.dropLast(1)) {
            parent = output.ensureDirectory(parent, part)
                ?: throw ArchiveException("无法创建目录：$part")
        }
        if (header.isDirectory) {
            output.ensureDirectory(parent, segments.last())
            return 0L
        }
        val fileUri = output.createFile(
            parentUri = parent,
            name = segments.last(),
            conflictPolicy = settings.conflictPolicy
        ) ?: return 0L
        archive.getInputStream(header).use { input ->
            output.openOutputStream(fileUri)?.use { out ->
                val written = copyWithLimit(
                    input = input,
                    output = out,
                    remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                )
                settings.onFileCreated(fileUri)
                return written
            } ?: throw ArchiveException("无法写入文件：${segments.last()}")
        }
    }

    private fun createDestination(
        output: SafOutput,
        archiveName: String,
        settings: ExtractSettings
    ): Uri {
        val baseName = archiveName.substringBeforeLast('.', archiveName)
        return if (settings.createFolder) {
            output.ensureDirectory(settings.targetParentUri, baseName)
        } else {
            settings.targetParentUri
        } ?: throw ArchiveException("无法创建目标文件夹")
    }

    private fun sanitizePath(raw: String): List<String> {
        val normalized = raw.replace('\\', '/')
        val segments = normalized.split('/')
            .filter { it.isNotBlank() && it != "." }
        if (segments.any { it == ".." }) return emptyList()
        return segments.mapNotNull { segment ->
            val clean = segment
                .replace(Regex("[\\u0000-\\u001F<>:\"|?*]"), "_")
                .trim()
            clean.ifBlank { null }
        }
    }

    private class SafOutput(
        private val resolver: ContentResolver,
        private val treeUri: Uri,
        private val rootDocumentUri: Uri
    ) {
        fun ensureDirectory(parentUri: Uri, name: String): Uri? {
            parentUri.localFile()?.let { parent ->
                val directory = File(parent, name)
                if (directory.isDirectory) return Uri.fromFile(directory)
                return if (directory.mkdirs()) Uri.fromFile(directory) else null
            }
            findChild(parentUri, name)?.let { return it }
            return runCatching {
                DocumentsContract.createDocument(
                    resolver,
                    parentUri,
                    Document.MIME_TYPE_DIR,
                    name
                )
            }.getOrNull()
        }

        fun createFile(
            parentUri: Uri,
            name: String,
            conflictPolicy: ConflictPolicy
        ): Uri? {
            parentUri.localFile()?.let { parent ->
                val target = File(parent, name)
                if (target.exists()) {
                    when (conflictPolicy) {
                        ConflictPolicy.SKIP -> return null
                        ConflictPolicy.OVERWRITE -> {
                            if (!target.delete()) return null
                        }
                        ConflictPolicy.RENAME -> return createUniqueFile(
                            parentUri,
                            name
                        )
                    }
                }
                return createDocument(parentUri, name)
            }
            val existing = findChild(parentUri, name)
            if (existing != null) {
                when (conflictPolicy) {
                    ConflictPolicy.SKIP -> return null
                    ConflictPolicy.OVERWRITE -> {
                        runCatching { DocumentsContract.deleteDocument(resolver, existing) }
                    }
                    ConflictPolicy.RENAME -> {
                        return createUniqueFile(parentUri, name)
                    }
                }
            }
            return createDocument(parentUri, name)
        }

        fun openOutputStream(uri: Uri): OutputStream? =
            uri.localFile()?.let(::FileOutputStream)
                ?: resolver.openOutputStream(uri)

        private fun createUniqueFile(parentUri: Uri, name: String): Uri? {
            val dot = name.lastIndexOf('.')
            val base = if (dot > 0) name.substring(0, dot) else name
            val suffix = if (dot > 0) name.substring(dot) else ""
            var index = 1
            while (true) {
                val candidate = "$base ($index)$suffix"
                parentUri.localFile()?.let { parent ->
                    val target = File(parent, candidate)
                    if (!target.exists() && target.createNewFile()) {
                        return Uri.fromFile(target)
                    }
                    return@let
                }
                if (findChild(parentUri, candidate) == null) {
                    return createDocument(parentUri, candidate)
                }
                index++
                if (index > 10_000) return null
            }
        }

        private fun createDocument(parentUri: Uri, name: String): Uri? =
            parentUri.localFile()?.let { parent ->
                val target = File(parent, name)
                if (target.exists() || target.createNewFile()) {
                    Uri.fromFile(target)
                } else {
                    null
                }
            } ?: run {
            runCatching {
                DocumentsContract.createDocument(
                    resolver,
                    parentUri,
                    "application/octet-stream",
                    name
                )
            }.getOrNull()
            }

        private fun findChild(parentUri: Uri, name: String): Uri? {
            parentUri.localFile()?.let { parent ->
                val child = File(parent, name)
                return if (child.exists()) Uri.fromFile(child) else null
            }
            val parentId = DocumentsContract.getDocumentId(parentUri)
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            resolver.query(
                childrenUri,
                arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == name) {
                        return DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            cursor.getString(idIndex)
                        )
                    }
                }
            }
            return null
        }
    }

    companion object {
        private const val MAX_ENTRIES = 50_000
        private const val MAX_TOTAL_BYTES = 20L * 1024 * 1024 * 1024
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}
