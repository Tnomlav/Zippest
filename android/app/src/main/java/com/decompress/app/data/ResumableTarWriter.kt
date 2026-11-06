package com.decompress.app.data

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.Date

/**
 * 可续写的 TAR 写入器。
 *
 * 做法：TAR 本身是顺序流，压缩器（gzip / bzip2 / xz）支持“多成员拼接”，
 * 所以每写满一个分段就把当前压缩成员收尾、在同一文件末尾开一个新成员，
 * 并在成员边界记录「已完成条目数 + 文件偏移」。进程被杀后从该偏移截断、
 * 新开成员继续写，读侧看到的是连续的 TAR 字节流。
 *
 * 只在直连存储目录下使用（需要能截断文件），SAF 目标走原有的一次性写入。
 */
internal class ResumableTarWriter(
    private val file: File,
    private val format: CompressionFormat,
    level: Int,
    private val startOffset: Long = 0L,
    private val startEntryCount: Int = 0,
    private var lastEntryName: String = "",
    private val segmentBytes: Long = DEFAULT_SEGMENT_BYTES,
    private val onProgress: (Int, Int) -> Unit = { _, _ -> },
    private val onSection: (entryCount: Int, lastEntryName: String, offset: Long) -> Unit
) : Closeable {

    init {
        // 先按记录偏移截断，再以追加方式打开：崩溃前多写的半截数据全部丢弃。
        if (startOffset > 0L && file.length() > startOffset) {
            RandomAccessFile(file, "rw").use { it.setLength(startOffset) }
        }
    }

    private val fileOut = FileOutputStream(file, startOffset > 0L)
    private val switchable = SwitchableOutput(fileOut, format, level)
    private val buffered = BufferedOutputStream(switchable, BUFFER_SIZE)
    private val tar = TarArchiveOutputStream(buffered).apply {
        setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
        setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
    }

    private var entryCount = startEntryCount
    private var bytesSinceSection = 0L
    private var closed = false

    fun putEntry(
        name: String,
        size: Long,
        lastModified: Long,
        isDirectory: Boolean,
        writeBody: ((OutputStream) -> Unit)?
    ) {
        val entry = TarArchiveEntry(name).apply {
            setMode(if (isDirectory) 0x1ED else 0x1A4)
            setSize(if (isDirectory) 0L else size)
            setModTime(Date(lastModified.takeIf { it > 0 } ?: System.currentTimeMillis()))
        }
        tar.putArchiveEntry(entry)
        if (!isDirectory && writeBody != null) {
            writeBody(tar)
        }
        tar.closeArchiveEntry()
        entryCount++
        lastEntryName = name
        bytesSinceSection += size + 512L
        if (bytesSinceSection >= segmentBytes) {
            sealSection()
        }
    }

    /** 收尾：写 TAR 结束块、结束最后一个压缩成员、落盘。 */
    fun finish() {
        if (closed) return
        tar.finish()
        tar.flush()
        buffered.flush()
        switchable.close()
        fileOut.flush()
        fileOut.channel.force(false)
        fileOut.close()
        closed = true
    }

    /** 仅用于测试/异常路径：丢弃未收尾的字节，不写 TAR 结束块。 */
    internal fun abandon() {
        if (closed) return
        runCatching { fileOut.flush() }
        runCatching { fileOut.close() }
        closed = true
    }

    private fun sealSection() {
        tar.flush()
        buffered.flush()
        switchable.finishMember()
        fileOut.flush()
        fileOut.channel.force(false)
        val offset = fileOut.channel.position()
        onSection(entryCount, lastEntryName, offset)
        switchable.startMember()
        bytesSinceSection = 0L
    }

    override fun close() {
        finish()
    }

    /**
     * TAR 写当前压缩成员；收尾成员时只 flush 底层文件，不关闭它，
     * 这样能在同一个文件后面继续追加新成员。
     */
    private class SwitchableOutput(
        fileOut: FileOutputStream,
        private val format: CompressionFormat,
        private val level: Int
    ) : OutputStream() {
        private val sink = object : FilterOutputStream(fileOut) {
            override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
            override fun close() {
                flush()
            }
        }
        private var member: OutputStream = newMember()

        override fun write(b: Int) = member.write(b)

        override fun write(b: ByteArray, off: Int, len: Int) = member.write(b, off, len)

        override fun flush() = member.flush()

        override fun close() {
            runCatching { member.flush() }
            runCatching { member.close() }
            sink.flush()
        }

        fun finishMember() {
            runCatching { member.flush() }
            member.close()
        }

        fun startMember() {
            member = newMember()
        }

        private fun newMember(): OutputStream = when (format) {
            CompressionFormat.TAR -> sink
            CompressionFormat.TAR_GZ -> GzipCompressorOutputStream(
                sink,
                GzipParameters().apply { compressionLevel = level.coerceIn(1, 9) }
            )
            CompressionFormat.TAR_BZ2 -> BZip2CompressorOutputStream(
                sink,
                level.coerceIn(1, 9)
            )
            CompressionFormat.TAR_XZ -> XZCompressorOutputStream(
                sink,
                level.coerceIn(0, 9)
            )
            else -> error("${format.label} 不支持断点续传")
        }
    }

    companion object {
        const val DEFAULT_SEGMENT_BYTES = 64L * 1024 * 1024
        private const val BUFFER_SIZE = 64 * 1024

        fun supports(format: CompressionFormat): Boolean = when (format) {
            CompressionFormat.TAR,
            CompressionFormat.TAR_GZ,
            CompressionFormat.TAR_BZ2,
            CompressionFormat.TAR_XZ -> true
            else -> false
        }
    }
}
