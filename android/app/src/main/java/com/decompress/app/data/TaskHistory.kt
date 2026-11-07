package com.decompress.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.decompress.app.service.CompressionTask
import com.decompress.app.service.ExtractionTask
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 任务历史：最近 50 条解压 / 压缩记录，含失败。
 * 重试所需的参数（含密码）用 Android Keystore 加密后随记录保存。
 */
data class TaskHistoryEntry(
    val id: String,
    val kind: String,
    val title: String,
    val message: String,
    val success: Boolean,
    val finishedAt: Long,
    val locationUri: String?,
    val locationName: String,
    val treeUri: String?,
    val path: String,
    val retryPayload: String?
)

class TaskHistoryStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("task-history", Context.MODE_PRIVATE)

    fun list(): List<TaskHistoryEntry> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                TaskHistoryEntry(
                    id = item.getString("id"),
                    kind = item.getString("kind"),
                    title = item.getString("title"),
                    message = item.optString("message"),
                    success = item.optBoolean("success"),
                    finishedAt = item.optLong("finishedAt"),
                    locationUri = item.optString("locationUri").takeIf { it.isNotBlank() },
                    locationName = item.optString("locationName"),
                    treeUri = item.optString("treeUri").takeIf { it.isNotBlank() },
                    path = item.optString("path"),
                    retryPayload = item.optString("retryPayload").takeIf { it.isNotBlank() }
                )
            }
        }.getOrDefault(emptyList())
    }

    fun record(entry: TaskHistoryEntry) {
        val merged = (listOf(entry) + list().filterNot { it.id == entry.id })
            .take(MAX_ENTRIES)
        save(merged)
    }

    fun delete(id: String) {
        save(list().filterNot { it.id == id })
    }

    private fun save(entries: List<TaskHistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("kind", entry.kind)
                    put("title", entry.title)
                    put("message", entry.message)
                    put("success", entry.success)
                    put("finishedAt", entry.finishedAt)
                    put("locationUri", entry.locationUri.orEmpty())
                    put("locationName", entry.locationName)
                    put("treeUri", entry.treeUri.orEmpty())
                    put("path", entry.path)
                    put("retryPayload", entry.retryPayload.orEmpty())
                }
            )
        }
        prefs.edit().putString(KEY_LIST, array.toString()).apply()
    }

    companion object {
        private const val KEY_LIST = "entries"
        const val MAX_ENTRIES = 50
    }
}

/** 把解压任务序列化成可重试的 JSON；密码整体加密。 */
object TaskHistoryCodec {
    fun encodeExtraction(task: ExtractionTask): String {
        val json = JSONObject().apply {
            put("kind", "extract")
            put("archiveUris", JSONArray(task.archiveUris.map(Uri::toString)))
            put("archiveNames", JSONArray(task.archiveNames))
            put("parentUri", task.parentUri?.toString().orEmpty())
            put("parentTreeUri", task.parentTreeUri?.toString().orEmpty())
            put("mode", task.mode.name)
            put("customTreeUri", task.customTreeUri?.toString().orEmpty())
            put("customRootUri", task.customRootUri?.toString().orEmpty())
            put("createFolder", task.createFolder)
            put("conflict", task.conflictPolicy.name)
            put("encoding", task.encoding.name)
            put("parentPath", task.parentPath)
            put("parentName", task.parentName)
            put("entryPaths", JSONArray(task.entryPaths))
            put("passwords", TaskSecretBox.encrypt(task.passwords.joinToString("\u0000")))
        }
        return TaskSecretBox.encrypt(json.toString())
    }

