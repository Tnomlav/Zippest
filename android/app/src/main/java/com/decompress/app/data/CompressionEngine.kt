package com.decompress.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.io.outputstream.SplitOutputStream as Zip4jSplitOutputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream as Zip4jOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel as Zip4jCompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod as Zip4jCompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream
import org.apache.commons.compress.archivers.cpio.CpioConstants
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Date

enum class CompressionFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val supportsPassword: Boolean,
    val acceptsMultipleSources: Boolean = true,
    val supportsCreation: Boolean = true
) {
    ZIP("ZIP", ".zip", "application/zip", true),
    SEVEN_Z("7Z", ".7z", "application/x-7z-compressed", true),
    TAR("TAR", ".tar", "application/x-tar", false),
    TAR_GZ("TAR.GZ", ".tar.gz", "application/gzip", false),
    TAR_BZ2("TAR.BZ2", ".tar.bz2", "application/x-bzip2", false),
    TAR_XZ("TAR.XZ", ".tar.xz", "application/x-xz", false),
    TAR_ZSTD("TAR.ZST", ".tar.zst", "application/zstd", false),
    TAR_BROTLI("TAR.BR", ".tar.br", "application/x-brotli", false, true, false),
    GZIP("GZ", ".gz", "application/gzip", false, false),
    BZIP2("BZ2", ".bz2", "application/x-bzip2", false, false),
    XZ("XZ", ".xz", "application/x-xz", false, false),
    LZMA("LZMA", ".lzma", "application/x-lzma", false, false),
    LZ4("LZ4", ".lz4", "application/x-lz4", false, false),
    ZSTD("ZST", ".zst", "application/zstd", false, false),
    BROTLI("BR", ".br", "application/x-brotli", false, false, false),
    CPIO("CPIO", ".cpio", "application/x-cpio", false)
}

enum class ArchiveCompressionLevel(val label: String, val zipLevel: Int) {
    STORE("仅存储", 0),
    FAST("快速", 3),
    NORMAL("标准", 6),
    MAXIMUM("最大", 9)
}

data class CompressionOptions(
    val format: CompressionFormat,
    val archiveName: String,
    val level: ArchiveCompressionLevel = ArchiveCompressionLevel.NORMAL,
    val password: String = "",
    val splitSizeBytes: Long? = null,
    val onCheckpoint: () -> Unit = {}
)

data class CompressionResult(
    val displayName: String,
    val primaryUri: Uri,
    val partUris: List<Uri>
)

