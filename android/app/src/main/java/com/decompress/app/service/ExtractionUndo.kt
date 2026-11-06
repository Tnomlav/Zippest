package com.decompress.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Toast
import com.decompress.app.data.localFile

/**
 * 「撤销解压」：只记住本次任务新建的文件，点一下全部删掉，回到解压前的状态。
 *
 * 只保留最近一次成功任务，最多 2000 个条目（再多就不提供撤销，避免占用内存）。
 */
object ExtractionUndoStore {
    private const val PREFERENCES = "extraction-undo"
    private const val KEY_URIS = "uris"
    private const val KEY_TASK = "task"
    const val MAX_ENTRIES = 2000

    fun save(context: Context, taskId: String, uris: List<Uri>) {
        if (uris.isEmpty() || uris.size > MAX_ENTRIES) {
            clear(context)
            return
        }
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URIS, uris.joinToString("\n") { it.toString() })
            .putString(KEY_TASK, taskId)
            .apply()
    }

    fun load(context: Context): List<Uri> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_URIS, null)
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
            .toList()
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

class ExtractionUndoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UNDO_EXTRACTION) return
        val uris = ExtractionUndoStore.load(context)
        if (uris.isEmpty()) return
        val resolver = context.contentResolver
        var deleted = 0
        uris.forEach { uri ->
            val ok = runCatching {
                val file = uri.localFile()
                if (file != null) {
                    file.delete()
                } else {
                    DocumentsContract.deleteDocument(resolver, uri)
                }
            }.getOrDefault(false)
            if (ok) deleted++
        }
        ExtractionUndoStore.clear(context)
        val message = if (deleted == uris.size) {
            "已撤销，删除了 $deleted 个文件"
        } else {
            "已撤销，删除 $deleted/${uris.size} 个文件"
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_UNDO_EXTRACTION = "com.decompress.app.action.UNDO_EXTRACTION"

        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ExtractionUndoReceiver::class.java).apply {
                action = ACTION_UNDO_EXTRACTION
                setPackage(context.packageName)
            }
            return PendingIntent.getBroadcast(
                context.applicationContext,
                0x5151,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
