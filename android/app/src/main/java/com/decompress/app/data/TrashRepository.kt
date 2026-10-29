package com.decompress.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class TrashItem(
    val id: String,
    val name: String,
    val originalPath: String,
    val deletedAt: Long,
    val size: Long,
    val isDirectory: Boolean
)

class TrashRepository(private val context: Context) {
    private val trashRoot: File by lazy {
        (context.getExternalFilesDir(null) ?: context.filesDir)
            .resolve("trash")
            .apply { mkdirs() }
    }

    suspend fun moveToTrash(source: File): Boolean = withContext(Dispatchers.IO) {
        if (!source.exists()) return@withContext false
        val id = UUID.randomUUID().toString()
        val itemDirectory = File(trashRoot, id)
        if (!itemDirectory.mkdirs()) return@withContext false
        val payload = File(itemDirectory, PAYLOAD_NAME)
        val metadata = JSONObject()
            .put("id", id)
            .put("name", source.name)
            .put("originalPath", source.absolutePath)
            .put("deletedAt", System.currentTimeMillis())
            .put("isDirectory", source.isDirectory)
            .put("size", if (source.isDirectory) directorySize(source) else source.length())
        val metadataFile = File(itemDirectory, METADATA_NAME)
        val moved = runCatching {
            metadataFile.writeText(metadata.toString(), Charsets.UTF_8)
            moveFile(source, payload)
        }.getOrDefault(false)
        if (!moved) {
            itemDirectory.deleteRecursively()
            return@withContext false
        }
        true
    }

    suspend fun list(): List<TrashItem> = withContext(Dispatchers.IO) {
        trashRoot.listFiles().orEmpty()
            .mapNotNull { readItem(it) }
            .sortedByDescending { it.deletedAt }
    }

    suspend fun restore(id: String): Boolean = withContext(Dispatchers.IO) {
        val itemDirectory = itemDirectory(id) ?: return@withContext false
        val item = readItem(itemDirectory) ?: return@withContext false
        val payload = File(itemDirectory, PAYLOAD_NAME)
        if (!payload.exists()) return@withContext false
        val original = File(item.originalPath)
        val parent = original.parentFile ?: return@withContext false
        if (!parent.isDirectory && !parent.mkdirs()) return@withContext false
        val target = uniqueFile(parent, original.name)
        val restored = moveFile(payload, target)
        if (restored) itemDirectory.deleteRecursively()
        restored
    }

    suspend fun deleteForever(id: String): Boolean = withContext(Dispatchers.IO) {
        itemDirectory(id)?.deleteRecursively() == true
    }

    suspend fun empty(): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        trashRoot.listFiles().orEmpty().forEach { item ->
            if (item.deleteRecursively()) deleted++
        }
        deleted
    }

    suspend fun purgeExpired(retentionDays: Int): Int = withContext(Dispatchers.IO) {
        if (retentionDays <= 0) return@withContext 0
        val cutoff = System.currentTimeMillis() - retentionDays * DAY_MILLIS
        var deleted = 0
        trashRoot.listFiles().orEmpty().forEach { directory ->
            val item = readItem(directory)
            if (item != null && item.deletedAt < cutoff && directory.deleteRecursively()) {
                deleted++
            }
        }
        deleted
    }

    private fun readItem(directory: File): TrashItem? = runCatching {
        val metadata = JSONObject(
            File(directory, METADATA_NAME).readText(Charsets.UTF_8)
        )
        TrashItem(
            id = metadata.getString("id"),
            name = metadata.getString("name"),
            originalPath = metadata.getString("originalPath"),
            deletedAt = metadata.getLong("deletedAt"),
            size = metadata.optLong("size", 0L),
            isDirectory = metadata.optBoolean("isDirectory", false)
        )
    }.getOrNull()

    private fun itemDirectory(id: String): File? = runCatching {
        val candidate = File(trashRoot, id)
        val rootPath = trashRoot.canonicalPath + File.separator
        if (candidate.canonicalPath.startsWith(rootPath) && candidate.isDirectory) {
            candidate
        } else {
            null
        }
    }.getOrNull()

    private fun moveFile(source: File, target: File): Boolean {
        if (source.renameTo(target)) return true
        return runCatching {
            if (source.isDirectory) {
                copyDirectory(source, target)
            } else {
                source.copyTo(target, overwrite = false)
                target.setLastModified(source.lastModified())
            }
            if (!source.deleteRecursively()) {
                target.deleteRecursively()
                false
            } else {
                true
            }
        }.getOrDefault(false)
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.mkdirs()) {
            throw IllegalStateException("无法创建 ${destination.name}")
        }
        source.listFiles().orEmpty().forEach { child ->
            val target = File(destination, child.name)
            if (child.isDirectory) {
                copyDirectory(child, target)
            } else {
                child.copyTo(target, overwrite = false)
                target.setLastModified(child.lastModified())
            }
        }
        destination.setLastModified(source.lastModified())
    }

    private fun uniqueFile(parent: File, name: String): File {
        val direct = File(parent, name)
        if (!direct.exists()) return direct
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val suffix = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (index < 10_000) {
            val candidate = File(parent, "$base ($index)$suffix")
            if (!candidate.exists()) return candidate
            index++
        }
        throw IllegalStateException("无法生成可恢复文件名")
    }

    private fun directorySize(directory: File): Long =
        directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }

    companion object {
        private const val METADATA_NAME = "metadata.json"
        private const val PAYLOAD_NAME = "payload"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