    fun decodeExtraction(payload: String): ExtractionTask? = runCatching {
        val json = JSONObject(TaskSecretBox.decrypt(payload))
        val passwords = TaskSecretBox.decrypt(json.getString("passwords"))
            .split("\u0000")
            .let { if (it.size == 1 && it[0].isEmpty()) emptyList() else it }
        ExtractionTask(
            archiveUris = json.getJSONArray("archiveUris").toStringList().map(Uri::parse),
            archiveNames = json.getJSONArray("archiveNames").toStringList(),
            parentUri = json.optString("parentUri").takeIf { it.isNotBlank() }?.let(Uri::parse),
            parentTreeUri = json.optString("parentTreeUri").takeIf { it.isNotBlank() }?.let(Uri::parse),
            mode = DestinationMode.valueOf(json.getString("mode")),
            customTreeUri = json.optString("customTreeUri").takeIf { it.isNotBlank() }?.let(Uri::parse),
            customRootUri = json.optString("customRootUri").takeIf { it.isNotBlank() }?.let(Uri::parse),
            createFolder = json.optBoolean("createFolder"),
            conflictPolicy = ConflictPolicy.valueOf(json.getString("conflict")),
            encoding = FileNameEncoding.valueOf(json.getString("encoding")),
            passwords = passwords,
            parentPath = json.optString("parentPath"),
            parentName = json.optString("parentName"),
            entryPaths = json.optJSONArray("entryPaths")?.toStringList().orEmpty()
        )
    }.getOrNull()

    fun encodeCompression(task: CompressionTask): String {
        val sources = JSONArray()
        task.sources.forEach { entry ->
            sources.put(
                JSONObject().apply {
                    put("uri", entry.uri.toString())
                    put("name", entry.name)
                    put("isDirectory", entry.isDirectory)
                    put("size", entry.size)
                    put("lastModified", entry.lastModified)
                    put("mimeType", entry.mimeType.orEmpty())
                }
            )
        }
        val json = JSONObject().apply {
            put("kind", "compress")
            put("sources", sources)
            put("sourceTreeUri", task.sourceTreeUri.toString())
            put("targetParentUri", task.targetParentUri.toString())
            put("targetTreeUri", task.targetTreeUri.toString())
            put("format", task.options.format.name)
            put("archiveName", task.options.archiveName)
            put("level", task.options.level.name)
            put("splitSize", task.options.splitSizeBytes ?: -1L)
            put("password", TaskSecretBox.encrypt(task.options.password))
        }
        return TaskSecretBox.encrypt(json.toString())
    }

    fun decodeCompression(payload: String): CompressionTask? = runCatching {
        val json = JSONObject(TaskSecretBox.decrypt(payload))
        val sources = json.getJSONArray("sources")
        val entries = (0 until sources.length()).map { index ->
            val item = sources.getJSONObject(index)
            BrowserEntry(
                uri = Uri.parse(item.getString("uri")),
                name = item.getString("name"),
                isDirectory = item.optBoolean("isDirectory"),
                size = item.optLong("size"),
                lastModified = item.optLong("lastModified"),
                mimeType = item.optString("mimeType").takeIf { it.isNotBlank() }
            )
        }
        val split = json.optLong("splitSize", -1L).takeIf { it > 0 }
        CompressionTask(
            sources = entries,
            sourceTreeUri = Uri.parse(json.getString("sourceTreeUri")),
            targetParentUri = Uri.parse(json.getString("targetParentUri")),
            targetTreeUri = Uri.parse(json.getString("targetTreeUri")),
            options = CompressionOptions(
                format = CompressionFormat.valueOf(json.getString("format")),
                archiveName = json.getString("archiveName"),
                level = ArchiveCompressionLevel.valueOf(json.getString("level")),
                password = TaskSecretBox.decrypt(json.optString("password")),
                splitSizeBytes = split
            )
        )
    }.getOrNull()

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { optString(it) }
}

private object TaskSecretBox {
    private const val ALIAS = "zippest-task-history"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(payload: String): String {
        if (payload.isEmpty()) return ""
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(),
            GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES))
        )
        return String(
            cipher.doFinal(bytes.copyOfRange(IV_BYTES, bytes.size)),
            Charsets.UTF_8
        )
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()
        )
        return generator.generateKey()
    }
}
