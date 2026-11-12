package com.decompress.app

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.decompress.app.ui.UiStrings
import com.decompress.app.ui.queryOpenWithOptions
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 打开方式列表必须包含「分享」目标；旧实现查询 VIEW 时带 MATCH_DEFAULT_ONLY，
 * 且 SEND 没有 EXTRA_STREAM，导致列表偏少。
 */
@RunWith(AndroidJUnit4::class)
class OpenWithQueryTest {

    @Test
    fun returnsViewAndShareTargets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        UiStrings.install(context)
        val uri = Uri.parse("content://com.decompress.app.test/file.txt")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val options = queryOpenWithOptions(context, intent)
        assertTrue("至少应有一个直接打开目标", options.any { it.action == Intent.ACTION_VIEW })
        assertTrue(
            "应包含分享目标，实际 ${options.map { it.label + "/" + it.action }}",
            options.any { it.action == Intent.ACTION_SEND }
        )
    }
}
