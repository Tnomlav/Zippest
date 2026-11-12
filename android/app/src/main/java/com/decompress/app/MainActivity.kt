package com.decompress.app

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.IntentCompat
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.service.TaskNavigation
import com.decompress.app.ui.ZippestApp

class MainActivity : ComponentActivity() {
    private val incomingArchive = mutableStateOf<IncomingArchive?>(null)
    private val openLocation = mutableStateOf<OpenLocationRequest?>(null)

    override fun attachBaseContext(newBase: Context) {
        val language = AppSettingsRepository.read(newBase).language
        val wrapped = com.decompress.app.data.LanguageContext.wrap(newBase, language)
        com.decompress.app.ui.UiStrings.install(wrapped)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.decompress.app.data.CrashLog.install(applicationContext)
        Thread { com.decompress.app.data.TempFiles.cleanStale(applicationContext) }.start()
        incomingArchive.value = readIncomingArchive(intent)
        openLocation.value = readOpenLocation(intent)
        enableEdgeToEdge()
        setContent {
            ZippestApp(
                incomingArchive = incomingArchive.value,
                openLocation = openLocation.value,
                onOpenLocationHandled = { openLocation.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingArchive.value = readIncomingArchive(intent)
        readOpenLocation(intent)?.let { openLocation.value = it }
    }

    private fun readOpenLocation(intent: Intent?): OpenLocationRequest? {
        if (intent?.action != TaskNavigation.ACTION_OPEN_LOCATION) return null
        val uri = intent.getStringExtra(TaskNavigation.EXTRA_LOCATION_URI)
            ?.let(Uri::parse) ?: return null
        return OpenLocationRequest(
            uri = uri,
            name = intent.getStringExtra(TaskNavigation.EXTRA_LOCATION_NAME).orEmpty(),
            treeUri = intent.getStringExtra(TaskNavigation.EXTRA_LOCATION_TREE_URI)
                ?.let(Uri::parse),
            path = intent.getStringExtra(TaskNavigation.EXTRA_LOCATION_PATH).orEmpty()
        )
    }

    private fun readIncomingArchive(intent: Intent?): IncomingArchive? {
        if (intent?.action == TaskNavigation.ACTION_OPEN_LOCATION) return null
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

/** 任务完成后要定位到的目录，来自通知 action 或 MainActivity 的重入 Intent。 */
data class OpenLocationRequest(
    val uri: Uri,
    val name: String,
    val treeUri: Uri?,
    val path: String
)
