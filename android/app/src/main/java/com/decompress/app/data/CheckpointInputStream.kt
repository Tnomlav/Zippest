package com.decompress.app.data

import java.io.InputStream

internal class CheckpointInputStream(
    private val delegate: InputStream,
    private val onCheckpoint: () -> Unit
) : InputStream() {
    private var bytesSinceCheckpoint = 0

    override fun read(): Int {
        checkpointIfNeeded(1)
        return delegate.read()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        checkpointIfNeeded(length)
        return delegate.read(buffer, offset, length)
    }

    override fun available(): Int = delegate.available()

    override fun close() = delegate.close()

    private fun checkpointIfNeeded(length: Int) {
        bytesSinceCheckpoint += length
        if (bytesSinceCheckpoint >= CHECKPOINT_BYTES) {
            bytesSinceCheckpoint = 0
            onCheckpoint()
        }
    }

    private companion object {
        const val CHECKPOINT_BYTES = 512 * 1024
    }
}
