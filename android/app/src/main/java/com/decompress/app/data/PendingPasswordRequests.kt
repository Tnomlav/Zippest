package com.decompress.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 后台解压遇到「密码和前面几个包不一样」时，把需要补密码的压缩包记下来；
 * 应用回到前台后弹窗补填，再单独重新入队。只在本机保存，不联网。
 */
data class PendingPasswordRequest(
    val id: String,
    val archiveUri: String,
    val archiveName: String,
    val parentUri: String?,
    val parentTreeUri: String?,
    val parentName: String,
    val parentPath: String,
    val mode: DestinationMode,
    val customTreeUri: String?,
    val customRootUri: String?,
    val createFolder: Boolean,
    val conflictPolicy: ConflictPolicy,
    val encoding: FileNameEncoding,
    val entryPaths: List<String>
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("archiveUri", archiveUri)
        .put("archiveName", archiveName)
        .put("parentUri", parentUri)
        .put("parentTreeUri", parentTreeUri)
        .put("parentName", parentName)
        .put("parentPath", parentPath)
        .put("mode", mode.name)
        .put("customTreeUri", customTreeUri)
        .put("customRootUri", customRootUri)
        .put("createFolder", createFolder)
        .put("conflictPolicy", conflictPolicy.name)
        .put("encoding", encoding.name)
        .put("entryPaths", JSONArray(entryPaths))

    companion object {
        fun fromJson(json: JSONObject): PendingPasswordRequest = PendingPasswordRequest(
            id = json.getString("id"),
            archiveUri = json.getString("archiveUri"),
            archiveName = json.getString("archiveName"),
            parentUri = json.optString("parentUri").ifBlank { null },
            parentTreeUri = json.optString("parentTreeUri").ifBlank { null },
            parentName = json.optString("parentName"),
            parentPath = json.optString("parentPath"),
            mode = runCatching {
                DestinationMode.valueOf(json.getString("mode"))
            }.getOrDefault(DestinationMode.CURRENT_FOLDER),
            customTreeUri = json.optString("customTreeUri").ifBlank { null },
            customRootUri = json.optString("customRootUri").ifBlank { null },
            createFolder = json.optBoolean("createFolder", true),
            conflictPolicy = runCatching {
                ConflictPolicy.valueOf(json.getString("conflictPolicy"))
            }.getOrDefault(ConflictPolicy.RENAME),
            encoding = runCatching {
                FileNameEncoding.valueOf(json.getString("encoding"))
            }.getOrDefault(FileNameEncoding.AUTO),
            entryPaths = json.optJSONArray("entryPaths")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            }.orEmpty()
        )

        fun create(
            archiveUri: String,
            archiveName: String,
            parentUri: String?,
            parentTreeUri: String?,
            parentName: String,
            parentPath: String,
            mode: DestinationMode,
            customTreeUri: String?,
            customRootUri: String?,
            createFolder: Boolean,
            conflictPolicy: ConflictPolicy,
            encoding: FileNameEncoding,
            entryPaths: List<String>
        ): PendingPasswordRequest = PendingPasswordRequest(
            id = UUID.randomUUID().toString(),
            archiveUri = archiveUri,
            archiveName = archiveName,
            parentUri = parentUri,
            parentTreeUri = parentTreeUri,
            parentName = parentName,
            parentPath = parentPath,
            mode = mode,
            customTreeUri = customTreeUri,
            customRootUri = customRootUri,
            createFolder = createFolder,
            conflictPolicy = conflictPolicy,
            encoding = encoding,
            entryPaths = entryPaths
        )
    }
}

object PendingPasswordStore {
    private const val PREFS = "pending_password_requests"
    private const val KEY = "requests"

    @Synchronized
    fun add(context: Context, requests: List<PendingPasswordRequest>) {
        if (requests.isEmpty()) return
        val existing = all(context)
        val merged = (existing + requests).distinctBy { it.archiveUri }
        write(context, merged)
    }

    @Synchronized
    fun all(context: Context): List<PendingPasswordRequest> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                runCatching {
                    PendingPasswordRequest.fromJson(array.getJSONObject(index))
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun remove(context: Context, id: String) {
        write(context, all(context).filterNot { it.id == id })
    }

    @Synchronized
    fun clear(context: Context) {
        write(context, emptyList())
    }

    private fun write(context: Context, requests: List<PendingPasswordRequest>) {
        val array = JSONArray()
        requests.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
