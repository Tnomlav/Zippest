package com.decompress.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ArchiveSafetyTest {
    @Test
    fun rejectsArchiveThatExceedsExpansionRatio() {
        assertThrows(ArchiveException::class.java) {
            ensureSafeExpansion(
                unpackedBytes = 2L * 1024 * 1024 * 1024,
                packedBytes = 1024L * 1024
            )
        }
    }

    @Test
    fun allowsNormalExpansion() {
        ensureSafeExpansion(
            unpackedBytes = 100L * 1024 * 1024,
            packedBytes = 50L * 1024 * 1024
        )
    }

    @Test
    fun boundedCopyRejectsUnexpectedExtraData() {
        val input = ByteArrayInputStream(ByteArray(32) { it.toByte() })
        val output = ByteArrayOutputStream()

        assertThrows(ArchiveException::class.java) {
            copyWithLimit(input, output, remainingBytes = 16)
        }
    }

    @Test
    fun boundedCopyReturnsWrittenByteCount() {
        val input = ByteArrayInputStream("Zippest".toByteArray())
        val output = ByteArrayOutputStream()

        val written = copyWithLimit(input, output, remainingBytes = 32)

        assertEquals(7L, written)
        assertEquals("Zippest", output.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun boundedCopyReportsMonotonicProgress() {
        val input = ByteArrayInputStream(ByteArray(256 * 1024) { it.toByte() })
        val progress = mutableListOf<Long>()

        val written = copyWithLimit(
            input = input,
            output = ByteArrayOutputStream(),
            remainingBytes = Long.MAX_VALUE,
            onProgress = { progress += it }
        )

        assertEquals(written, progress.last())
        assertEquals(
            progress.sorted(),
            progress
        )
        assertEquals(4, progress.size)
    }

    @Test
    fun inputStreamProgressIsClampedAndMonotonic() {
        val progress = mutableListOf<Float>()
        val input = ProgressInputStream(
            ByteArrayInputStream(ByteArray(200) { it.toByte() }),
            totalBytes = 100L,
            onProgress = { progress += it }
        )

        input.copyTo(ByteArrayOutputStream())

        assertEquals(progress.sorted(), progress)
        assertEquals(1f, progress.last())
        assertTrue(progress.all { it in 0f..1f })
    }
}