class CompressionEngine(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val repository = SafFileRepository(context)

    suspend fun compress(
        selected: List<BrowserEntry>,
        sourceTreeUri: Uri,
        targetParentUri: Uri,
        targetTreeUri: Uri,
        options: CompressionOptions,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): CompressionResult = withContext(Dispatchers.IO) {
        require(selected.isNotEmpty()) { "请选择要压缩的文件" }
        if (!options.format.acceptsMultipleSources &&
            (selected.size != 1 || selected.first().isDirectory)
        ) {
            throw ArchiveException("${options.format.label} 只能压缩单个文件")
        }
        if (options.password.isNotEmpty() && !options.format.supportsPassword) {
            throw ArchiveException("${options.format.label} 不支持密码")
        }
        if (options.splitSizeBytes != null && options.splitSizeBytes < 64L * 1024) {
            throw ArchiveException("分卷大小不能小于 64 KB")
        }

        val nodes = collectNodes(selected, sourceTreeUri)
        if (nodes.isEmpty()) throw ArchiveException("没有可压缩的内容")
        val output = ArchiveOutputTarget(
            resolver = resolver,
            parentUri = targetParentUri,
            treeUri = targetTreeUri,
            requestedName = normalizedArchiveName(options.archiveName, options.format),
            mimeType = options.format.mimeType,
            splitSize = options.splitSizeBytes
        )
        try {
            when (options.format) {
                CompressionFormat.ZIP -> {
                    if (options.splitSizeBytes != null) {
                        writeNativeSplitZip(nodes, options, output, onProgress)
                    } else {
                        output.open().use { stream ->
                            writeZip(stream, nodes, options, onProgress)
                        }
                    }
                }
                CompressionFormat.SEVEN_Z -> {
                    val temp = File.createTempFile("zippest-create-", ".7z", context.cacheDir)
                    try {
                        writeSevenZ(temp, nodes, options, onProgress)
                        copyToOutput(temp, output)
                    } finally {
                        temp.delete()
                    }
                }
                else -> {
                    output.open().use { stream ->
                        writeArchive(stream, nodes, options, onProgress)
                    }
                }
            }
            output.finish()
            output.result()
        } catch (error: CancellationException) {
            output.abort()
            throw error
        } catch (error: Exception) {
            output.abort()
            if (error is ArchiveException) throw error
            throw ArchiveException(
                "创建 ${options.format.label} 失败：${error.message ?: error.javaClass.simpleName}",
                error
            )
        }
    }

    private fun writeNativeSplitZip(
        nodes: List<SourceNode>,
        options: CompressionOptions,
        output: ArchiveOutputTarget,
        onProgress: (Int, Int) -> Unit
    ) {
        val splitSize = options.splitSizeBytes ?: error("Split size missing")
        val temp = File.createTempFile("zippest-split-", ".zip", context.cacheDir)
        var splitFiles: List<File> = emptyList()
        try {
            temp.delete()
            Zip4jSplitOutputStream(temp, splitSize).use { splitOutput ->
                writeZip(
                    output = splitOutput,
                    nodes = nodes,
                    options = options,
                    onProgress = onProgress,
                    bufferOutput = false
                )
            }
            splitFiles = Zip4jFile(temp).splitZipFiles
            if (splitFiles.isEmpty()) {
                throw ArchiveException("未生成 ZIP 分卷")
            }
            val tempBase = temp.name.removeSuffix(".zip")
            val targetBase = output.archiveName.removeSuffix(".zip")
            val names = splitFiles.map { file ->
                val suffix = when {
                    file.name.endsWith(".zip", ignoreCase = true) -> ".zip"
                    file.name.startsWith(tempBase + ".", ignoreCase = true) ->
                        file.name.substring(tempBase.length)
                    else -> file.name
                }
                targetBase + suffix
            }
            output.copyGeneratedFiles(splitFiles, names)
        } finally {
            splitFiles.forEach { it.delete() }
            temp.delete()
        }
    }

    private suspend fun collectNodes(
        selected: List<BrowserEntry>,
        treeUri: Uri
    ): List<SourceNode> {
        val result = mutableListOf<SourceNode>()
        val rootNames = mutableSetOf<String>()
        suspend fun visit(entry: BrowserEntry, path: String) {
            if (result.size >= MAX_COMPRESSION_ENTRIES) {
                throw ArchiveException("项目数量过多，最多支持 $MAX_COMPRESSION_ENTRIES 项")
            }
            val safePath = sanitizeArchivePath(path)
            if (safePath.isEmpty()) return
            result += SourceNode(
                path = safePath,
                uri = entry.uri,
                isDirectory = entry.isDirectory,
                size = if (entry.isDirectory) 0L else entry.size.coerceAtLeast(0L),
                lastModified = entry.lastModified
            )
            if (!entry.isDirectory) return

            val children = entry.uri.localFile()?.let { directory ->
                repository.listDirectChildren(directory)
            } ?: repository.listChildren(treeUri, entry.uri)
            children.forEach { child ->
                visit(child, "$safePath/${child.name}")
            }
        }
        selected.forEach { entry ->
            val safeName = sanitizeArchivePath(entry.name)
            if (!rootNames.add(safeName)) {
                throw ArchiveException("所选项目中有同名内容：$safeName")
            }
            visit(entry, safeName)
        }
        return result
    }

    private fun writeArchive(
        output: OutputStream,
        nodes: List<SourceNode>,
        options: CompressionOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        when (options.format) {
            CompressionFormat.ZIP -> writeZip(output, nodes, options, onProgress)
            CompressionFormat.TAR,
            CompressionFormat.TAR_GZ,
            CompressionFormat.TAR_BZ2,
            CompressionFormat.TAR_XZ,
            CompressionFormat.TAR_ZSTD ->
                writeTar(output, nodes, options, onProgress)
            CompressionFormat.GZIP,
            CompressionFormat.BZIP2,
            CompressionFormat.XZ,
            CompressionFormat.LZMA,
            CompressionFormat.LZ4,
            CompressionFormat.ZSTD ->
                writeSingleStream(output, nodes.single(), options, onProgress)
            CompressionFormat.CPIO -> writeCpio(output, nodes, options, onProgress)
            CompressionFormat.TAR_BROTLI,
            CompressionFormat.BROTLI -> error("Brotli creation is not available")
            CompressionFormat.SEVEN_Z -> error("7Z is handled separately")
        }
    }

    private fun writeZip(
        output: OutputStream,
        nodes: List<SourceNode>,
        options: CompressionOptions,
        onProgress: (Int, Int) -> Unit,
        bufferOutput: Boolean = true
    ) {
        val password = options.password.toCharArray()
        val targetOutput = if (bufferOutput) BufferedOutputStream(output) else output
        val zip = if (password.isEmpty()) {
            Zip4jOutputStream(targetOutput)
        } else {
            Zip4jOutputStream(targetOutput, password)
        }
        zip.use {
            nodes.forEachIndexed { index, node ->
                val entryName = if (node.isDirectory) {
                    node.path.trimEnd('/') + "/"
                } else {
                    node.path
                }
                val parameters = ZipParameters().apply {
                    fileNameInZip = entryName
                    compressionMethod = if (options.level == ArchiveCompressionLevel.STORE) {
                        Zip4jCompressionMethod.STORE
                    } else {
                        Zip4jCompressionMethod.DEFLATE
                    }
                    compressionLevel = when (options.level) {
                        ArchiveCompressionLevel.STORE ->
                            Zip4jCompressionLevel.NO_COMPRESSION
                        ArchiveCompressionLevel.FAST ->
                            Zip4jCompressionLevel.FAST
                        ArchiveCompressionLevel.NORMAL ->
                            Zip4jCompressionLevel.NORMAL
                        ArchiveCompressionLevel.MAXIMUM ->
                            Zip4jCompressionLevel.MAXIMUM
                    }
                    isEncryptFiles = password.isNotEmpty() && !node.isDirectory
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    lastModifiedFileTime = node.lastModified
                }
                zip.putNextEntry(parameters)
                if (!node.isDirectory) {
                    openInput(node.uri, options.onCheckpoint).use {
                        it.copyTo(zip, BUFFER_SIZE)
                    }
                }
                zip.closeEntry()
                onProgress(index + 1, nodes.size)
            }
        }
    }

    private fun writeTar(
        output: OutputStream,
        nodes: List<SourceNode>,
        options: CompressionOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        val compressed: OutputStream = when (options.format) {
            CompressionFormat.TAR -> output
            CompressionFormat.TAR_GZ -> GzipCompressorOutputStream(
                output,
                GzipParameters().apply {
                    compressionLevel = options.level.zipLevel.coerceIn(1, 9)
                }
            )
            CompressionFormat.TAR_BZ2 -> BZip2CompressorOutputStream(
                output,
                options.level.zipLevel.coerceIn(1, 9)
            )
            CompressionFormat.TAR_XZ -> XZCompressorOutputStream(
                output,
                options.level.zipLevel.coerceIn(0, 9)
            )
            CompressionFormat.TAR_ZSTD -> ZstdCodec.outputStream(output)
            else -> error("Not a TAR format")
        }
        TarArchiveOutputStream(BufferedOutputStream(compressed)).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            nodes.forEachIndexed { index, node ->
                val name = if (node.isDirectory) {
                    node.path.trimEnd('/') + "/"
                } else {
                    node.path
                }
                val entry = TarArchiveEntry(name).apply {
                    setMode(if (node.isDirectory) 0x1ED else 0x1A4)
                    setSize(if (node.isDirectory) 0L else node.size)
                    setModTime(Date(node.lastModified.takeIf { it > 0 } ?: System.currentTimeMillis()))
                }
                tar.putArchiveEntry(entry)
                if (!node.isDirectory) {
                    openInput(node.uri, options.onCheckpoint).use {
                        it.copyTo(tar, BUFFER_SIZE)
                    }
                }
                tar.closeArchiveEntry()
                onProgress(index + 1, nodes.size)
            }
        }
    }

    private fun writeCpio(
        output: OutputStream,
        nodes: List<SourceNode>,
        options: CompressionOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        CpioArchiveOutputStream(
            BufferedOutputStream(output),
            CpioConstants.FORMAT_NEW,
            512,
            "UTF-8"
        ).use { cpio ->
            nodes.forEachIndexed { index, node ->
                val name = if (node.isDirectory) {
                    node.path.trimEnd('/') + "/"
                } else {
                    node.path
                }
                val entry = CpioArchiveEntry(name, if (node.isDirectory) 0L else node.size).apply {
                    setMode(if (node.isDirectory) 0x41ED else 0x81A4)
                    setTime(node.lastModified.takeIf { it > 0 } ?: System.currentTimeMillis())
                }
                cpio.putArchiveEntry(entry)
                if (!node.isDirectory) {
                    openInput(node.uri, options.onCheckpoint).use {
                        it.copyTo(cpio, BUFFER_SIZE)
                    }
                }
                cpio.closeArchiveEntry()
                onProgress(index + 1, nodes.size)
            }
        }
    }

    private fun writeSingleStream(
        output: OutputStream,
        node: SourceNode,
        options: CompressionOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        val compressed: OutputStream = when (options.format) {
            CompressionFormat.GZIP -> GzipCompressorOutputStream(
                output,
                GzipParameters().apply {
                    compressionLevel = options.level.zipLevel.coerceIn(1, 9)
                    fileName = node.path.substringAfterLast('/')
                    modificationTime = node.lastModified.takeIf { it > 0 }
                        ?: System.currentTimeMillis()
                }
            )
            CompressionFormat.BZIP2 -> BZip2CompressorOutputStream(
                output,
                options.level.zipLevel.coerceIn(1, 9)
            )
            CompressionFormat.XZ -> XZCompressorOutputStream(
                output,
                options.level.zipLevel.coerceIn(0, 9)
            )
            CompressionFormat.LZMA -> LZMACompressorOutputStream(output)
            CompressionFormat.LZ4 -> FramedLZ4CompressorOutputStream(output)
            CompressionFormat.ZSTD -> ZstdCodec.outputStream(output)
            else -> error("Not a single stream format")
        }
        compressed.use { stream ->
            openInput(node.uri, options.onCheckpoint).use { input ->
                input.copyTo(stream, BUFFER_SIZE)
            }
        }
        onProgress(1, 1)
    }

    private fun writeSevenZ(
        file: File,
        nodes: List<SourceNode>,
        options: CompressionOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        val password = options.password.toCharArray()
        val archive = if (password.isEmpty()) {
            SevenZOutputFile(file)
        } else {
            SevenZOutputFile(file, password)
        }
        archive.use {
            it.setContentCompression(
                if (options.level == ArchiveCompressionLevel.STORE) {
                    SevenZMethod.COPY
                } else {
                    SevenZMethod.LZMA2
                }
            )
            nodes.forEachIndexed { index, node ->
                val entry = SevenZArchiveEntry().apply {
                    name = node.path
                    isDirectory = node.isDirectory
                    setHasStream(!node.isDirectory)
                    size = if (node.isDirectory) 0L else node.size
                    if (node.lastModified > 0) {
                        lastModifiedDate = Date(node.lastModified)
                    }
                }
                it.putArchiveEntry(entry)
                if (!node.isDirectory) {
                    openInput(node.uri, options.onCheckpoint).use { input ->
                        it.write(input)
                    }
                }
                it.closeArchiveEntry()
                onProgress(index + 1, nodes.size)
            }
        }
    }

    private fun copyToOutput(file: File, output: ArchiveOutputTarget) {
        output.open().use { target ->
            FileInputStream(file).use { input ->
                input.copyTo(target, BUFFER_SIZE)
            }
        }
    }

    private fun openInput(uri: Uri, onCheckpoint: () -> Unit = {}): InputStream {
        val direct = uri.localFile()
        if (direct != null) {
            if (!direct.isFile) throw ArchiveException("无法读取 ${direct.name}")
            return CheckpointInputStream(FileInputStream(direct), onCheckpoint)
        }
        val input = resolver.openInputStream(uri)
            ?: throw ArchiveException("无法读取所选文件")
        return CheckpointInputStream(input, onCheckpoint)
    }

    private fun normalizedArchiveName(name: String, format: CompressionFormat): String {
        val cleaned = name.trim()
            .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
            .trim('.', ' ')
            .ifBlank { "压缩包" }
            .take(180)
        return if (cleaned.lowercase().endsWith(format.extension)) {
            cleaned
        } else {
            cleaned + format.extension
        }
    }

    private fun sanitizeArchivePath(raw: String): String =
        raw.replace('\\', '/')
            .split('/')
            .map {
                it.replace(Regex("""[\u0000-\u001F<>:"|?*]"""), "_").trim()
            }
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString("/")

    private data class SourceNode(
        val path: String,
        val uri: Uri,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    private class ArchiveOutputTarget(
        private val resolver: ContentResolver,
        private val parentUri: Uri,
        private val treeUri: Uri,
        requestedName: String,
        private val mimeType: String,
        private val splitSize: Long?
    ) {
        private val created = mutableListOf<Uri>()
        private val existingNames = childNames(parentUri, treeUri)
        val archiveName: String = makeUniqueName(requestedName, existingNames, splitSize != null)
        private var explicitDisplayName: String? = null

        fun open(): OutputStream {
            return if (splitSize == null) {
                val uri = createFile(archiveName, mimeType)
                BufferedOutputStream(openOutput(uri))
            } else {
                SplitOutputStream(
                    splitSize = splitSize,
                    openPart = { index ->
                        val uri = createFile(
                            "$archiveName.${index.toString().padStart(3, '0')}",
                            "application/octet-stream"
                        )
                        BufferedOutputStream(openOutput(uri))
                    }
                )
            }
        }

        fun finish() {
            if (created.isEmpty()) throw ArchiveException("未生成压缩文件")
        }

        fun copyGeneratedFiles(files: List<File>, names: List<String>) {
            if (files.size != names.size) {
                throw ArchiveException("分卷文件名映射失败")
            }
            files.forEachIndexed { index, file ->
                val uri = createFile(names[index], mimeType)
                file.inputStream().use { input ->
                    openOutput(uri).use { target ->
                        input.copyTo(target, BUFFER_SIZE)
                    }
                }
            }
            explicitDisplayName = names.firstOrNull()
        }

        fun result(): CompressionResult {
            val primary = created.firstOrNull()
                ?: throw ArchiveException("未生成压缩文件")
            val displayName = explicitDisplayName ?: if (splitSize == null) {
                archiveName
            } else {
                "$archiveName.001"
            }
            return CompressionResult(
                displayName = displayName,
                primaryUri = primary,
                partUris = created.toList()
            )
        }

        fun abort() {
            created.forEach { uri ->
                runCatching { DocumentsContract.deleteDocument(resolver, uri) }
                uri.localFile()?.delete()
            }
            created.clear()
        }

        private fun createFile(name: String, type: String): Uri {
            parentUri.localFile()?.let { parent ->
                val file = File(parent, name)
                if (file.exists() || file.createNewFile()) {
                    val uri = Uri.fromFile(file)
                    created += uri
                    return uri
                }
                throw ArchiveException("无法创建 $name")
            }
            val uri = DocumentsContract.createDocument(resolver, parentUri, type, name)
                ?: throw ArchiveException("无法创建 $name")
            created += uri
            return uri
        }

        private fun openOutput(uri: Uri): OutputStream =
            uri.localFile()?.let { FileOutputStream(it) }
                ?: resolver.openOutputStream(uri, "w")
                ?: throw ArchiveException("无法写入压缩文件")

        private fun makeUniqueName(
            requested: String,
            existingNames: Set<String>,
            split: Boolean
        ): String {
                fun conflicts(candidate: String): Boolean =
                    if (split) {
                        val zipBase = candidate.removeSuffix(".zip")
                        val zipVolumePattern = Regex(
                            Regex.escape(zipBase) + """\.z\d{2,}$""",
                            RegexOption.IGNORE_CASE
                        )
                        existingNames.any {
                            it == candidate ||
                                (it.startsWith("$candidate.") &&
                                    it.removePrefix("$candidate.")
                                        .matches(Regex("""\d{3,}"""))) ||
                                (candidate.endsWith(".zip", ignoreCase = true) &&
                                    zipVolumePattern.matches(it))
                        }
                    } else {
                        candidate in existingNames
                    }

                if (!conflicts(requested)) return requested
                val dot = requested.indexOf('.')
                val base = if (dot > 0) requested.substring(0, dot) else requested
                val suffix = if (dot > 0) requested.substring(dot) else ""
                var index = 1
                while (index < 10_000) {
                    val candidate = "$base ($index)$suffix"
                    if (!conflicts(candidate)) return candidate
                    index++
                }
                throw ArchiveException("无法生成可用文件名")
            }

        private fun childNames(parentUri: Uri, treeUri: Uri): Set<String> {
            parentUri.localFile()?.let { parent ->
                return parent.listFiles().orEmpty().mapTo(mutableSetOf()) { it.name }
            }
            val parentId = DocumentsContract.getDocumentId(parentUri)
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val names = mutableSetOf<String>()
            resolver.query(
                childrenUri,
                arrayOf(Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        cursor.getString(0)?.let(names::add)
                    } while (cursor.moveToNext())
                }
            }
            return names
        }
    }

    private class SplitOutputStream(
        private val splitSize: Long,
        private val openPart: (Int) -> OutputStream
    ) : OutputStream() {
        private var partIndex = 0
        private var current: OutputStream? = null
        private var currentBytes = 0L

        override fun write(byte: Int) {
            write(byteArrayOf(byte.toByte()), 0, 1)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            var sourceOffset = offset
            var remaining = length
            while (remaining > 0) {
                if (current == null || currentBytes >= splitSize) {
                    closeCurrent()
                    current = openPart(++partIndex)
                }
                val capacity = splitSize - currentBytes
                val count = minOf(remaining.toLong(), capacity).toInt()
                current!!.write(bytes, sourceOffset, count)
                sourceOffset += count
                remaining -= count
                currentBytes += count
            }
        }

        override fun flush() {
            current?.flush()
        }

        override fun close() {
            closeCurrent()
        }

        private fun closeCurrent() {
            current?.close()
            current = null
            currentBytes = 0L
        }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_COMPRESSION_ENTRIES = 50_000
    }
}
