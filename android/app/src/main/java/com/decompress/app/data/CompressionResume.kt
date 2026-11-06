package com.decompress.app.data

import android.content.Context
import org.json.JSONObject

/**
 * 压缩任务的断点现场。
 *
 * 只记录“写到哪一个条目、文件多大”这类可校验的事实，不记录任何用户内容；
 * 密码仍然只存在于 WorkManager 的加密输入里，不会落到这里。
 */
data class CompressionResumeState(
    val format: String,
    val archiveName: String,
    val stagingName: String,
    val entryCount: Int,
    val lastEntryName: String,
    val offset: Long,
    val signature: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun encode(): String = JSONObject().apply {
        put("version", VERSION)
        put("format", format)
        put("archiveName", archiveName)
        put("stagingName", stagingName)
        put("entryCount", entryCount)
        put("lastEntryName", lastEntryName)
        put("offset", offset)
        put("signature", signature)
        put("updatedAt", updatedAt)
    }.toString()

    companion object {
        const val VERSION = 1

        fun decode(raw: String?): CompressionResumeState? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(raw)
                if (json.optInt("version", 0) != VERSION) return null
                CompressionResumeState(
                    format = json.getString("format"),
                    archiveName = json.getString("archiveName"),
                    stagingName = json.getString("stagingName"),
                    entryCount = json.getInt("entryCount"),
                    lastEntryName = json.getString("lastEntryName"),
                    offset = json.getLong("offset"),
                    signature = json.getString("signature"),
                    updatedAt = json.optLong("updatedAt", 0L)
                )
            }.getOrNull()
        }
    }
}

/** 断点现场存在 SharedPreferences 里，进程被杀也不会丢。 */
class CompressionResumeStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(taskId: String): CompressionResumeState? =
        CompressionResumeState.decode(prefs.getString(taskId, null))

    fun save(taskId: String, state: CompressionResumeState) {
        prefs.edit().putString(taskId, state.encode()).apply()
    }

    fun clear(taskId: String) {
        prefs.edit().remove(taskId).apply()
    }

    /** 启动时清掉过期现场，避免用户目录里长期存在半成品。 */
    fun prune(maxAgeMillis: Long, stagingDirs: List<java.io.File>): List<java.io.File> {
        val removed = mutableListOf<java.io.File>()
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        prefs.all.forEach { (key, value) ->
            val state = CompressionResumeState.decode(value as? String)
            if (state == null || now - state.updatedAt > maxAgeMillis) {
                editor.remove(key)
                stagingDirs.forEach { dir ->
                    val candidate = java.io.File(dir, state?.stagingName ?: "")
                    if (state != null && candidate.isFile) {
                        candidate.delete()
                        removed += candidate
                    }
                }
            }
        }
        editor.apply()
        return removed
    }

    private companion object {
        const val PREFERENCES = "compression-resume"
    }
}

/**
 * 只判断“是不是同一批源文件”的廉价指纹：条目数、总大小、时间戳之和、首尾文件名。
 * 精确到字节的校验交给压缩过程本身（条目名对不上就放弃续传）。
 */
fun compressionSignature(entries: List<BrowserEntry>): String {
    var totalSize = 0L
    var totalModified = 0L
    entries.forEach {
        totalSize += it.size
        totalModified += it.lastModified
    }
    val head = entries.firstOrNull()?.name.orEmpty()
    val tail = entries.lastOrNull()?.name.orEmpty()
    return "${entries.size}:$totalSize:$totalModified:$head:$tail"
}
