package com.decompress.app.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile as Zip4jFile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Random

@RunWith(AndroidJUnit4::class)
class CompressionEngineDeviceTest {
    private lateinit var context: Context
    private lateinit var root: File
    private lateinit var sourceDirectory: File
    private lateinit var outputDirectory: File
    private lateinit var sourceEntry: BrowserEntry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        root = File(context.cacheDir, "compression-test-${System.nanoTime()}").apply { mkdirs() }
        sourceDirectory = File(root, "资料").apply { mkdirs() }
        outputDirectory = File(root, "output").apply { mkdirs() }
        File(sourceDirectory, "说明.txt").writeText("Zippest compression test")
        File(sourceDirectory, "random.bin").writeBytes(
            ByteArray(180 * 1024).also { Random(42).nextBytes(it) }
        )
        sourceEntry = SafFileRepository(context).directEntry(sourceDirectory)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun createsAndReadsSplitZip() = runBlocking {
        val result = CompressionEngine(context).compress(
            selected = listOf(sourceEntry),
            sourceTreeUri = Uri.fromFile(root),
            targetParentUri = Uri.fromFile(outputDirectory),
            targetTreeUri = Uri.fromFile(outputDirectory),
            options = CompressionOptions(
                format = CompressionFormat.ZIP,
                archiveName = "split.zip",
                level = ArchiveCompressionLevel.FAST,
                password = "split-pass",
                splitSizeBytes = 64L * 1024
            )
        )

        assertTrue(result.partUris.size > 1)
        assertTrue(result.displayName.endsWith(".z01"))
        val finalZip = result.partUris.last().localFile()
        assertTrue(finalZip != null && finalZip.name.endsWith(".zip"))
        assertTrue(Zip4jFile(finalZip!!).isSplitArchive)
        val items = MultiArchiveEngine(context).list(
            archiveUri = result.primaryUri,
            archiveName = result.displayName,
            password = "split-pass",
            archiveTreeUri = Uri.fromFile(outputDirectory),
            archiveParentUri = Uri.fromFile(outputDirectory)
        )
        assertTrue(items.any { it.path == "资料/说明.txt" })
        assertTrue(items.any { it.path == "资料/random.bin" })

        val itemsFromFinalPart = MultiArchiveEngine(context).list(
            archiveUri = result.partUris.last(),
            archiveName = finalZip!!.name,
            password = "split-pass",
            archiveTreeUri = Uri.fromFile(outputDirectory),
            archiveParentUri = Uri.fromFile(outputDirectory)
        )
        assertTrue(itemsFromFinalPart.any { it.path == "资料/random.bin" })

        val extractedDirectory = File(root, "extracted").apply { mkdirs() }
        MultiArchiveEngine(context).extract(
            archiveUri = result.primaryUri,
            archiveName = result.displayName,
            settings = ExtractSettings(
                mode = DestinationMode.CURRENT_FOLDER,
                targetParentUri = Uri.fromFile(extractedDirectory),
                targetTreeUri = Uri.fromFile(extractedDirectory),
                createFolder = true,
                conflictPolicy = ConflictPolicy.RENAME,
                encoding = FileNameEncoding.AUTO,
                password = "split-pass"
            ),
            onProgress = { _, _ -> },
            archiveTreeUri = Uri.fromFile(outputDirectory),
            archiveParentUri = Uri.fromFile(outputDirectory)
        )
        assertEquals(
            File(sourceDirectory, "random.bin").readBytes().toList(),
            File(extractedDirectory, "split/资料/random.bin").readBytes().toList()
        )
    }

