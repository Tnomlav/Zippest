package com.decompress.app.data

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * 断点续写的核心证明：
 * 写到一半直接丢弃（模拟进程被杀），按记录的偏移截断后继续写，
 * 产出的 TAR / TAR.GZ / TAR.BZ2 / TAR.XZ 必须能被完整读回。
 *
 * 条目里特意放了一个目录（`sub/`），用来覆盖“最后完成的条目是目录”时
 * 记录名带斜杠、恢复校验要归一化的情况。
 */
class ResumableTarWriterTest {

    @Test
    fun tarResumesAfterCrash() = assertResume(CompressionFormat.TAR)

    @Test
    fun tarGzResumesAfterCrash() = assertResume(CompressionFormat.TAR_GZ)

    @Test
    fun tarBz2ResumesAfterCrash() = assertResume(CompressionFormat.TAR_BZ2)

    @Test
    fun tarXzResumesAfterCrash() = assertResume(CompressionFormat.TAR_XZ)

    private fun assertResume(format: CompressionFormat) {
        val dir = File(
            System.getProperty("java.io.tmpdir"),
            "zippest-resume-${format.name}-${System.nanoTime()}"
        )
        dir.mkdirs()
        val target = File(dir, "out${format.extension}")
        val entries: List<Pair<String, ByteArray?>> = listOf(
            "file-1.txt" to payload(1),
            "sub/" to null,
            "file-2.txt" to payload(2),
            "file-3.txt" to payload(3),
            "file-4.txt" to payload(4),
            "file-5.txt" to payload(5)
        )
        val fileEntries = entries.filter { it.second != null }

        var sectionCount = 0
        var sectionOffset = 0L
        var sectionLastEntry = ""

        val first = ResumableTarWriter(
            file = target,
            format = format,
            level = 6,
            segmentBytes = 1024,
            onSection = { count, name, offset ->
                sectionCount = count
                sectionOffset = offset
                sectionLastEntry = name
            }
        )
        entries.take(3).forEach { (name, bytes) ->
            if (bytes == null) {
                first.putEntry(name, 0L, FIXED_TIME, true, null)
            } else {
                first.putEntry(name, bytes.size.toLong(), FIXED_TIME, false) { it.write(bytes) }
            }
        }
        // 模拟进程被杀：不写 TAR 结束块，也不写最后一个压缩成员的尾。
        first.abandon()

        assertTrue("发生过分段，才有可续的偏移", sectionCount > 0 && sectionOffset > 0)
        assertTrue("分段偏移不应超过文件长度", sectionOffset <= target.length())

        val second = ResumableTarWriter(
            file = target,
            format = format,
            level = 6,
            segmentBytes = 1024,
            startOffset = sectionOffset,
            startEntryCount = sectionCount,
            lastEntryName = sectionLastEntry,
            onSection = { _, _, _ -> }
        )
        entries.drop(sectionCount).forEach { (name, bytes) ->
            if (bytes == null) {
                second.putEntry(name, 0L, FIXED_TIME, true, null)
            } else {
                second.putEntry(name, bytes.size.toLong(), FIXED_TIME, false) { it.write(bytes) }
            }
        }
        second.finish()

        val read = readEntries(target, format)
        assertEquals(fileEntries.map { it.first }, read.map { it.first })
        fileEntries.forEachIndexed { index, entry ->
            assertArrayEquals("内容不一致：${entry.first}", entry.second, read[index].second)
        }

        // 留一份产物给外部工具（python tarfile）交叉验证。
        val artifacts = File("build/resume-artifacts").apply { mkdirs() }
        target.copyTo(File(artifacts, target.name), overwrite = true)
        dir.deleteRecursively()
    }

    private fun readEntries(
        file: File,
        format: CompressionFormat
    ): List<Pair<String, ByteArray>> {
        val raw = FileInputStream(file)
        val decompressed: InputStream = when (format) {
            CompressionFormat.TAR -> raw
            CompressionFormat.TAR_GZ -> GzipCompressorInputStream(raw, true)
            CompressionFormat.TAR_BZ2 -> BZip2CompressorInputStream(raw, true)
            CompressionFormat.TAR_XZ -> XZCompressorInputStream(raw, true)
            else -> error("unsupported")
        }
        val result = mutableListOf<Pair<String, ByteArray>>()
        TarArchiveInputStream(decompressed).use { tar ->
            var entry: TarArchiveEntry? = tar.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    result += entry.name to tar.readBytes()
                }
                entry = tar.nextEntry
            }
        }
        return result
    }

    private fun payload(index: Int): ByteArray =
        ByteArray(600) { position -> ((index * 31 + position) % 251).toByte() }

    private companion object {
        const val FIXED_TIME = 1_700_000_000_000L
    }
}
