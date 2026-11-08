package com.decompress.app.data

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地崩溃记录：只写应用私有目录，不上传、不发起网络请求。
 * 启动时读取并提示，用户可复制或关闭。
 */
object CrashLog {
    private const val FILE_NAME = "last-crash.txt"
    private const val MAX_CHARS = 60_000

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? =
        File(context.filesDir, FILE_NAME)
            .takeIf { it.isFile }
            ?.readText()
            ?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            appendLine("时间：$timestamp")
            appendLine("线程：${thread.name}")
            appendLine(error.toString())
            append(stack)
        }.take(MAX_CHARS)
        File(context.filesDir, FILE_NAME).writeText(text)
    }
}
