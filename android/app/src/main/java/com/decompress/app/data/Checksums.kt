package com.decompress.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** 本地计算 SHA-256 与 MD5，不上传任何数据。 */
object Checksums {
    suspend fun compute(context: Context, uri: Uri): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val sha = MessageDigest.getInstance("SHA-256")
                val md5 = MessageDigest.getInstance("MD5")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        sha.update(buffer, 0, read)
                        md5.update(buffer, 0, read)
                    }
                } ?: return@runCatching null
                sha.digest().toHex() to md5.digest().toHex()
            }.getOrNull()
        }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
