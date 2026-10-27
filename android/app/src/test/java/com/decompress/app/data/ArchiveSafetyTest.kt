package com.decompress.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
}
