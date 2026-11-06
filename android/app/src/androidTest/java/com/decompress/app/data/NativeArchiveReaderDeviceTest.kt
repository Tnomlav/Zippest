package com.decompress.app.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.OutputStream
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeArchiveReaderDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val root = File(context.cacheDir, "native-archive-test-${System.nanoTime()}")

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun readsSevenZipAndRarVariants() {
        val fixtures = listOf(
            Triple("sample-7z.7z", "", "README.md"),
            Triple("sample-rar.rar", "", "LICENSE.txt")
        )
        fixtures.forEach { (assetName, password, expectedEntry) ->
            val file = copyAsset(assetName)
            val entries = mutableListOf<String>()
            try {
                NativeArchiveReader.open(listOf(file), password).use { reader ->
                    while (true) {
                        val entry = reader.nextEntry() ?: break
                        entries += entry.path
                        if (!entry.isDirectory) {
                            reader.openEntryInputStream()
                                .use { it.copyTo(OutputStream.nullOutputStream()) }
                        }
                    }
                }
            } catch (error: Throwable) {
                throw AssertionError("$assetName: ${error.message}", error)
            }
            assertTrue(
                "$assetName did not expose $expectedEntry: $entries",
                entries.any { it.endsWith(expectedEntry) }
            )
        }
    }

    private fun copyAsset(name: String): File {
        root.mkdirs()
        val target = File(root, name)
        instrumentation.context.assets.open("archives/$name").use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }
}