    @Test
    fun createsSevenZipTarGzipAndCpio() = runBlocking {
        val engine = CompressionEngine(context)
        val archiveEngine = MultiArchiveEngine(context)
        listOf(
            CompressionFormat.SEVEN_Z,
            CompressionFormat.TAR,
            CompressionFormat.TAR_GZ,
            CompressionFormat.TAR_BZ2,
            CompressionFormat.TAR_XZ,
            CompressionFormat.CPIO
        ).forEach { format ->
            val result = engine.compress(
                selected = listOf(sourceEntry),
                sourceTreeUri = Uri.fromFile(root),
                targetParentUri = Uri.fromFile(outputDirectory),
                targetTreeUri = Uri.fromFile(outputDirectory),
                options = CompressionOptions(
                    format = format,
                    archiveName = "roundtrip${format.extension}"
                )
            )
            val items = archiveEngine.list(
                archiveUri = result.primaryUri,
                archiveName = result.displayName,
                archiveTreeUri = Uri.fromFile(outputDirectory),
                archiveParentUri = Uri.fromFile(outputDirectory)
            )
            assertEquals(
                "${format.label} entry count",
                2,
                items.count { !it.isDirectory }
            )
            assertTrue(items.any { it.path == "资料/说明.txt" })
            assertExtractsToFiles(result, "")
        }
    }

    @Test
    fun createsEncryptedZipReadableWithPassword() = runBlocking {
        val result = CompressionEngine(context).compress(
            selected = listOf(sourceEntry),
            sourceTreeUri = Uri.fromFile(root),
            targetParentUri = Uri.fromFile(outputDirectory),
            targetTreeUri = Uri.fromFile(outputDirectory),
            options = CompressionOptions(
                format = CompressionFormat.ZIP,
                archiveName = "encrypted.zip",
                password = "zippest-123"
            )
        )
        val items = MultiArchiveEngine(context).list(
            archiveUri = result.primaryUri,
            archiveName = result.displayName,
            password = "zippest-123"
        )
        assertTrue(items.any { it.path == "资料/说明.txt" })
        assertExtractsToFiles(result, "zippest-123")
    }

    @Test
    fun createsEncryptedSevenZipReadableWithPassword() = runBlocking {
        val result = CompressionEngine(context).compress(
            selected = listOf(sourceEntry),
            sourceTreeUri = Uri.fromFile(root),
            targetParentUri = Uri.fromFile(outputDirectory),
            targetTreeUri = Uri.fromFile(outputDirectory),
            options = CompressionOptions(
                format = CompressionFormat.SEVEN_Z,
                archiveName = "encrypted.7z",
                password = "zippest-123"
            )
        )
        val items = MultiArchiveEngine(context).list(
            archiveUri = result.primaryUri,
            archiveName = result.displayName,
            password = "zippest-123"
        )
        assertTrue(items.any { it.path == "资料/说明.txt" })
        assertExtractsToFiles(result, "zippest-123")
    }

    @Test
    fun createsAndReadsSplitSevenZip() = runBlocking {
        val result = CompressionEngine(context).compress(
            selected = listOf(sourceEntry),
            sourceTreeUri = Uri.fromFile(root),
            targetParentUri = Uri.fromFile(outputDirectory),
            targetTreeUri = Uri.fromFile(outputDirectory),
            options = CompressionOptions(
                format = CompressionFormat.SEVEN_Z,
                archiveName = "split.7z",
                splitSizeBytes = 64L * 1024
            )
        )
        assertTrue(result.partUris.size > 1)
        val items = MultiArchiveEngine(context).list(
            archiveUri = result.primaryUri,
            archiveName = result.displayName,
            archiveTreeUri = Uri.fromFile(outputDirectory),
            archiveParentUri = Uri.fromFile(outputDirectory)
        )
        assertTrue(items.any { it.path == "资料/random.bin" })
    }

