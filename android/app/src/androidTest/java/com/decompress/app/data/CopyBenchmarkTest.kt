package com.decompress.app.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 复制性能对照：顺序 vs 4 路并发。
 * 只写应用 cacheDir，不碰用户文件；文件总量 300 × 16 KB ≈ 4.7 MB。
 */
@RunWith(AndroidJUnit4::class)
class CopyBenchmarkTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun sequentialVersusParallelCopy(): Unit = runBlocking {
        val root = File(context.cacheDir, "copy-bench").apply {
            deleteRecursively()
            mkdirs()
        }
        val source = File(root, "source").apply { mkdirs() }
        repeat(FILES) { index ->
            File(source, "file-$index.bin").writeBytes(ByteArray(FILE_BYTES) { (index % 251).toByte() })
        }
        val repository = SafFileRepository(context)

        val sequentialTarget = File(root, "sequential").apply { mkdirs() }
        val sequentialStart = System.currentTimeMillis()
        source.listFiles().orEmpty().forEach { file ->
            val result = repository.copyOrMoveDirect(
                Uri.fromFile(file),
                Uri.fromFile(sequentialTarget),
                move = false
            )
            assertEquals(TransferOutcome.COMPLETED, result.outcome)
        }
        val sequentialMillis = System.currentTimeMillis() - sequentialStart

        val parallelTarget = File(root, "parallel").apply { mkdirs() }
        val parallelStart = System.currentTimeMillis()
        coroutineScope {
            val permits = Semaphore(PARALLELISM)
            source.listFiles().orEmpty().map { file ->
                async(Dispatchers.IO) {
                    permits.withPermit {
                        repository.copyOrMoveDirect(
                            Uri.fromFile(file),
                            Uri.fromFile(parallelTarget),
                            move = false
                        ).outcome
                    }
                }
            }.awaitAll().forEach { assertEquals(TransferOutcome.COMPLETED, it) }
        }
        val parallelMillis = System.currentTimeMillis() - parallelStart

        println("COPY_BENCH files=$FILES bytes=$FILE_BYTES sequential=${sequentialMillis}ms parallel=${parallelMillis}ms")
        assertEquals(FILES, sequentialTarget.listFiles()?.size)
        assertEquals(FILES, parallelTarget.listFiles()?.size)

        root.deleteRecursively()
        println(
            "COPY_BENCH files=$FILES bytes=$FILE_BYTES " +
                "sequential=${sequentialMillis}ms parallel=${parallelMillis}ms"
        )
    }

    private companion object {
        const val FILES = 300
        const val FILE_BYTES = 64 * 1024
        const val PARALLELISM = 4
    }
}
