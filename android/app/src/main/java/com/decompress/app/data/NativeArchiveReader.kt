package com.decompress.app.data

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveEntry

internal data class NativeArchiveEntry(
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)

/**
 * Thin stateful wrapper around the native libarchive read API.
 *
 * A handle can enumerate or extract one archive at a time and must be closed on
 * the same thread that consumes it.
 */
internal class NativeArchiveReader private constructor(
    private val archive: Long,
    firstFile: File
) : Closeable {
    private val inputBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
    private var closed = false

    init {
        require(firstFile.isFile) { "原生压缩包文件不可用" }
    }

    fun nextEntry(): NativeArchiveEntry? {
        check(!closed) { "原生压缩包已关闭" }
        while (true) {
            val entry = Archive.readNextHeader(archive)
            if (entry == 0L) return null
            val rawPath = ArchiveEntry.pathnameUtf8(entry)
                ?: ArchiveEntry.pathname(entry)
                    ?.toString(StandardCharsets.UTF_8)
                ?: continue
            val type = if (ArchiveEntry.filetypeIsSet(entry)) {
                ArchiveEntry.filetype(entry)
            } else {
                0
            }
            return NativeArchiveEntry(
                path = rawPath,
                isDirectory = type == ArchiveEntry.AE_IFDIR || rawPath.endsWith('/'),
                size = if (ArchiveEntry.sizeIsSet(entry)) {
                    ArchiveEntry.size(entry).coerceAtLeast(0L)
                } else {
                    0L
                }
            )
        }
    }

    fun openEntryInputStream(): InputStream = EntryInputStream()

    override fun close() {
        if (closed) return
        closed = true
        runCatching { Archive.readClose(archive) }
        runCatching { Archive.free(archive) }
    }

    private inner class EntryInputStream : InputStream() {
        override fun read(): Int {
            val single = ByteArray(1)
            val count = read(single, 0, 1)
            return if (count <= 0) -1 else single[0].toInt() and 0xff
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            check(!closed) { "原生压缩包已关闭" }
            if (length == 0) return 0
            val readLength = minOf(length, inputBuffer.capacity())
            inputBuffer.clear()
            inputBuffer.limit(readLength)
            Archive.readData(archive, inputBuffer)
            val count = inputBuffer.position()
            if (count <= 0) return -1
            inputBuffer.flip()
            inputBuffer.get(buffer, offset, count)
            return count
        }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024

        fun open(
            files: List<File>,
            password: String,
            charsetName: String = StandardCharsets.UTF_8.name()
        ): NativeArchiveReader {
            require(files.isNotEmpty()) { "压缩包不可用" }
            val archive = Archive.readNew()
            try {
                Archive.setCharset(
                    archive,
                    charsetName.toByteArray(StandardCharsets.UTF_8)
                )
                Archive.readSupportFilterAll(archive)
                Archive.readSupportFormat7zip(archive)
                Archive.readSupportFormatRar(archive)
                Archive.readSupportFormatRar5(archive)
                if (password.isNotEmpty()) {
                    Archive.readAddPassphrase(
                        archive,
                        password.toByteArray(StandardCharsets.UTF_8)
                    )
                }
                val names = Array(files.size) { index ->
                    files[index].absolutePath.toByteArray(StandardCharsets.UTF_8)
                }
                Archive.readOpenFileNames(archive, names, BUFFER_SIZE.toLong())
                return NativeArchiveReader(archive, files.first())
            } catch (error: Throwable) {
                runCatching { Archive.free(archive) }
                throw error
            }
        }
    }
}
