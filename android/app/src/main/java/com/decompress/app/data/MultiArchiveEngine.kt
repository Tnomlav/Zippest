package com.decompress.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.github.junrar.Archive as RarArchive
import com.github.junrar.rarfile.FileHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.brotli.dec.BrotliInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

class MultiArchiveEngine(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val zipEngine = ZipArchiveEngine(context)
    private val repository = SafFileRepository(context)

    fun supports(name: String): Boolean {
        return isSupportedArchiveName(name)
    }

    suspend fun list(
        archiveUri: Uri,
        archiveName: String,
        password: String = "",
        archiveTreeUri: Uri? = null,
        archiveParentUri: Uri? = null,
        encoding: FileNameEncoding = FileNameEncoding.AUTO,
        onBatch: ((List<ArchiveItem>, Boolean) -> Unit)? = null
    ): List<ArchiveItem> = withResolvedArchive(
        archiveUri,
        archiveName,
        archiveTreeUri,
        archiveParentUri
    ) { uri, name ->
        val lower = name.lowercase()
        when {
            lower.endsWith(".zip") || lower.endsWith(".jar") || lower.endsWith(".apk") ->
                zipEngine.list(uri, encoding, onBatch)
            lower.endsWith(".iso") -> listNative(uri, password).also {
                onBatch?.invoke(it, true)
            }
            lower.endsWith(".7z") -> listSevenZ(uri, password).also {
                onBatch?.invoke(it, true)
            }
            lower.endsWith(".rar") -> listRar(uri, password).also {
                onBatch?.invoke(it, true)
            }
            else -> listStream(uri, name.lowercase(), name, encoding, onBatch)
        }
    }

    suspend fun extract(
        archiveUri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit = {},
        onEntry: (String) -> Unit = {},
        archiveTreeUri: Uri? = null,
        archiveParentUri: Uri? = null
    ) = withResolvedArchive(
        archiveUri,
        archiveName,
        archiveTreeUri,
        archiveParentUri,
        onPreparationProgress = { fraction ->
            onFractionProgress(fraction.coerceIn(0f, 1f) * PREPARATION_SHARE)
        }
    ) { uri, name ->
        val reportExtraction: (Float) -> Unit = { fraction ->
            onFractionProgress(
                PREPARATION_SHARE +
                    fraction.coerceIn(0f, 1f) * (1f - PREPARATION_SHARE)
            )
        }
        val lower = name.lowercase()
        when {
            lower.endsWith(".zip") || lower.endsWith(".jar") || lower.endsWith(".apk") ->
                zipEngine.extract(
                    uri,
                    name,
                    settings,
                    onProgress,
                    reportExtraction,
                    onEntry
                )
            lower.endsWith(".iso") ->
                extractIso(uri, name, settings, onProgress, reportExtraction)
            lower.endsWith(".7z") ->
                extractSevenZ(uri, name, settings, onProgress, reportExtraction)
            lower.endsWith(".rar") ->
                extractRar(uri, name, settings, onProgress, reportExtraction)
            lower.endsWith(".tar") ||
                lower.endsWith(".tar.gz") ||
                lower.endsWith(".tgz") ||
                lower.endsWith(".tar.bz2") ||
                lower.endsWith(".tbz2") ||
                lower.endsWith(".tar.xz") ||
                lower.endsWith(".txz") ||
                lower.endsWith(".tar.zst") ||
                lower.endsWith(".tar.br") ||
                lower.endsWith(".cpio") ->
                extractStream(
                    uri,
                    lower,
                    name,
                    settings,
                    onProgress,
                    reportExtraction,
                    onEntry
                )
            lower.endsWith(".gz") ||
                lower.endsWith(".bz2") ||
                lower.endsWith(".xz") ||
                lower.endsWith(".lzma") ||
                lower.endsWith(".lz4") ||
                lower.endsWith(".zst") ||
                lower.endsWith(".br") ->
                extractSingleCompressed(
                    uri,
                    lower,
                    name,
                    settings,
                    reportExtraction
                )
            else -> throw ArchiveException("暂不支持此压缩格式")
        }
    }

    suspend fun test(
        archiveUri: Uri,
        archiveName: String,
        password: String,
        onProgress: (Int, Int) -> Unit,
        archiveTreeUri: Uri? = null,
        archiveParentUri: Uri? = null
    ): Int = withResolvedArchive(
        archiveUri,
        archiveName,
        archiveTreeUri,
        archiveParentUri
    ) { uri, name ->
        val lower = name.lowercase()
        when {
            lower.endsWith(".zip") || lower.endsWith(".jar") ->
                zipEngine.test(uri, password, onProgress)
            lower.endsWith(".7z") ->
                testSevenZ(uri, password, onProgress)
            lower.endsWith(".rar") ->
                testRar(uri, password, onProgress)
            lower.endsWith(".tar") ||
                lower.endsWith(".tar.gz") ||
                lower.endsWith(".tgz") ||
                lower.endsWith(".tar.bz2") ||
                lower.endsWith(".tbz2") ||
                lower.endsWith(".tar.xz") ||
                lower.endsWith(".txz") ||
                lower.endsWith(".tar.zst") ||
                lower.endsWith(".tar.br") ||
                lower.endsWith(".cpio") ->
                testStream(uri, lower, onProgress)
            lower.endsWith(".gz") ||
                lower.endsWith(".bz2") ||
                lower.endsWith(".xz") ||
                lower.endsWith(".lzma") ||
                lower.endsWith(".lz4") ||
                lower.endsWith(".zst") ||
                lower.endsWith(".br") -> {
                testSingleCompressed(uri, lower)
                onProgress(1, 1)
                1
            }
            else -> throw ArchiveException("暂不支持此压缩格式")
        }
    }

    private suspend fun <T> withResolvedArchive(
        archiveUri: Uri,
        archiveName: String,
        archiveTreeUri: Uri?,
        archiveParentUri: Uri?,
        onPreparationProgress: (Float) -> Unit = {},
        block: suspend (Uri, String) -> T
    ): T {
        val nativeZip = resolveNativeSplitZip(
            archiveUri,
            archiveName,
            archiveTreeUri,
            archiveParentUri
        )
        if (nativeZip != null) {
            val directory = Files.createTempDirectory(
                context.cacheDir.toPath(),
                "zippest-zip-volumes-"
            ).toFile()
            try {
                val totalBytes = nativeZip.sumOf { (_, partUri) -> fileSize(partUri) }
                var copiedBytes = 0L
                withContext(Dispatchers.IO) {
                    nativeZip.forEach { (name, partUri) ->
                        resolver.openInputStream(partUri)?.use { input ->
                            File(directory, name).outputStream().use { output ->
                                copiedBytes += copyWithLimit(
                                    input = input,
                                    output = output,
                                    remainingBytes = Long.MAX_VALUE,
                                    onProgress = { partBytes ->
                                        reportFraction(
                                            onPreparationProgress,
                                            copiedBytes + partBytes,
                                            totalBytes
                                        )
                                    }
                                )
                            }
                        } ?: throw ArchiveException("无法读取分卷：$name")
                    }
                }
                onPreparationProgress(1f)
                val finalName = canonicalArchiveName(archiveName)
                return block(Uri.fromFile(File(directory, finalName)), finalName)
            } finally {
                directory.deleteRecursively()
            }
        }
        val rarVolumes = resolveRarVolumes(
            archiveUri = archiveUri,
            archiveName = archiveName,
            treeUri = archiveTreeUri,
            parentUri = archiveParentUri
        )
        if (rarVolumes != null) {
            val localParts = rarVolumes.mapNotNull { repository.localFileForUri(it.uri) }
            if (localParts.size == rarVolumes.size) {
                onPreparationProgress(1f)
                return block(
                    Uri.fromFile(localParts.first()),
                    rarVolumes.first().name
                )
            }
            val directory = Files.createTempDirectory(
                context.cacheDir.toPath(),
                "zippest-rar-volumes-"
            ).toFile()
            try {
                val totalBytes = rarVolumes.sumOf { part ->
                    fileSize(part.uri).takeIf { it > 0L }
                        ?: part.size.coerceAtLeast(0L)
                }
                var copiedBytes = 0L
                withContext(Dispatchers.IO) {
                    rarVolumes.forEach { part ->
                        resolver.openInputStream(part.uri)?.use { input ->
                            File(directory, part.name).outputStream().use { output ->
                                copiedBytes += copyWithLimit(
                                    input = input,
                                    output = output,
                                    remainingBytes = Long.MAX_VALUE,
                                    onProgress = { partBytes ->
                                        reportFraction(
                                            onPreparationProgress,
                                            copiedBytes + partBytes,
                                            totalBytes
                                        )
                                    }
                                )
                            }
                        } ?: throw ArchiveException("无法读取分卷：${part.name}")
                    }
                }
                onPreparationProgress(1f)
                return block(
                    Uri.fromFile(File(directory, rarVolumes.first().name)),
                    rarVolumes.first().name
                )
            } finally {
                directory.deleteRecursively()
            }
        }
        if (!isFirstSplitVolume(archiveName)) {
            onPreparationProgress(1f)
            return block(archiveUri, archiveName)
        }
        val parts = findSplitParts(
            archiveUri,
            archiveName,
            archiveTreeUri,
            archiveParentUri
        )
        if (parts.isEmpty()) throw ArchiveException("找不到完整的分卷文件")
        val totalBytes = parts.sumOf { part ->
            fileSize(part.uri).takeIf { it > 0L }
                ?: part.size.coerceAtLeast(0L)
        }
        val usableBytes = runCatching {
            android.os.StatFs(context.cacheDir.absolutePath).availableBytes
        }.getOrDefault(Long.MAX_VALUE)
        val safetyMargin = 32L * 1024 * 1024
        if (totalBytes + safetyMargin > usableBytes) {
            throw ArchiveException(
                "合并分卷需要约 ${formatBytes(totalBytes)} 临时空间，当前可用 " +
                    "${formatBytes(usableBytes)}。请先清理缓存，或把分卷换到空间更大的位置再解压。"
            )
        }
        val temp = File.createTempFile("zippest-parts-", ".tmp", context.cacheDir)
        try {
            var copiedBytes = 0L
            withContext(Dispatchers.IO) {
                temp.outputStream().buffered().use { output ->
                    parts.forEach { part ->
                        resolver.openInputStream(part.uri)?.use { input ->
                            copiedBytes += copyWithLimit(
                                input = input,
                                output = output,
                                remainingBytes = Long.MAX_VALUE,
                                onProgress = { partBytes ->
                                    reportFraction(
                                        onPreparationProgress,
                                        copiedBytes + partBytes,
                                        totalBytes
                                    )
                                }
                            )
                        } ?: throw ArchiveException("无法读取分卷：${part.name}")
                    }
                }
            }
            onPreparationProgress(1f)
            return block(Uri.fromFile(temp), canonicalArchiveName(archiveName))
        } finally {
            temp.delete()
        }
    }

    private suspend fun resolveNativeSplitZip(
        archiveUri: Uri,
        archiveName: String,
        treeUri: Uri?,
        parentUri: Uri?
    ): List<Pair<String, Uri>>? {
        val baseName = nativeZipBaseName(archiveName)
            ?: if (archiveName.endsWith(".zip", ignoreCase = true)) {
                archiveName.dropLast(4)
            } else {
                null
            }
            ?: return null
        val volumePattern = Regex(
            Regex.escape(baseName) + """\.z(\d{2,})$""",
            RegexOption.IGNORE_CASE
        )
        val finalName = baseName + ".zip"
        val children = siblingEntries(archiveUri, treeUri, parentUri)
        val volumes = children.filter { volumePattern.matches(it.name) }
            .sortedBy { entry ->
                volumePattern.matchEntire(entry.name)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
            }
        if (volumes.isEmpty()) return null
        val numbers = volumes.mapNotNull { entry ->
            volumePattern.matchEntire(entry.name)
                ?.groupValues?.get(1)?.toIntOrNull()
        }
        if (numbers.firstOrNull() != 1 || numbers != (1..numbers.size).toList()) {
            throw ArchiveException("ZIP 分卷编号不连续，请确保从 .z01 开始")
        }
        val finalPart = children.firstOrNull {
            it.name.equals(finalName, ignoreCase = true)
        } ?: throw ArchiveException("缺少 ZIP 分卷末端文件：$finalName")
        return volumes.map { it.name to it.uri } + (finalPart.name to finalPart.uri)
    }

    private suspend fun resolveRarVolumes(
        archiveUri: Uri,
        archiveName: String,
        treeUri: Uri?,
        parentUri: Uri?
    ): List<BrowserEntry>? {
        val children = siblingEntries(archiveUri, treeUri, parentUri)
        val selectedEntry = children.firstOrNull {
            it.name.equals(archiveName, ignoreCase = true)
        } ?: repository.localFileForUri(archiveUri)?.let { local ->
            repository.directEntry(local)
        } ?: BrowserEntry(
            uri = archiveUri,
            name = archiveName,
            isDirectory = false,
            size = 0L,
            lastModified = 0L,
            mimeType = "application/x-rar-compressed"
        )
        val partVolume = parseRarPartVolume(archiveName)
        if (partVolume != null) {
            val firstNumber = if (partVolume.zeroBased) 0 else 1
            if (partVolume.number != firstNumber) return null
            val parts = children.mapNotNull { entry ->
                parseRarPartVolume(entry.name)
                    ?.takeIf { it.baseName.equals(partVolume.baseName, ignoreCase = true) }
                    ?.let { it.number to entry }
            }.toMap().toMutableMap()
            parts[partVolume.number] = selectedEntry
            val numbers = parts.keys.sorted()
            if (numbers != (firstNumber until firstNumber + numbers.size).toList()) {
                throw ArchiveException("RAR 分卷编号不连续，请从第一个分卷开始")
            }
            return numbers.mapNotNull(parts::get)
        }

        if (!archiveName.endsWith(".rar", ignoreCase = true)) return null
        val baseName = archiveName.dropLast(4)
        val legacyParts = children.mapNotNull { entry ->
            parseLegacyRarVolume(entry.name)
                ?.takeIf { it.baseName.equals(baseName, ignoreCase = true) }
                ?.let { it.number to entry }
        }.toMap().toMutableMap()
        if (legacyParts.isEmpty()) return null
        val numbers = legacyParts.keys.sorted()
        if (numbers != (0 until numbers.size).toList()) {
            throw ArchiveException("RAR 分卷编号不连续，请确保从 .r00 开始")
        }
        return listOf(selectedEntry) + numbers.mapNotNull(legacyParts::get)
    }

    private suspend fun findSplitParts(
        archiveUri: Uri,
        firstName: String,
        treeUri: Uri?,
        parentUri: Uri?
    ): List<BrowserEntry> {
        val baseName = canonicalArchiveName(firstName)
        val pattern = Regex(
            Regex.escape(baseName) + """\.(\d{3,})$""",
            RegexOption.IGNORE_CASE
        )
        val candidates = siblingEntries(
            archiveUri,
            treeUri,
            parentUri
        ).filter { pattern.matches(it.name) }
        val sorted = candidates.sortedBy { entry ->
            pattern.matchEntire(entry.name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
        }
        val numbers = sorted.mapNotNull { entry ->
            pattern.matchEntire(entry.name)?.groupValues?.get(1)?.toIntOrNull()
        }
        if (numbers.isEmpty()) {
            throw ArchiveException("找不到分卷文件。请从 .001 开始，或确认分卷在同一目录。")
        }
        if (numbers.firstOrNull() != 1) {
            throw ArchiveException("缺少第一卷（.001）。请从 .001 开始解压，或把分卷补齐。")
        }
        val highest = numbers.maxOrNull() ?: 0
        val missing = (1..highest).filter { it !in numbers }
        if (missing.isNotEmpty()) {
            val shown = missing.take(6).joinToString("、") { "%03d".format(it) }
            val more = if (missing.size > 6) " 等 ${missing.size} 卷" else ""
            throw ArchiveException(
                "缺少第 $shown 卷$more。请把同一目录下的分卷补齐后再试。"
            )
        }
        return sorted
    }

    private suspend fun siblingEntries(
        archiveUri: Uri,
        treeUri: Uri?,
        parentUri: Uri?
    ): List<BrowserEntry> {
        parentUri?.localFile()?.let { parent ->
            return parent.listFiles().orEmpty().map { repository.directEntry(it) }
        }
        if (treeUri != null && parentUri != null) {
            return repository.listChildren(treeUri, parentUri)
        }
        return repository.localFileForUri(archiveUri)
            ?.parentFile
            ?.listFiles()
            .orEmpty()
            .map { repository.directEntry(it) }
    }

    private suspend fun extractIso(
        uri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit
    ) {
        val handled = withArchiveFile(uri) { file ->
            val entries = runCatching {
                scanNative(file, archiveName, "", "UTF-8")
            }.getOrNull() ?: return@withArchiveFile false
            extractNative(
                file = file,
                archiveName = archiveName,
                settings = settings,
                entries = entries,
                onProgress = onProgress,
                onFractionProgress = onFractionProgress
            )
            true
        }
        if (!handled) throw ArchiveException("无法读取 ISO 镜像")
    }
    private suspend fun listNative(uri: Uri, password: String): List<ArchiveItem> =
        withArchiveFile(uri) { file ->
            val entries = scanNative(file, file.name, password, "UTF-8")
            entries.map { entry ->
                ArchiveItem(
                    path = entry.path,
                    isDirectory = entry.isDirectory,
                    size = entry.size
                )
            }
        }

    private fun scanNative(
        file: File,
        archiveName: String,
        password: String,
        charsetName: String
    ): List<NativeArchiveEntry> {
        val files = nativeArchiveFiles(file, archiveName)
        val entries = mutableListOf<NativeArchiveEntry>()
        NativeArchiveReader.open(files, password, charsetName).use { reader ->
            while (true) {
                val entry = reader.nextEntry() ?: break
                if (entries.size >= MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止读取")
                }
                entries += entry
            }
        }
        return entries
    }

    private fun extractNative(
        file: File,
        archiveName: String,
        settings: ExtractSettings,
        entries: List<NativeArchiveEntry>,
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit
    ) {
        val files = nativeArchiveFiles(file, archiveName)
        val packedSize = files.sumOf { it.length() }.coerceAtLeast(files.first().length())
        val declaredBytes = entries.sumOf { it.size }
        ensureSafeExpansion(declaredBytes, packedSize)
        ensureLocalFreeSpace(context, settings.targetTreeUri, declaredBytes)

        val output = outputWriter(settings)
        try {
        val destination = prepareDestination(output, settings, archiveName)
        var processed = 0
        var writtenBytes = 0L
        onFractionProgress(0f)
        NativeArchiveReader.open(
            files,
            settings.password,
            settings.encoding.charsetName
        ).use { reader ->
            while (true) {
                val entry = reader.nextEntry() ?: break
                if (processed >= MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止解压")
                }
                writtenBytes += writeEntry(
                    output = output,
                    destination = destination,
                    path = entry.path,
                    isDirectory = entry.isDirectory,
                    settings = settings,
                    inputFactory = reader::openEntryInputStream,
                    closeInput = true,
                    remainingBytes = MAX_TOTAL_BYTES - writtenBytes,
                    onProgress = { entryBytes ->
                        reportFraction(
                            onFractionProgress,
                            writtenBytes + entryBytes,
                            declaredBytes
                        )
                    }
                )
                ensureSafeExpansion(writtenBytes, packedSize)
                processed++
                onProgress(processed, entries.size.coerceAtLeast(processed))
            }
        }
        onFractionProgress(1f)
        } catch (t: Throwable) {
            output.rollback()
            throw t
        }
    }

    private fun nativeArchiveFiles(file: File, archiveName: String): List<File> =
        if (archiveName.endsWith(".rar", ignoreCase = true)) {
            rarVolumeFiles(file)
        } else {
            listOf(file)
        }

    private suspend fun testNative(
        uri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withArchiveFile(uri) { file ->
        val entries = scanNative(file, file.name, password, "UTF-8")
        if (entries.size > MAX_ENTRIES) {
            throw ArchiveException("压缩包条目过多，已停止测试")
        }
        val files = nativeArchiveFiles(file, file.name)
        val packedSize = files.sumOf { it.length() }.coerceAtLeast(files.first().length())
        val declaredBytes = entries.sumOf { it.size }
        ensureSafeExpansion(declaredBytes, packedSize)
        var processed = 0
        var writtenBytes = 0L
        NativeArchiveReader.open(files, password, "UTF-8").use { reader ->
            while (true) {
                val entry = reader.nextEntry() ?: break
                if (processed >= MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止测试")
                }
                if (!entry.isDirectory) {
                    reader.openEntryInputStream().use { input ->
                        writtenBytes += copyWithLimit(
                            input = input,
                            output = DiscardOutputStream,
                            remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                        )
                    }
                }
                ensureSafeExpansion(writtenBytes, packedSize)
                processed++
                onProgress(processed, entries.size.coerceAtLeast(processed))
            }
        }
        processed
    }

    private suspend fun testSevenZ(
        uri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int {
        if (password.isEmpty()) {
            val nativeResult = runCatching {
                testNative(uri, password, onProgress)
            }.getOrNull()
            if (nativeResult != null) return nativeResult
        }
        return testSevenZJava(uri, password, onProgress)
    }

    private suspend fun testSevenZJava(
        uri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withArchiveFile(uri) { file ->
        runCatching {
            val packedSize = packedSizeForVolumes(file)
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
                ensureSafeExpansion(declaredBytes, packedSize)
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
    ): Int {
        if (password.isEmpty()) {
            val nativeResult = runCatching {
                testNative(uri, password, onProgress)
            }.getOrNull()
            if (nativeResult != null) return nativeResult
        }
        return testRarJava(uri, password, onProgress)
    }

    private suspend fun testRarJava(
        uri: Uri,
        password: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withArchiveFile(uri) { file ->
        runCatching {
            val headers = rarHeaders(file, password)
            if (headers.size > MAX_ENTRIES) {
                throw ArchiveException("压缩包条目过多，已停止测试")
            }
            val declaredBytes = headers.sumOf {
                it.fullUnpackSize.coerceAtLeast(0L)
            }
            val packedSize = packedSizeForVolumes(file)
            ensureSafeExpansion(declaredBytes, packedSize)
            val archive = if (password.isBlank()) {
                RarArchive(file)
            } else {
                RarArchive(file, password)
            }
            archive.use { rar ->
                var writtenBytes = 0L
                var processed = 0
                var header = rar.nextFileHeader()
                while (header != null) {
                    if (processed >= MAX_ENTRIES) {
                        throw ArchiveException("压缩包条目过多，已停止测试")
                    }
                    if (!header.isDirectory) {
                        rar.getInputStream(header).use { input ->
                            writtenBytes += copyWithLimit(
                                input = input,
                                output = DiscardOutputStream,
                                remainingBytes = MAX_TOTAL_BYTES - writtenBytes
                            )
                        }
                    }
                    ensureSafeExpansion(writtenBytes, packedSize)
                    processed++
                    onProgress(processed, headers.size.coerceAtLeast(processed))
                    header = rar.nextFileHeader()
                }
                processed
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
            lowerName.endsWith(".xz") ->
                XZCompressorInputStream(BufferedInputStream(input))
            lowerName.endsWith(".lzma") ->
                LZMACompressorInputStream(BufferedInputStream(input))
            lowerName.endsWith(".lz4") ->
                FramedLZ4CompressorInputStream(BufferedInputStream(input))
            lowerName.endsWith(".zst") ->
                ZstdCodec.inputStream(BufferedInputStream(input))
            else -> BrotliInputStream(BufferedInputStream(input))
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
        if (password.isNotEmpty()) {
            listSevenZJava(uri, password)
        } else {
            runCatching {
                listNative(uri, password)
            }.getOrElse {
                listSevenZJava(uri, password)
            }
        }

    private suspend fun listSevenZJava(uri: Uri, password: String): List<ArchiveItem> =
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
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit
    ) {
        if (settings.password.isNotEmpty()) {
            extractSevenZJava(
                uri,
                archiveName,
                settings,
                onProgress,
                onFractionProgress
            )
            return
        }
        val handled = withArchiveFile(uri) { file ->
            val entries = runCatching {
                scanNative(file, archiveName, settings.password, "UTF-8")
            }.getOrNull() ?: return@withArchiveFile false
            extractNative(
                file = file,
                archiveName = archiveName,
                settings = settings,
                entries = entries,
                onProgress = onProgress,
                onFractionProgress = onFractionProgress
            )
            true
        }
        if (!handled) {
            extractSevenZJava(
                uri,
                archiveName,
                settings,
                onProgress,
                onFractionProgress
            )
        }
    }

    private suspend fun extractSevenZJava(
        uri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit
    ) = withArchiveFile(uri) { file ->
        var rollbackOutput: ArchiveOutput? = null
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
                onFractionProgress(0f)
                val output = outputWriter(settings)
                rollbackOutput = output
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
                        remainingBytes = MAX_TOTAL_BYTES - writtenBytes,
                        onProgress = { fileBytes ->
                            reportFraction(
                                onFractionProgress,
                                writtenBytes + fileBytes,
                                declaredBytes
                            )
                        }
                    )
                    ensureSafeExpansion(writtenBytes, file.length())
                    onProgress(index + 1, entries.size)
                }
                onFractionProgress(1f)
            }
        }.getOrElse {
            rollbackOutput?.rollback()
            throw ArchiveException("7Z 解压失败：${it.archiveMessage()}", it)
        }
    }

    private suspend fun listRar(uri: Uri, password: String): List<ArchiveItem> =
        if (password.isNotEmpty()) {
            listRarJava(uri, password)
        } else {
            runCatching {
                listNative(uri, password)
            }.getOrElse {
                listRarJava(uri, password)
            }
        }

    private suspend fun listRarJava(uri: Uri, password: String): List<ArchiveItem> =
        withArchiveFile(uri) { file ->
            runCatching {
                rarHeaders(file, password).take(MAX_ENTRIES).map { header ->
                    ArchiveItem(
                        path = header.fileName,
                        isDirectory = header.isDirectory,
                        size = header.fullUnpackSize.coerceAtLeast(0L)
                    )
                }
            }.getOrElse { throw ArchiveException("无法读取 RAR：${it.archiveMessage()}", it) }
        }

    private suspend fun extractRar(
        uri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit
    ) {
        if (settings.password.isNotEmpty()) {
            extractRarJava(
                uri,
                archiveName,
                settings,
                onProgress,
                onFractionProgress
            )
            return
        }
        val handled = withArchiveFile(uri) { file ->
            val entries = runCatching {
                scanNative(file, archiveName, settings.password, "UTF-8")
            }.getOrNull() ?: return@withArchiveFile false
            extractNative(
                file = file,
                archiveName = archiveName,
                settings = settings,
                entries = entries,
                onProgress = onProgress,
                onFractionProgress = onFractionProgress
            )
            true
        }
        if (!handled) {
            extractRarJava(
                uri,
                archiveName,
                settings,
                onProgress,
                onFractionProgress
            )
        }
    }

    private suspend fun extractRarJava(
        uri: Uri,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit
    ) = withArchiveFile(uri) { file ->
        var rollbackOutput: ArchiveOutput? = null
        runCatching {
            val headers = rarHeaders(file, settings.password)
            if (headers.size > MAX_ENTRIES) {
                throw ArchiveException("压缩包条目过多，已停止解压")
            }
            val declaredBytes = headers.sumOf {
                it.fullUnpackSize.coerceAtLeast(0L)
            }
            val packedSize = packedSizeForVolumes(file)
            ensureSafeExpansion(declaredBytes, packedSize)
            ensureLocalFreeSpace(context, settings.targetTreeUri, declaredBytes)
            onFractionProgress(0f)
            val archive = if (settings.password.isBlank()) {
                RarArchive(file)
            } else {
                RarArchive(file, settings.password)
            }
            archive.use { rar ->
                val output = outputWriter(settings)
                rollbackOutput = output
                val destination = prepareDestination(output, settings, archiveName)
                var writtenBytes = 0L
                var processed = 0
                var header = rar.nextFileHeader()
                while (header != null) {
                    if (processed >= MAX_ENTRIES) {
                        throw ArchiveException("压缩包条目过多，已停止解压")
                    }
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
                        remainingBytes = MAX_TOTAL_BYTES - writtenBytes,
                        onProgress = { fileBytes ->
                            reportFraction(
                                onFractionProgress,
                                writtenBytes + fileBytes,
                                declaredBytes
                            )
                        }
                    )
                    ensureSafeExpansion(writtenBytes, packedSize)
                    processed++
                    onProgress(processed, headers.size.coerceAtLeast(processed))
                    header = rar.nextFileHeader()
                }
                onFractionProgress(1f)
            }
        }.getOrElse {
            rollbackOutput?.rollback()
            throw ArchiveException("RAR 解压失败：${it.archiveMessage()}", it)
        }
    }

    private suspend fun listStream(
        uri: Uri,
        lowerName: String,
        archiveName: String,
        encoding: FileNameEncoding = FileNameEncoding.AUTO,
        onBatch: ((List<ArchiveItem>, Boolean) -> Unit)? = null
    ): List<ArchiveItem> {
        if (lowerName.endsWith(".gz") && !lowerName.endsWith(".tar.gz") ||
            lowerName.endsWith(".bz2") && !lowerName.endsWith(".tar.bz2") ||
            lowerName.endsWith(".xz") && !lowerName.endsWith(".tar.xz") ||
            lowerName.endsWith(".lzma") ||
            lowerName.endsWith(".lz4") ||
            lowerName.endsWith(".zst") && !lowerName.endsWith(".tar.zst") ||
            lowerName.endsWith(".br") && !lowerName.endsWith(".tar.br")
        ) {
            val single = listOf(
                ArchiveItem(
                    path = singleOutputName(archiveName, lowerName),
                    isDirectory = false,
                    size = 0L
                )
            )
            onBatch?.invoke(single, true)
            return single
        }

        return withContext(Dispatchers.IO) {
            openStreamArchive(uri, lowerName, encoding = encoding).use { archive ->
                val result = mutableListOf<ArchiveItem>()
                var emitted = 0
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
                    if (onBatch != null && result.size - emitted >= LIST_BATCH_SIZE) {
                        onBatch(result.subList(emitted, result.size).toList(), false)
                        emitted = result.size
                    }
                    entry = archive.nextEntry
                }
                onBatch?.invoke(result.subList(emitted, result.size).toList(), true)
                result
            }
        }
    }

    private suspend fun extractStream(
        uri: Uri,
        lowerName: String,
        archiveName: String,
        settings: ExtractSettings,
        onProgress: (Int, Int) -> Unit,
        onFractionProgress: (Float) -> Unit,
        onEntry: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        onFractionProgress(0f)
        openStreamArchive(uri, lowerName, onFractionProgress, settings.encoding).use { archive ->
            val output = outputWriter(settings)
            try {
            val destination = prepareDestination(output, settings, archiveName)
            var entry: ArchiveEntry? = archive.nextEntry
            var processed = 0
            var writtenBytes = 0L
            val packedSize = fileSize(uri)
            while (entry != null) {
                if (processed >= MAX_ENTRIES) {
                    throw ArchiveException("压缩包条目过多，已停止解压")
                }
                onEntry(entry.name)
                if (entry is org.apache.commons.compress.archivers.tar.TarArchiveEntry &&
                    (entry.isSymbolicLink || entry.isLink)
                ) {
                    entry = archive.nextEntry
                    continue
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
            onFractionProgress(1f)
            } catch (t: Throwable) {
                output.rollback()
                throw t
            }
        }
    }

    private suspend fun extractSingleCompressed(
        uri: Uri,
        lowerName: String,
        archiveName: String,
        settings: ExtractSettings,
        onFractionProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(uri)
            ?: throw ArchiveException("无法读取压缩包")
        onFractionProgress(0f)
        val progressInput = ProgressInputStream(
            input,
            fileSize(uri),
            onFractionProgress
        )
        val compressed: InputStream = when {
            lowerName.endsWith(".gz") ->
                GzipCompressorInputStream(BufferedInputStream(progressInput))
            lowerName.endsWith(".bz2") ->
                BZip2CompressorInputStream(BufferedInputStream(progressInput))
            lowerName.endsWith(".xz") ->
                XZCompressorInputStream(BufferedInputStream(progressInput))
            lowerName.endsWith(".lzma") ->
                LZMACompressorInputStream(BufferedInputStream(progressInput))
            lowerName.endsWith(".lz4") ->
                FramedLZ4CompressorInputStream(BufferedInputStream(progressInput))
            lowerName.endsWith(".zst") ->
                ZstdCodec.inputStream(BufferedInputStream(progressInput))
            else -> BrotliInputStream(BufferedInputStream(progressInput))
        }
        compressed.use { stream ->
            val output = outputWriter(settings)
            try {
            val destination = prepareDestination(output, settings, archiveName)
            val name = sanitizePath(singleOutputName(archiveName, lowerName)).lastOrNull()
                ?: throw ArchiveException("无法确定输出文件名")
            val fileUri = output.createFile(destination, name, settings.conflictPolicy)
                ?: return@use
            output.openOutputStream(fileUri)?.use { out ->
                val written = copyWithLimit(
                    input = CheckpointInputStream(stream, settings.onCheckpoint),
                    output = out,
                    remainingBytes = MAX_TOTAL_BYTES
                )
                ensureSafeExpansion(written, fileSize(uri))
            } ?: throw ArchiveException("无法写入解压文件")
            settings.onFileCreated(fileUri)
            onFractionProgress(1f)
            } catch (t: Throwable) {
                output.rollback()
                throw t
            }
        }
    }

    private fun openStreamArchive(
        uri: Uri,
        lowerName: String,
        onFractionProgress: (Float) -> Unit = {},
        encoding: FileNameEncoding = FileNameEncoding.AUTO
    ): ArchiveInputStream<*> {
        val input = resolver.openInputStream(uri)
            ?: throw ArchiveException("无法读取压缩包")
        val buffered = BufferedInputStream(
            ProgressInputStream(input, fileSize(uri), onFractionProgress)
        )
        val tarEncoding = if (encoding == FileNameEncoding.AUTO) {
            "UTF-8"
        } else {
            encoding.charsetName
        }
        return when {
            lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz") ->
                TarArchiveInputStream(GzipCompressorInputStream(buffered, true), tarEncoding)
            lowerName.endsWith(".tar.bz2") || lowerName.endsWith(".tbz2") ->
                TarArchiveInputStream(BZip2CompressorInputStream(buffered, true), tarEncoding)
            lowerName.endsWith(".tar.xz") || lowerName.endsWith(".txz") ->
                TarArchiveInputStream(XZCompressorInputStream(buffered, true), tarEncoding)
            lowerName.endsWith(".tar.zst") ->
                TarArchiveInputStream(ZstdCodec.inputStream(buffered), tarEncoding)
            lowerName.endsWith(".tar.br") ->
                TarArchiveInputStream(BrotliInputStream(buffered), tarEncoding)
            lowerName.endsWith(".tar") -> TarArchiveInputStream(buffered, tarEncoding)
            lowerName.endsWith(".cpio") -> CpioArchiveInputStream(buffered, tarEncoding)
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
        remainingBytes: Long,
        onProgress: (Long) -> Unit = {}
    ): Long {
        if (!settings.matches(path)) return 0L
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
        val rawInput = inputFactory()
        val input = CheckpointInputStream(rawInput, settings.onCheckpoint)
        val written = try {
            output.openOutputStream(fileUri)?.use { out ->
                copyWithLimit(
                    input = input,
                    output = out,
                    remainingBytes = remainingBytes,
                    onProgress = onProgress
                )
            } ?: throw ArchiveException("无法写入文件：${segments.last()}")
        } finally {
            if (closeInput) rawInput.close()
        }
        settings.onFileCreated(fileUri)
        return written
    }

    private fun fileSize(uri: Uri): Long =
        uri.localFile()?.takeIf { it.isFile }?.length()
            ?: resolver.openFileDescriptor(uri, "r")?.use { it.statSize }.orZero()

    private fun Long?.orZero(): Long = this?.coerceAtLeast(0L) ?: 0L

    private fun outputWriter(settings: ExtractSettings): ArchiveOutput =
        ArchiveOutput(resolver, settings.targetTreeUri)

    private fun prepareDestination(
        output: ArchiveOutput,
        settings: ExtractSettings,
        archiveName: String
    ): Uri {
        if (!settings.createFolder) {
            settings.onDestinationCreated(settings.targetParentUri, archiveName)
            return settings.targetParentUri
        }
        val baseName = singleOutputName(archiveName, archiveName.lowercase())
        val destination = output.ensureDirectory(settings.targetParentUri, baseName)
            ?: throw ArchiveException("无法创建目标文件夹")
        settings.onDestinationCreated(destination, baseName)
        return destination
    }

    private fun singleOutputName(archiveName: String, lowerName: String): String {
        return when {
            lowerName.endsWith(".tar.gz") -> archiveName.dropLast(7)
            lowerName.endsWith(".tar.bz2") -> archiveName.dropLast(8)
            lowerName.endsWith(".tar.xz") -> archiveName.dropLast(7)
            lowerName.endsWith(".tar.zst") -> archiveName.dropLast(8)
            lowerName.endsWith(".tar.br") -> archiveName.dropLast(7)
            lowerName.endsWith(".tgz") ||
                lowerName.endsWith(".tbz2") ||
                lowerName.endsWith(".txz") -> archiveName.substringBeforeLast('.')
            lowerName.endsWith(".gz") -> archiveName.dropLast(3)
            lowerName.endsWith(".bz2") -> archiveName.dropLast(4)
            lowerName.endsWith(".xz") -> archiveName.dropLast(3)
            lowerName.endsWith(".lzma") -> archiveName.dropLast(5)
            lowerName.endsWith(".lz4") -> archiveName.dropLast(4)
            lowerName.endsWith(".zst") -> archiveName.dropLast(4)
            lowerName.endsWith(".br") -> archiveName.dropLast(3)
            else -> archiveName.substringBeforeLast('.', archiveName)
        }
    }

    private suspend fun <T> withArchiveFile(uri: Uri, block: (File) -> T): T {
        val local = uri.localFile()
        if (local != null) {
            return withContext(Dispatchers.IO) {
                if (!local.isFile) throw ArchiveException("压缩包不可用")
                block(local)
            }
        }
        return withTempArchive(uri, block)
    }

    private fun packedSizeForVolumes(firstVolume: File): Long {
        return rarVolumeFiles(firstVolume)
            .map { it.length() }
            .sum()
            .coerceAtLeast(firstVolume.length())
    }

    private fun rarHeaders(firstVolume: File, password: String): List<FileHeader> {
        val headers = linkedMapOf<String, FileHeader>()
        rarVolumeFiles(firstVolume).forEach { volume ->
            val archive = if (password.isBlank()) {
                RarArchive(volume)
            } else {
                RarArchive(volume, password)
            }
            archive.use {
                it.fileHeaders.forEach { header ->
                    val key = "${header.fileName}\u0000" +
                        "${header.fullUnpackSize}\u0000${header.fileCRC}"
                    headers.putIfAbsent(key, header)
                }
            }
        }
        return headers.values.toList()
    }

    private fun rarVolumeFiles(firstVolume: File): List<File> {
        val siblings = firstVolume.parentFile?.listFiles().orEmpty()
        val part = parseRarPartVolume(firstVolume.name)
        if (part != null) {
            return siblings
                .mapNotNull { file ->
                    parseRarPartVolume(file.name)
                        ?.takeIf { it.baseName.equals(part.baseName, ignoreCase = true) }
                        ?.let { it.number to file }
                }
                .sortedBy { it.first }
                .map { it.second }
                .ifEmpty { listOf(firstVolume) }
        }
        if (firstVolume.name.endsWith(".rar", ignoreCase = true)) {
            val baseName = firstVolume.name.dropLast(4)
            val legacy = siblings
                .mapNotNull { file ->
                    parseLegacyRarVolume(file.name)
                        ?.takeIf { it.baseName.equals(baseName, ignoreCase = true) }
                        ?.let { it.number to file }
                }
                .sortedBy { it.first }
                .map { it.second }
            if (legacy.isNotEmpty()) return listOf(firstVolume) + legacy
        }
        return listOf(firstVolume)
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
            val cleaned = NameText.sanitizeBidi(
                segment
                    .replace(Regex("[\\u0000-\\u001F<>:\"|?*]"), "_")
                    .trim()
            )
            if (cleaned.isBlank()) return@mapNotNull null
            if (cleaned.toByteArray(Charsets.UTF_8).size <= 255) {
                cleaned
            } else {
                val dot = cleaned.lastIndexOf('.')
                val extension = if (dot > 0 && cleaned.length - dot <= 12) {
                    cleaned.substring(dot)
                } else {
                    ""
                }
                var base = cleaned.removeSuffix(extension)
                while (base.toByteArray(Charsets.UTF_8).size > 200 && base.isNotEmpty()) {
                    base = NameText.truncateByCodePoints(
                        base,
                        base.codePointCount(0, base.length) - 1
                    )
                }
                base + "_" + Integer.toHexString(cleaned.hashCode()) + extension
            }
        }
    }

    private class ArchiveOutput(
        private val resolver: ContentResolver,
        private val treeUri: Uri
    ) {
        // 记录本次解压新建的文件/目录；失败时回滚，避免留下半截文件。
        private val created = mutableListOf<Uri>()

        private fun track(uri: Uri?): Uri? {
            if (uri != null) created += uri
            return uri
        }

        fun rollback() {
            created.asReversed().forEach { uri ->
                runCatching {
                    val file = uri.localFile()
                    if (file != null) {
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    } else {
                        DocumentsContract.deleteDocument(resolver, uri)
                    }
                }
            }
            created.clear()
        }

        private val directoryCache = mutableMapOf<String, Uri?>()

        fun ensureDirectory(parentUri: Uri, name: String): Uri? {
            val cacheKey = "${parentUri}\u0000$name"
            if (directoryCache.containsKey(cacheKey)) {
                return directoryCache[cacheKey]
            }
            val directory = parentUri.localFile()?.let { parent ->
                val directory = File(parent, name)
                if (directory.isDirectory) {
                    Uri.fromFile(directory)
                } else {
                    if (directory.mkdirs()) track(Uri.fromFile(directory)) else null
                }
            } ?: findChild(parentUri, name)
                ?: track(createDocument(parentUri, name, Document.MIME_TYPE_DIR))
            directoryCache[cacheKey] = directory
            return directory
        }

        private val usedNames = mutableMapOf<String, MutableSet<String>>()

        fun createFile(
            parentUri: Uri,
            name: String,
            conflictPolicy: ConflictPolicy
        ): Uri? {
            val used = usedNames.getOrPut(parentUri.toString()) { mutableSetOf() }
            var resolved = name
            if (!used.add(NameText.nfc(resolved).lowercase())) {
                val dot = name.lastIndexOf('.')
                val base = if (dot > 0) name.substring(0, dot) else name
                val ext = if (dot > 0) name.substring(dot) else ""
                var index = 2
                while (!used.add(NameText.nfc("$base ($index)$ext").lowercase())) index++
                resolved = "$base ($index)$ext"
            }
            return createFileInternal(parentUri, resolved, conflictPolicy)
        }

        private fun createFileInternal(
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
                        ConflictPolicy.RENAME -> return createUnique(parentUri, name)
                    }
                }
                return createDocument(parentUri, name, "application/octet-stream")
            }
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

        fun openOutputStream(uri: Uri): OutputStream? =
            uri.localFile()?.let(::FileOutputStream)
                ?: resolver.openOutputStream(uri)

        private fun createUnique(parentUri: Uri, name: String): Uri? {
            val dot = name.lastIndexOf('.')
            val base = if (dot > 0) name.substring(0, dot) else name
            val suffix = if (dot > 0) name.substring(dot) else ""
            var index = 1
            while (index < 10_000) {
                val candidate = "$base ($index)$suffix"
                parentUri.localFile()?.let { parent ->
                    val target = File(parent, candidate)
                    if (!target.exists() && target.createNewFile()) {
                        return track(Uri.fromFile(target))
                    }
                    return@let
                }
                if (findChild(parentUri, candidate) == null) {
                    return createDocument(parentUri, candidate, "application/octet-stream")
                }
                index++
            }
            return null
        }

        private fun createDocument(parentUri: Uri, name: String, mime: String): Uri? =
            track(
                parentUri.localFile()?.let { parent ->
                    val target = File(parent, name)
                    when {
                        target.exists() -> return Uri.fromFile(target)
                        target.createNewFile() -> Uri.fromFile(target)
                        else -> null
                    }
                } ?: run {
                    runCatching {
                        DocumentsContract.createDocument(resolver, parentUri, mime, name)
                    }.getOrNull()
                }
            )

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
        private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 ->
        String.format(java.util.Locale.ROOT, "%.1f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 ->
        String.format(java.util.Locale.ROOT, "%.0f MB", bytes / (1L shl 20).toDouble())
    else -> String.format(java.util.Locale.ROOT, "%.0f KB", bytes / 1024.0)
}

private const val PREPARATION_SHARE = 0.1f
private const val LIST_BATCH_SIZE = 200
        private const val MAX_ENTRIES = 50_000
        private const val MAX_TOTAL_BYTES = 20L * 1024 * 1024 * 1024
    }
}