    @Test
    fun createsLzmaAndLz4SingleFileStreams() = runBlocking {
        val engine = CompressionEngine(context)
        val archiveEngine = MultiArchiveEngine(context)
        val sourceFile = File(sourceDirectory, "说明.txt")
        val source = SafFileRepository(context).directEntry(sourceFile)
        listOf(
            CompressionFormat.GZIP,
            CompressionFormat.BZIP2,
            CompressionFormat.XZ,
            CompressionFormat.LZMA,
            CompressionFormat.LZ4
        ).forEach { format ->
            val result = engine.compress(
                selected = listOf(source),
                sourceTreeUri = Uri.fromFile(sourceDirectory),
                targetParentUri = Uri.fromFile(outputDirectory),
                targetTreeUri = Uri.fromFile(outputDirectory),
                options = CompressionOptions(
                    format = format,
                    archiveName = "说明.txt${format.extension}"
                )
            )
            val items = archiveEngine.list(
                archiveUri = result.primaryUri,
                archiveName = result.displayName
            )
            assertEquals(1, items.size)
            assertEquals("说明.txt", items.single().path)
            val extracted = File(root, "single-${format.name}").apply { mkdirs() }
            archiveEngine.extract(
                archiveUri = result.primaryUri,
                archiveName = result.displayName,
                settings = ExtractSettings(
                    mode = DestinationMode.CURRENT_FOLDER,
                    targetParentUri = Uri.fromFile(extracted),
                    targetTreeUri = Uri.fromFile(extracted),
                    createFolder = false,
                    conflictPolicy = ConflictPolicy.RENAME,
                    encoding = FileNameEncoding.AUTO,
                    password = ""
                ),
                onProgress = { _, _ -> }
            )
            assertEquals(
                sourceFile.readText(),
                File(extracted, "说明.txt").readText()
            )
        }
    }

    @Test
    fun createsZstandardStreamsAndTarZstandard() = runBlocking {
        val engine = CompressionEngine(context)
        val archiveEngine = MultiArchiveEngine(context)
        val tarResult = engine.compress(
            selected = listOf(sourceEntry),
            sourceTreeUri = Uri.fromFile(root),
            targetParentUri = Uri.fromFile(outputDirectory),
            targetTreeUri = Uri.fromFile(outputDirectory),
            options = CompressionOptions(
                format = CompressionFormat.TAR_ZSTD,
                archiveName = "zstd.tar.zst"
            )
        )
        assertExtractsToFiles(tarResult, "")

        val sourceFile = File(sourceDirectory, "说明.txt")
        val streamResult = engine.compress(
            selected = listOf(
                SafFileRepository(context).directEntry(sourceFile)
            ),
            sourceTreeUri = Uri.fromFile(sourceDirectory),
            targetParentUri = Uri.fromFile(outputDirectory),
            targetTreeUri = Uri.fromFile(outputDirectory),
            options = CompressionOptions(
                format = CompressionFormat.ZSTD,
                archiveName = "说明.txt.zst"
            )
        )
        assertEquals(
            1,
            archiveEngine.list(
                archiveUri = streamResult.primaryUri,
                archiveName = streamResult.displayName
            ).size
        )
        val extracted = File(root, "zstd-extracted").apply { mkdirs() }
        archiveEngine.extract(
            archiveUri = streamResult.primaryUri,
            archiveName = streamResult.displayName,
            settings = ExtractSettings(
                mode = DestinationMode.CURRENT_FOLDER,
                targetParentUri = Uri.fromFile(extracted),
                targetTreeUri = Uri.fromFile(extracted),
                createFolder = false,
                conflictPolicy = ConflictPolicy.RENAME,
                encoding = FileNameEncoding.AUTO,
                password = ""
            ),
            onProgress = { _, _ -> }
        )
        assertEquals(
            sourceFile.readText(),
            File(extracted, "说明.txt").readText()
        )
    }

