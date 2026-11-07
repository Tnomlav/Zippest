package com.decompress.app.data

import android.content.Context

/** 清理 cacheDir 里过期的临时文件，避免压缩/解压中断后长期占用空间。 */
object TempFiles {
    private val MAX_AGE_MS = 24L * 60 * 60 * 1000

    fun cleanStale(context: Context, maxAgeMs: Long = MAX_AGE_MS) {
        val now = System.currentTimeMillis()
        context.cacheDir.listFiles()?.forEach { file ->
            val name = file.name
            if (!name.startsWith("zippest-")) return@forEach
            if (now - file.lastModified() <= maxAgeMs) return@forEach
            runCatching {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
        }
    }
}
