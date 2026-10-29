package com.decompress.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ArchiveCompatibilityDeviceTest {
    private lateinit var context: Context
    private lateinit var root: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(context.cacheDir, "archive-fixtures-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun zip() = verifyFixture("sample-archive-2.zip")

    @Test
    fun encryptedZip() = verifyFixture("encrypted-secret.zip", "Secret42")

    @Test
    fun sevenZip() = verifyFixture("sample-7z.7z")

    @Test
    fun encryptedSevenZip() = verifyFixture("sample-7z-encrypted.7z", "Secret42")

    @Test
    fun rar4() = verifyFixture("sample-rar.rar")

    @Test
    fun rar5WithPassword() = verifyFixture("sample-rar-password.rar", "junrar")

    @Test
    fun rar5WithEncryptedHeaders() =
        verifyFixture("sample-rar-encrypted-header.rar", "junrar")

    @Test
    fun tarGzip() = verifyFixture(
        name = "sample-tar-gz.tar.gz",
        assetName = "sample-tar-gz.tar.gz.bin"
    )

    @Test
    fun tarBzip2() = verifyFixture("sample-tar-bz2.tar.bz2")

    @Test
    fun tarXz() = verifyFixture("sample-tar-xz.tar.xz")

    @Test
    fun singleBzip2() = verifyFixture("sample-single.bz2")

    @Test
    fun singleXz() = verifyFixture("sample-single.xz")

    @Test
    fun singleBrotli() = verifyFixture("sample-single.br")

    @Test
    fun tarBrotli() = verifyFixture("sample-tar.br")

    private fun verifyFixture(
        name: String,
        password: String = "",
        assetName: String = name
    ) = runBlocking {
        val source = File(root, name)
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("archives/$assetName")
            .use { input ->
                source.outputStream().use { output -> input.copyTo(output) }
            }
        val engine = MultiArchiveEngine(context)
        assertTrue("$name is not recognized", engine.supports(name))
        Log.i(TAG, "$name: list start")
        val items = engine.list(Uri.fromFile(source), name, password)
        Log.i(TAG, "$name: list complete")
        assertTrue("$name returned no entries", items.isNotEmpty())
        Log.i(TAG, "$name: test start")
        val tested = engine.test(
            archiveUri = Uri.fromFile(source),
            archiveName = name,
            password = password,
            onProgress = { _, _ -> }
        )
        Log.i(TAG, "$name: test complete")
        assertTrue("$name failed integrity traversal", tested > 0)

        val destination = File(root, "extracted-${name.hashCode()}").apply { mkdirs() }
        Log.i(TAG, "$name: extract start")
        engine.extract(
            archiveUri = Uri.fromFile(source),
            archiveName = name,
            settings = ExtractSettings(
                mode = DestinationMode.CURRENT_FOLDER,
                targetParentUri = Uri.fromFile(destination),
                targetTreeUri = Uri.fromFile(destination),
                createFolder = false,
                conflictPolicy = ConflictPolicy.RENAME,
                encoding = FileNameEncoding.AUTO,
                password = password
            ),
            onProgress = { _, _ -> }
        )
        Log.i(TAG, "$name: extract complete")
        assertTrue(
            "$name extracted no files",
            destination.walkTopDown().any { it.isFile }
        )
    }

    companion object {
        private const val TAG = "ZippestAudit"
    }
}