    @Test
    fun createsAndReadsGenericSplitArchives() = runBlocking {
        val engine = CompressionEngine(context)
        val archiveEngine = MultiArchiveEngine(context)
        val sourceFile = File(sourceDirectory, "random.bin")
        val singleSource = SafFileRepository(context).directEntry(sourceFile)
        val multiFormats = listOf(
            CompressionFormat.TAR,
            CompressionFormat.TAR_GZ,
            CompressionFormat.TAR_BZ2,
            CompressionFormat.TAR_XZ,
            CompressionFormat.CPIO
        )
        multiFormats.forEach { format ->
            val result = engine.compress(
                selected = listOf(sourceEntry),
                sourceTreeUri = Uri.fromFile(root),
                targetParentUri = Uri.fromFile(outputDirectory),
                targetTreeUri = Uri.fromFile(outputDirectory),
                options = CompressionOptions(
                    format = format,
                    archiveName = "split-${format.name.lowercase()}${format.extension}",
                    splitSizeBytes = 64L * 1024
                )
            )
            assertTrue("${format.label} did not create multiple parts", result.partUris.size > 1)
            val items = archiveEngine.list(
                archiveUri = result.primaryUri,
                archiveName = result.displayName,
                archiveTreeUri = Uri.fromFile(outputDirectory),
                archiveParentUri = Uri.fromFile(outputDirectory)
            )
            assertTrue(items.any { it.path == "资料/random.bin" })
            val extracted = File(root, "split-extract-${format.name}").apply { mkdirs() }
            archiveEngine.extract(
                archiveUri = result.primaryUri,
                archiveName = result.displayName,
                settings = ExtractSettings(
                    mode = DestinationMode.CURRENT_FOLDER,
                    targetParentUri = Uri.fromFile(extracted),
                    targetTreeUri = Uri.fromFile(extracted),
                    createFolder = false,
                    conflictPolicy = ConflictPolicy.RENAME,
                    encoding = FileNameEncoding.AUTO,
                    password = ""
                ),
                onProgress = { _, _ -> },
                archiveTreeUri = Uri.fromFile(outputDirectory),
                archiveParentUri = Uri.fromFile(outputDirectory)
            )
            assertEquals(
                sourceFile.readBytes().toList(),
                File(extracted, "资料/random.bin").readBytes().toList()
            )
        }

        listOf(
            CompressionFormat.GZIP,
            CompressionFormat.BZIP2,
            CompressionFormat.XZ,
            CompressionFormat.LZMA,
            CompressionFormat.LZ4
        ).forEach { format ->
            val result = engine.compress(
                selected = listOf(singleSource),
                sourceTreeUri = Uri.fromFile(sourceDirectory),
                targetParentUri = Uri.fromFile(outputDirectory),
                targetTreeUri = Uri.fromFile(outputDirectory),
                options = CompressionOptions(
                    format = format,
                    archiveName = "random.bin${format.extension}",
                    splitSizeBytes = 64L * 1024
                )
            )
            assertTrue("${format.label} did not create multiple parts", result.partUris.size > 1)
            val items = archiveEngine.list(
                archiveUri = result.primaryUri,
                archiveName = result.displayName,
                archiveTreeUri = Uri.fromFile(outputDirectory),
                archiveParentUri = Uri.fromFile(outputDirectory)
            )
            assertEquals(1, items.size)
            val extracted = File(root, "single-split-extract-${format.name}").apply { mkdirs() }
            archiveEngine.extract(
                archiveUri = result.primaryUri,
                archiveName = result.displayName,
                settings = ExtractSettings(
                    mode = DestinationMode.CURRENT_FOLDER,
                    targetParentUri = Uri.fromFile(extracted),
                    targetTreeUri = Uri.fromFile(extracted),
                    createFolder = false,
                    conflictPolicy = ConflictPolicy.RENAME,
                    encoding = FileNameEncoding.AUTO,
                    password = ""
                ),
                onProgress = { _, _ -> },
                archiveTreeUri = Uri.fromFile(outputDirectory),
                archiveParentUri = Uri.fromFile(outputDirectory)
            )
            assertEquals(
                sourceFile.readBytes().toList(),
                File(extracted, "random.bin").readBytes().toList()
            )
        }
    }

    private suspend fun assertExtractsToFiles(
        result: CompressionResult,
        password: String
    ) {
        val extracted = File(root, "extract-${System.nanoTime()}").apply { mkdirs() }
        MultiArchiveEngine(context).extract(
            archiveUri = result.primaryUri,
            archiveName = result.displayName,
            settings = ExtractSettings(
                mode = DestinationMode.CURRENT_FOLDER,
                targetParentUri = Uri.fromFile(extracted),
                targetTreeUri = Uri.fromFile(extracted),
                createFolder = false,
                conflictPolicy = ConflictPolicy.RENAME,
                encoding = FileNameEncoding.AUTO,
                password = password
            ),
            onProgress = { _, _ -> }
        )
        assertEquals(
            File(sourceDirectory, "说明.txt").readText(),
            File(extracted, "资料/说明.txt").readText()
        )
        assertEquals(
            File(sourceDirectory, "random.bin").readBytes().toList(),
            File(extracted, "资料/random.bin").readBytes().toList()
        )
    }
}
