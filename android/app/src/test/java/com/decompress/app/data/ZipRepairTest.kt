package com.decompress.app.data

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipRepairTest {
    @Test
    fun repairsZipWithTruncatedCentralDirectory() {
        val dir = createTempDir()
        val source = File(dir, "broken.zip")
        val content = "hello zippest repair".toByteArray()
        ZipOutputStream(source.outputStream()).use { zip ->
            listOf("a/one.txt", "two.bin").forEachIndexed { index, name ->
                val entry = ZipEntry(name)
                zip.putNextEntry(entry)
                zip.write(if (index == 0) content else content.reversedArray())
                zip.closeEntry()
            }
        }
        val fullLength = source.length()
        java.io.RandomAccessFile(source, "rw").use { it.setLength(fullLength - 30) }

        val output = File(dir, "fixed.zip")
        val result = ZipRepair.repair(source, output)

        if (result == null) {
            val reason = runCatching {
                java.util.zip.ZipFile(output).use { it.entries().asSequence().count() }
            }.exceptionOrNull()
            throw AssertionError("repair returned null, zip read error: $reason")
        }
        assertEquals(2, result!!.entries)
        java.util.zip.ZipFile(output).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            assertTrue(names.contains("a/one.txt"))
            val data = zip.getInputStream(zip.getEntry("a/one.txt")).readBytes()
            assertEquals(String(content), String(data))
            val crc = CRC32().apply { update(data) }.value
            assertEquals(crc, zip.getEntry("a/one.txt").crc)
        }
    }

    private fun createTempDir(): File =
        kotlin.io.path.createTempDirectory("ziprepair").toFile()
}
