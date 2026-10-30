package com.decompress.app.ui

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.decompress.app.data.BrowserEntry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OpenWithIntentDeviceTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        allowAllFilesAccess(instrumentation)
        root = File(
            Environment.getExternalStorageDirectory(),
            "Download/ZippestOpenWith-${System.nanoTime()}"
        ).apply {
            assertTrue(mkdirs())
        }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun imageIntentCarriesSiblingMediaAndWritePermission() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = createImage("first.png")
        val second = createImage("second.png")
        val latch = CountDownLatch(1)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(first.absolutePath, second.absolutePath),
            arrayOf("image/png", "image/png")
        ) { _, _ -> latch.countDown() }
        assertTrue(latch.await(10, TimeUnit.SECONDS))

        val intent = createOpenWithIntent(
            context,
            BrowserEntry(
                uri = Uri.fromFile(first),
                name = first.name,
                isDirectory = false,
                size = first.length(),
                lastModified = first.lastModified(),
                mimeType = "image/png"
            )
        )
        requireNotNull(intent)
        assertEquals(MediaStore.AUTHORITY, intent.data?.authority)
        assertTrue((intent.clipData?.itemCount ?: 0) >= 2)
        assertTrue(
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
        assertTrue(
            intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0
        )
    }

    private fun createImage(name: String): File {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        return File(root, name).apply {
            outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    private fun allowAllFilesAccess(instrumentation: Instrumentation) {
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.executeShellCommand(
            "appops set ${context.packageName} MANAGE_EXTERNAL_STORAGE allow"
        ).close()
    }
}
