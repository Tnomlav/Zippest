package com.decompress.app.data

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal object ZstdCodec {
    private const val CHUNK_SIZE = 128 * 1024
    private const val CONTINUE = 0
    private const val END = 2

    fun inputStream(source: InputStream): InputStream = ZstdInputStream(source)

    fun outputStream(target: OutputStream): OutputStream = ZstdOutputStream(target)

    private interface ZstdLibrary : Library {
        fun ZSTD_createCStream(): Pointer?
        fun ZSTD_freeCStream(stream: Pointer): NativeLong
        fun ZSTD_initCStream(stream: Pointer, compressionLevel: Int): NativeLong
        fun ZSTD_compressStream2(
            stream: Pointer,
            output: ZstdBuffer,
            input: ZstdBuffer,
            endOperation: Int
        ): NativeLong

        fun ZSTD_createDStream(): Pointer?
        fun ZSTD_freeDStream(stream: Pointer): NativeLong
        fun ZSTD_initDStream(stream: Pointer): NativeLong
        fun ZSTD_decompressStream(
            stream: Pointer,
            output: ZstdBuffer,
            input: ZstdBuffer
        ): NativeLong

        fun ZSTD_isError(code: NativeLong): Int
        fun ZSTD_getErrorName(code: NativeLong): String?
    }

    @Structure.FieldOrder("src", "size", "pos")
    class ZstdBuffer : Structure() {
        @JvmField
        var src: Pointer? = null

        @JvmField
        var size: NativeLong = NativeLong(0)

        @JvmField
        var pos: NativeLong = NativeLong(0)
    }

    private val native: ZstdLibrary by lazy {
        Native.load("zstd", ZstdLibrary::class.java)
    }

    private fun check(result: NativeLong) {
        if (native.ZSTD_isError(result) != 0) {
            throw IOException(
                native.ZSTD_getErrorName(result) ?: "Zstandard operation failed"
            )
        }
    }

    private class ZstdOutputStream(
        private val target: OutputStream
    ) : OutputStream() {
        private val stream = native.ZSTD_createCStream()
            ?: throw IOException("Unable to allocate Zstandard compressor")
        private val outputBuffer = ZstdBuffer()
        private val outputMemory = Memory(CHUNK_SIZE.toLong())
        private var closed = false

        init {
            check(native.ZSTD_initCStream(stream, 3))
            resetOutput()
        }

        override fun write(value: Int) {
            write(byteArrayOf(value.toByte()))
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            ensureOpen()
            if (length == 0) return
            val inputMemory = Memory(length.toLong())
            inputMemory.write(0, buffer, offset, length)
            val inputBuffer = ZstdBuffer().apply {
                src = inputMemory
                size = NativeLong(length.toLong())
                pos = NativeLong(0)
                write()
            }
            while (inputBuffer.pos.toLong() < inputBuffer.size.toLong()) {
                check(
                    native.ZSTD_compressStream2(
                        stream,
                        outputBuffer,
                        inputBuffer,
                        CONTINUE
                    )
                )
                inputBuffer.read()
                flushOutput()
            }
        }

        override fun close() {
            if (closed) return
            try {
                val emptyInput = ZstdBuffer().apply {
                    src = null
                    size = NativeLong(0)
                    pos = NativeLong(0)
                    write()
                }
                var remaining: Long
                do {
                    remaining = native.ZSTD_compressStream2(
                        stream,
                        outputBuffer,
                        emptyInput,
                        END
                    ).toLong()
                    check(NativeLong(remaining))
                    flushOutput()
                } while (remaining > 0)
                target.flush()
            } finally {
                closed = true
                native.ZSTD_freeCStream(stream)
                target.close()
            }
        }

        private fun flushOutput() {
            outputBuffer.read()
            val count = outputBuffer.pos.toLong().toInt()
            if (count > 0) {
                target.write(outputMemory.getByteArray(0, count))
                resetOutput()
            }
        }

        private fun resetOutput() {
            outputBuffer.src = outputMemory
            outputBuffer.size = NativeLong(CHUNK_SIZE.toLong())
            outputBuffer.pos = NativeLong(0)
            outputBuffer.write()
        }

        private fun ensureOpen() {
            if (closed) throw IOException("Stream is closed")
        }
    }

    private class ZstdInputStream(
        private val source: InputStream
    ) : InputStream() {
        private val stream = native.ZSTD_createDStream()
            ?: throw IOException("Unable to allocate Zstandard decompressor")
        private val outputBuffer = ZstdBuffer()
        private val outputMemory = Memory(CHUNK_SIZE.toLong())
        private val compressed = ByteArray(CHUNK_SIZE)
        private var compressedMemory: Memory? = null
        private var inputBuffer = ZstdBuffer()
        private var outputPosition = 0
        private var outputLength = 0
        private var sourceEnded = false
        private var frameEnded = false
        private var closed = false

        init {
            check(native.ZSTD_initDStream(stream))
            resetOutput()
        }

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            ensureOpen()
            if (length == 0) return 0
            if (!ensureOutput()) return -1
            val count = minOf(length, outputLength - outputPosition)
            outputMemory.read(
                (outputPosition).toLong(),
                buffer,
                offset,
                count
            )
            outputPosition += count
            return count
        }

        override fun close() {
            if (closed) return
            closed = true
            native.ZSTD_freeDStream(stream)
            source.close()
        }

        private fun ensureOutput(): Boolean {
            while (outputPosition >= outputLength) {
                if (frameEnded || sourceEnded && inputBuffer.pos.toLong() >=
                    inputBuffer.size.toLong()
                ) {
                    return false
                }
                if (inputBuffer.pos.toLong() >= inputBuffer.size.toLong()) {
                    val count = source.read(compressed)
                    if (count < 0) {
                        sourceEnded = true
                        return false
                    }
                    compressedMemory = Memory(count.toLong()).also {
                        it.write(0, compressed, 0, count)
                    }
                    inputBuffer = ZstdBuffer().apply {
                        src = compressedMemory
                        size = NativeLong(count.toLong())
                        pos = NativeLong(0)
                        write()
                    }
                }
                resetOutput()
                val remaining = native.ZSTD_decompressStream(
                    stream,
                    outputBuffer,
                    inputBuffer
                )
                check(remaining)
                outputBuffer.read()
                inputBuffer.read()
                outputLength = outputBuffer.pos.toLong().toInt()
                outputPosition = 0
                frameEnded = remaining.toLong() == 0L && outputLength == 0
            }
            return outputLength > 0
        }

        private fun resetOutput() {
            outputBuffer.src = outputMemory
            outputBuffer.size = NativeLong(CHUNK_SIZE.toLong())
            outputBuffer.pos = NativeLong(0)
            outputBuffer.write()
        }

        private fun ensureOpen() {
            if (closed) throw IOException("Stream is closed")
        }
    }
}
