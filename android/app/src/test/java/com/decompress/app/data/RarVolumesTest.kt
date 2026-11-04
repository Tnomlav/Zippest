package com.decompress.app.data

import com.github.junrar.Archive
import java.io.File
import java.io.OutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RarVolumesTest {
    @Test
    fun recognizesNumberedRarParts() {
        assertEquals(
            RarVolumeName("archive", 1),
            parseRarPartVolume("archive.part1.rar")
        )
        assertEquals(
            RarVolumeName("Archive", 12),
            parseRarPartVolume("Archive.part12.RAR")
        )
        assertEquals(
            RarVolumeName("archive", 0, zeroBased = true),
            parseRarPartVolume("archive.000.rar")
        )
        assertTrue(isSupportedArchiveName("archive.part1.rar"))
        assertNull(parseRarPartVolume("archive.rar"))
    }

    @Test
    fun recognizesLegacyRarVolumes() {
        assertEquals(
            RarVolumeName("archive", 0, zeroBased = true),
            parseLegacyRarVolume("archive.r00")
        )
        assertEquals(
            RarVolumeName("archive", 17, zeroBased = true),
            parseLegacyRarVolume("archive.r17")
        )
        assertNull(parseLegacyRarVolume("archive.rar"))
    }

    @Test
    fun readsModernRarVolumesFromFirstPart() {
        val fixtureDir = rarFixtureDirectory()
        assumeTrue(fixtureDir != null)

        val names = extractHeaders(
            File(requireNotNull(fixtureDir), "test-documents.part1.rar")
        )

        assertEquals(names.toString(), 9, names.size)
        assertTrue(names.contains("testTXT.txt"))
    }

    @Test
    fun readsNumberedRarVolumesFromFirstPart() {
        val fixtureDir = rarFixtureDirectory()
        assumeTrue(fixtureDir != null)

        val names = extractHeaders(
            File(requireNotNull(fixtureDir), "test-documents.000.rar")
        )

        assertEquals(names.toString(), 9, names.size)
        assertTrue(names.contains("testTXT.txt"))
    }

    @Test
    fun readsRar5VolumesFromFirstPart() {
        val fixtureDir = rarFixtureDirectory()
        assumeTrue(fixtureDir != null)

        val names = extractHeaders(File(requireNotNull(fixtureDir), "vols.part1.rar"))

        assertEquals(listOf("note.txt", "spanned.bin"), names)
    }

    @Test
    fun readsLegacyRarVolumesFromFirstPart() {
        val fixtureDir = rarFixtureDirectory()
        assumeTrue(fixtureDir != null)

        val names = extractHeaders(File(requireNotNull(fixtureDir), "r14volcm.rar"))

        assertTrue(names.isNotEmpty())
    }

    private fun extractHeaders(firstVolume: File): List<String> {
        assertTrue(firstVolume.isFile)
        return Archive(firstVolume).use { archive ->
            val names = mutableListOf<String>()
            var header = archive.nextFileHeader()
            while (header != null) {
                names += header.fileName
                if (!header.isDirectory) {
                    archive.getInputStream(header).use { it.copyTo(OutputStream.nullOutputStream()) }
                }
                header = archive.nextFileHeader()
            }
            names
        }
    }

    private fun rarFixtureDirectory(): File? =
        System.getenv("ZIPPEST_RAR_FIXTURES")
            ?.let(::File)
            ?.takeIf { it.isDirectory }
}
