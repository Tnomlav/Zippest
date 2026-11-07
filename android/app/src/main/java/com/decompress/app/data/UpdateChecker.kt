package com.decompress.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val releaseUrl: String,
    val sizeBytes: Long
)

/**
 * 只在你点「版本号」时访问 GitHub Releases API；不轮询、不上传任何信息。
 */
object UpdateChecker {
    private const val API_URL = "https://api.github.com/repos/Tnomlav/Zippest/releases/latest"

    suspend fun check(currentVersion: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Zippest-Android")
            }
            try {
                if (connection.responseCode != 200) {
                    throw IllegalStateException("GitHub 返回 ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name").trim()
                val releaseUrl = json.optString("html_url")
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                var size = 0L
                if (assets != null) {
                    val candidates = (0 until assets.length()).map { assets.getJSONObject(it) }
                    val picked = candidates.firstOrNull {
                        it.optString("name").contains("arm64-v8a")
                    } ?: candidates.firstOrNull {
                        it.optString("name").contains("universal")
                    } ?: candidates.firstOrNull {
                        it.optString("name").endsWith(".apk")
                    }
                    if (picked != null) {
                        apkUrl = picked.optString("browser_download_url")
                        size = picked.optLong("size")
                    }
                }
                val latest = tag.removePrefix("v")
                if (latest.isBlank() || apkUrl.isNullOrBlank()) return@runCatching null
                if (!isNewer(latest, currentVersion)) return@runCatching null
                UpdateInfo(latest, apkUrl, releaseUrl, size)
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun download(context: Context, info: UpdateInfo): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "Zippest-${info.version}.apk")
            if (target.exists() && target.length() > 0) return@withContext target
            val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", "Zippest-Android")
                instanceFollowRedirects = true
            }
            try {
                if (connection.responseCode != 200) {
                    throw IllegalStateException("下载失败：HTTP ${connection.responseCode}")
                }
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output, 64 * 1024)
                    }
                }
            } finally {
                connection.disconnect()
            }
            target
        }

    fun install(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    internal fun isNewer(latest: String, current: String): Boolean {
        val left = versionParts(latest)
        val right = versionParts(current)
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun versionParts(value: String): List<Int> =
        value.removePrefix("v")
            .substringBefore('-')
            .split('.')
            .mapNotNull { it.toIntOrNull() }
}
