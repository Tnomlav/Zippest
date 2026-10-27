package com.decompress.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.github.junrar.Archive as RarArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class MultiArchiveEngine(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val zipEngine = ZipArchiveEngine(context)

    fun supports(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".zip") ||
            lower.endsWith(".jar") ||
            lower.endsWith(".7z") ||
            lower.endsWith(".rar") ||
            lower.endsWith(".tar") ||
            lower.endsWith(".tar.gz") ||
            lower.endsWith(".tgz") ||
            lower.endsWith(".tar.bz2") ||
            lower.endsWith(".tbz2") ||
            lower.endsWith(".tar.xz") ||
            lower.endsWith(".txz") ||
            lower.endsWith(".gz") ||
            lower.endsWith(".bz2") ||
            lower.endsWith(".xz") ||
            lower.endsWith(".cpio")
    }

    suspend fun list(
        archiveUri: Uri,
        archiveName: String,
        password: String = ""
    ): List<ArchiveItem> {
        val lower = archiveName.lowercase()
        return when {
            lower.endsWith(".zip") || lower.endsWith(".jar") ->
                zipEngine.list(archiveUri)
            lower.endsWith(".7z") -> listSevenZ(archiveUri, password)
            lower.endsWith(".rar") -> listRar(archiveUri, password)
            else -> listStream(archiveUri, lower, archiveName)
        }
    }

    suspend fun extract(
        archiveUri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit
    ) {
        val lower = archiveName.lowercase()
        when {
            lower.endsWith(".zip") || lower.endsWith(".jar") ->
                zipEngine.extract(archiveUri, archiveName, settings, onProgress)
            lower.endsWith(".7z") ->
                extractSevenZ(archiveUri, archiveName, settings, onProgress)
            lower.endsWith(".rar") ->
                extractRar(archiveUri, archiveName, settings, onProgress)
            lower.endsWith(".tar") ||
                lower.endsWith(".tar.gz") ||
                lower.endsWith(".tgz") ||
                lower.endsWith(".tar.bz2") ||
                lower.endsWith(".tbz2") ||
                lower.endsWith(".tar.xz") ||
                lower.endsWith(".txz") ||
                lower.endsWith(".cpio") ->
                extractStream(archiveUri, lower, archiveName, settings, onProgress)
            lower.endsWith(".gz") ||
                lower.endsWith(".bz2") ||
                lower.endsWith(".xz") ->
                extractSingleCompressed(archiveUri, lower, archiveName, settings)
            else -> throw ArchiveException("暂不支持此压缩格式")
        }
    }

    suspend fun test(
        archiveUri: Uri,
        archiveName: String,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int {
        val lower = archiveName.lowercase()
        return when {
            lower.endsWith(".zip") || lower.endsWith(".jar") ->
                zipEngine.test(archiveUri, password, onProgress)
            lower.endsWith(".7z") ->
                testSevenZ(archiveUri, password, onProgress)
            lower.endsWith(".rar") ->
                testRar(archiveUri, password, onProgress)
            lower.endsWith(".tar") ||
                lower.endsWith(".tar.gz") ||
                lower.endsWith(".tgz") ||
                lower.endsWith(".tar.bz2") ||
                lower.endsWith(".tbz2") ||
                lower.endsWith(".tar.xz") ||
                lower.endsWith(".txz") ||
                lower.endsWith(".cpio") ->
                testStream(archiveUri, lower, onProgress)
            lower.endsWith(".gz") ||
                lower.endsWith(".bz2") ||
                lower.endsWith(".xz") -> {
                testSingleCompressed(archiveUri, lower)
                onProgress(1, 1)
                1
            }
            else -> throw ArchiveException("暂不支持此压缩格式")
        }
    }

    private suspend fun testSevenZ(
        uri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withTempArchive(uri) { file ->
        runCatching {
            val archive = if (password.isBlank()) {
                SevenZFile(file)
            } else {
                SevenZFile(file, password.toCharArray())
            }
            archive.use {
                val entries = it.entries.toList()
                if (entries.size > MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止测试")
                }
                val declaredBytes = entries.sumOf { entry ->
                    entry.size.coerceAtLeast(0L)
                }
                ensureSafeExpansion(declaredBytes, file.length())
                var writtenBytes = 0L
                entries.forEachIndexed { index, entry ->
                    if (!entry.isDirectory) {
                        it.getInputStream(entry).use { input ->
                            writtenBytes += copyWithLimit(
                                input = input,
                                output = DiscardOutputStream,
                                remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                            )
                        }
                    }
                    onProgress(index + 1, entries.size)
                }
                entries.size
            }
        }.getOrElse { throw ArchiveException("7Z 测试失败：${it.archiveMessage()}", it) }
    }

    private suspend fun testRar(
        uri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withTempArchive(uri) { file ->
        runCatching {
            val archive = if (password.isBlank()) {
                RarArchive(file)
            } else {
                RarArchive(file, password)
            }
            archive.use { rar ->
                val headers = rar.fileHeaders
                if (headers.size > MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止测试")
                }
                val declaredBytes = headers.sumOf {
                    it.fullUnpackSize.coerceAtLeast(0L)
                }
                ensureSafeExpansion(declaredBytes, file.length())
                var writtenBytes = 0L
                headers.forEachIndexed { index, header ->
                    if (!header.isDirectory) {
                        rar.getInputStream(header).use { input ->
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
            }
        }.getOrElse { throw ArchiveException("RAR 测试失败：${it.archiveMessage()}", it) }
    }

    private suspend fun testStream(
        uri: Uri,
        lowerName: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        openStreamArchive(uri, lowerName).use { archive ->
            val packedSize = fileSize(uri)
            var entry = archive.nextEntry
            var processed = 0
            var writtenBytes = 0L
            while (entry != null) {
                if (processed >= MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止测试")
                }
                if (!entry.isDirectory) {
                    writtenBytes += copyWithLimit(
                        input = archive,
                        output = DiscardOutputStream,
                        remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                    )
                }
                ensureSafeExpansion(writtenBytes, packedSize)
                processed++
                onProgress(processed, processed)
                entry = archive.nextEntry
            }
            processed
        }
    }

    private fun testSingleCompressed(uri: Uri, lowerName: String) {
        val input = resolver.openInputStream(uri)
            ?: throw ArchiveException("无法读取压缩包")
        val compressed: InputStream = when {
            lowerName.endsWith(".gz") ->
                GzipCompressorInputStream(BufferedInputStream(input))
            lowerName.endsWith(".bz2") ->
                BZip2CompressorInputStream(BufferedInputStream(input))
            else -> XZCompressorInputStream(BufferedInputStream(input))
        }
        compressed.use { stream ->
            val written = copyWithLimit(
                input = stream,
                output = DiscardOutputStream,
                remainingBytes = MAX_TOTAL_BYTES
            )
            ensureSafeExpansion(written, fileSize(uri))
        }
    }

    private suspend fun listSevenZ(uri: Uri, password: String): List<ArchiveItem> =
        withTempArchive(uri) { file ->
            runCatching {
                val sevenZ = if (password.isBlank()) {
                    SevenZFile(file)
                } else {
                    SevenZFile(file, password.toCharArray())
                }
                sevenZ.use { archive ->
                    archive.entries.map { entry ->
                        ArchiveItem(
                            path = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size.coerceAtLeast(0L)
                        )
                    }.take(MAX_ENTRIES)
                }
            }.getOrElse { throw ArchiveException("无法读取 7Z：${it.archiveMessage()}", it) }
        }

    private suspend fun extractSevenZ(
        uri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit
    ) = withTempArchive(uri) { file ->
        runCatching {
            val sevenZ = if (settings.password.isBlank()) {
                SevenZFile(file)
            } else {
                SevenZFile(file, settings.password.toCharArray())
            }
            sevenZ.use { archive ->
                val entries = archive.entries.toList()
                if (entries.size > MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止解压")
                }
                val declaredBytes = entries.sumOf { it.size.coerceAtLeast(0L) }
                ensureSafeExpansion(declaredBytes, file.length())
                ensureLocalFreeSpace(context, settings.targetTreeUri, declaredBytes)
                val output = outputWriter(settings)
                val destination = prepareDestination(output, settings, archiveName)
                var writtenBytes = 0L
                entries.forEachIndexed { index, entry ->
                    writtenBytes += writeEntry(
                        output = output,
                        destination = destination,
                        path = entry.name,
                        isDirectory = entry.isDirectory,
                        settings = settings,
                        inputFactory = {
                            archive.getInputStream(entry)
                        },
                        closeInput = true,
                        remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                    )
                    ensureSafeExpansion(writtenBytes, file.length())
                    onProgress(index + 1, entries.size)
                }
            }
        }.getOrElse { throw ArchiveException("7Z 解压失败：${it.archiveMessage()}", it) }
    }

    private suspend fun listRar(uri: Uri, password: String): List<ArchiveItem> =
        withTempArchive(uri) { file ->
            runCatching {
                val archive = if (password.isBlank()) {
                    RarArchive(file)
                } else {
                    RarArchive(file, password)
                }
                archive.use {
                    it.fileHeaders.take(MAX_ENTRIES).map { header ->
                        ArchiveItem(
                            path = header.fileName,
                            isDirectory = header.isDirectory,
                            size = header.fullUnpackSize.coerceAtLeast(0L)
                        )
                    }
                }
            }.getOrElse { throw ArchiveException("无法读取 RAR：${it.archiveMessage()}", it) }
        }

    private suspend fun extractRar(
        uri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit
    ) = withTempArchive(uri) { file ->
        runCatching {
            val archive = if (settings.password.isBlank()) {
                RarArchive(file)
            } else {
                RarArchive(file, settings.password)
            }
            archive.use { rar ->
                val headers = rar.fileHeaders
                if (headers.size > MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止解压")
                }
                val declaredBytes = headers.sumOf {
                    it.fullUnpackSize.coerceAtLeast(0L)
                }
                ensureSafeExpansion(declaredBytes, file.length())
                ensureLocalFreeSpace(context, settings.targetTreeUri, declaredBytes)
                val output = outputWriter(settings)
                val destination = prepareDestination(output, settings, archiveName)
                var writtenBytes = 0L
                headers.forEachIndexed { index, header ->
                    writtenBytes += writeEntry(
                        output = output,
                        destination = destination,
                        path = header.fileName,
                        isDirectory = header.isDirectory,
                        settings = settings,
                        inputFactory = {
                            rar.getInputStream(header)
                        },
                        closeInput = true,
                        remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                    )
                    ensureSafeExpansion(writtenBytes, file.length())
                    onProgress(index + 1, headers.size)
                }
            }
        }.getOrElse { throw ArchiveException("RAR 解压失败：${it.archiveMessage()}", it) }
    }

    private suspend fun listStream(
        uri: Uri,
        lowerName: String,
        archiveName: String
    ): List<ArchiveItem> {
        if (lowerName.endsWith(".gz") && !lowerName.endsWith(".tar.gz") ||
            lowerName.endsWith(".bz2") && !lowerName.endsWith(".tar.bz2") ||
            lowerName.endsWith(".xz") && !lowerName.endsWith(".tar.xz")
        ) {
            return listOf(
                ArchiveItem(
                    path = singleOutputName(archiveName, lowerName),
                    isDirectory = false,
                    size = 0L
                )
            )
        }

        return withContext(Dispatchers.IO) {
            openStreamArchive(uri, lowerName).use { archive ->
                val result = mutableListOf<ArchiveItem>()
                var entry: ArchiveEntry? = archive.nextEntry
                while (entry != null) {
                    if (result.size >= MAX_ENTRIES) {
                        throw ArchiveException("压缩包条目过多，已停止读取")
                    }
                    result += ArchiveItem(
                        path = entry.name,
                        isDirectory = entry.isDirectory,
                        size = entry.size.coerceAtLeast(0L)
                    )
                    entry = archive.nextEntry
                }
                result
            }
        }
    }

    private suspend fun extractStream(
        uri: Uri,
        lowerName: String,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        openStreamArchive(uri, lowerName).use { archive ->
            val output = outputWriter(settings)
            val destination = prepareDestination(output, settings, archiveName)
            var entry: ArchiveEntry? = archive.nextEntry
            var processed = 0
            var writtenBytes = 0L
            val packedSize = fileSize(uri)
            while (entry != null) {
                if (processed >= MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止解压")
                }
                writtenBytes += writeEntry(
                    output = output,
                    destination = destination,
                    path = entry.name,
                    isDirectory = entry.isDirectory,
                    settings = settings,
                    inputFactory = { archive },
                    closeInput = false,
                    remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                )
                ensureSafeExpansion(writtenBytes, packedSize)
                processed++
                onProgress(processed, processed)
                entry = archive.nextEntry
            }
        }
    }

    private suspend fun extractSingleCompressed(
        uri: Uri,
        lowerName: String,
        archiveName: String,
        settings: ExtractSettings
    ) = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(uri)
            ?: throw ArchiveException("无法读取压缩包")
        val compressed: InputStream = when {
            lowerName.endsWith(".gz") -> GzipCompressorInputStream(BufferedInputStream(input))
            lowerName.endsWith(".bz2") -> BZip2CompressorInputStream(BufferedInputStream(input))
            else -> XZCompressorInputStream(BufferedInputStream(input))
        }
        compressed.use { stream ->
            val output = outputWriter(settings)
            val destination = prepareDestination(output, settings, archiveName)
            val name = sanitizePath(singleOutputName(archiveName, lowerName)).lastOrNull()
                ?: throw ArchiveException("无法确定输出文件名")
            val fileUri = output.createFile(destination, name, settings.conflictPolicy)
                ?: return@use
            resolver.openOutputStream(fileUri)?.use { out ->
                val written = copyWithLimit(
                    input = stream,
                    output = out,
                    remainingBytes = MAX_TOTAL_BYTES
                )
                ensureSafeExpansion(written, fileSize(uri))
            } ?: throw ArchiveException("无法写入解压文件")
        }
    }

    private fun openStreamArchive(uri: Uri, lowerName: String): ArchiveInputStream<*> {
        val input = resolver.openInputStream(uri)
            ?: throw ArchiveException("无法读取压缩包")
        val buffered = BufferedInputStream(input)
        return when {
            lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz") ->
                TarArchiveInputStream(GzipCompressorInputStream(buffered))
            lowerName.endsWith(".tar.bz2") || lowerName.endsWith(".tbz2") ->
                TarArchiveInputStream(BZip2CompressorInputStream(buffered))
            lowerName.endsWith(".tar.xz") || lowerName.endsWith(".txz") ->
                TarArchiveInputStream(XZCompressorInputStream(buffered))
            lowerName.endsWith(".tar") -> TarArchiveInputStream(buffered)
            lowerName.endsWith(".cpio") -> CpioArchiveInputStream(buffered)
            else -> throw ArchiveException("暂不支持此压缩格式")
        }
    }

    private fun writeEntry(
        output: ArchiveOutput,
        destination: Uri,
        path: String,
        isDirectory: Boolean,
        settings: ExtractSettings,
        inputFactory: () -> InputStream,
        closeInput: Boolean,
        remainingBytes: Long
    ): Long {
        val segments = sanitizePath(path)
        if (segments.isEmpty()) return 0L
        var parent = destination
        for (part in segments.dropLast(1)) {
            parent = output.ensureDirectory(parent, part)
                ?: throw ArchiveException("无法创建目录：$part")
        }
        if (isDirectory) {
            output.ensureDirectory(parent, segments.last())
            return 0L
        }
        val fileUri = output.createFile(
            parentUri = parent,
            name = segments.last(),
            conflictPolicy = settings.conflictPolicy
        ) ?: return 0L
        val input = inputFactory()
        return try {
            resolver.openOutputStream(fileUri)?.use { out ->
                copyWithLimit(
                    input = input,
                    output = out,
                    remainingBytes = remainingBytes
                )
            } ?: throw ArchiveException("无法写入文件：${segments.last()}")
        } finally {
            if (closeInput) input.close()
        }
    }

    private fun fileSize(uri: Uri): Long =
        resolver.openFileDescriptor(uri, "r")?.use { it.statSize }.orZero()

    private fun Long?.orZero(): Long = this?.coerceAtLeast(0L) ?: 0L

    private fun outputWriter(settings: ExtractSettings): ArchiveOutput =
        ArchiveOutput(resolver, settings.targetTreeUri)

    private fun prepareDestination(
        output: ArchiveOutput,
        settings: ExtractSettings,
        archiveName: String
    ): Uri {
        if (!settings.createFolder) return settings.targetParentUri
        val baseName = singleOutputName(archiveName, archiveName.lowercase())
        return output.ensureDirectory(settings.targetParentUri, baseName)
            ?: throw ArchiveException("无法创建目标文件夹")
    }

    private fun singleOutputName(archiveName: String, lowerName: String): String {
        return when {
            lowerName.endsWith(".tar.gz") -> archiveName.dropLast(7)
            lowerName.endsWith(".tar.bz2") -> archiveName.dropLast(8)
            lowerName.endsWith(".tar.xz") -> archiveName.dropLast(7)
            lowerName.endsWith(".tgz") ||
                lowerName.endsWith(".tbz2") ||
                lowerName.endsWith(".txz") -> archiveName.substringBeforeLast('.')
            lowerName.endsWith(".gz") -> archiveName.dropLast(3)
            lowerName.endsWith(".bz2") -> archiveName.dropLast(4)
            lowerName.endsWith(".xz") -> archiveName.dropLast(3)
            else -> archiveName.substringBeforeLast('.', archiveName)
        }
    }

    private suspend fun <T> withTempArchive(uri: Uri, block: (File) -> T): T =
        withContext(Dispatchers.IO) {
            val temp = File.createTempFile("decompress-", ".tmp", context.cacheDir)
            try {
                resolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } ?: throw ArchiveException("无法读取压缩包")
                block(temp)
            } finally {
                temp.delete()
            }
        }

    private fun sanitizePath(raw: String): List<String> {
        val segments = raw.replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." }
        if (segments.any { it == ".." }) return emptyList()
        return segments.mapNotNull { segment ->
            segment
                .replace(Regex("[\\u0000-\\u001F<>:\"|?*]"), "_")
                .trim()
                .ifBlank { null }
        }
    }

    private class ArchiveOutput(
        private val resolver: ContentResolver,
        private val treeUri: Uri
    ) {
        fun ensureDirectory(parentUri: Uri, name: String): Uri? {
            findChild(parentUri, name)?.let { return it }
            return createDocument(parentUri, name, Document.MIME_TYPE_DIR)
        }

        fun createFile(
            parentUri: Uri,
            name: String,
            conflictPolicy: ConflictPolicy
        ): Uri? {
            val existing = findChild(parentUri, name)
            if (existing != null) {
                when (conflictPolicy) {
                    ConflictPolicy.SKIP -> return null
                    ConflictPolicy.OVERWRITE ->
                        runCatching { DocumentsContract.deleteDocument(resolver, existing) }
                    ConflictPolicy.RENAME ->
                        return createUnique(parentUri, name)
                }
            }
            return createDocument(parentUri, name, "application/octet-stream")
        }

        private fun createUnique(parentUri: Uri, name: String): Uri? {
            val dot = name.lastIndexOf('.')
            val base = if (dot > 0) name.substring(0, dot) else name
            val suffix = if (dot > 0) name.substring(dot) else ""
            var index = 1
            while (index < 10_000) {
                val candidate = "$base ($index)$suffix"
                if (findChild(parentUri, candidate) == null) {
                    return createDocument(parentUri, candidate, "application/octet-stream")
                }
                index++
            }
            return null
        }

        private fun createDocument(parentUri: Uri, name: String, mime: String): Uri? =
            runCatching {
                DocumentsContract.createDocument(resolver, parentUri, mime, name)
            }.getOrNull()

        private fun findChild(parentUri: Uri, name: String): Uri? {
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
    }
}
