package com.decompress.app

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.IntentCompat
import com.decompress.app.ui.ZippestApp

class MainActivity : ComponentActivity() {
    private val incomingArchive = mutableStateOf<IncomingArchive?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingArchive.value = readIncomingArchive(intent)
        enableEdgeToEdge()
        setContent {
            ZippestApp(incomingArchive = incomingArchive.value)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingArchive.value = readIncomingArchive(intent)
    }

    private fun readIncomingArchive(intent: Intent?): IncomingArchive? {
        val uri = when (intent?.action) {
            Intent.ACTION_SEND -> intent?.let { sent ->
                IntentCompat.getParcelableExtra(
                    sent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )
            }
            else -> intent?.data
        } ?: return null
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return IncomingArchive(uri, null)

        val hasReadGrant =
            (intent?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        if (hasReadGrant) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        var displayName: String? = null
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0)
                }
            }
        }
        return IncomingArchive(uri, displayName)
    }
}

data class IncomingArchive(
    val uri: Uri,
    val displayName: String?
)
