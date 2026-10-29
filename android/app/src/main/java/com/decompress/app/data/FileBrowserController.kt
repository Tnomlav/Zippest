package com.decompress.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.io.File

data class TransferSummary(
    val completed: Int,
    val sourceRetained: Int,
    val failed: Int,
    val firstError: String?
)

class FileBrowserController(context: Context) {
    private val appContext = context.applicationContext
    private val repository = SafFileRepository(context)
    private val preferences =
        context.getSharedPreferences("decompress-browser", Context.MODE_PRIVATE)
    private val backStack = ArrayDeque<Pair<Uri, String>>()

    enum class StorageMode {
        SAF,
        DIRECT
    }

    var storageMode by mutableStateOf<StorageMode?>(null)
        private set
    var treeUri by mutableStateOf<Uri?>(null)
        private set
    var currentUri by mutableStateOf<Uri?>(null)
        private set
    var folderName by mutableStateOf("内部存储")
        private set
    var path by mutableStateOf<List<String>>(emptyList())
        private set
    var entries by mutableStateOf<List<BrowserEntry>>(emptyList())
        private set
    var selected by mutableStateOf<Set<Uri>>(emptySet())
        private set
    var selectionMode by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canGoUp: Boolean
        get() = backStack.isNotEmpty()

    suspend fun restore() {
        val savedMode = preferences.getString(KEY_STORAGE_MODE, null)
        if (savedMode == StorageMode.DIRECT.name && hasDirectStorageAccess()) {
            openInternalStorage()
            return
        }
        val saved = preferences.getString(KEY_TREE_URI, null) ?: return
        val uri = Uri.parse(saved)
        runCatching {
            repository.persistPermission(uri)
            storageMode = StorageMode.SAF
            treeUri = uri
            currentUri = repository.rootDocumentUri(uri)
            folderName = repository.displayName(currentUri!!) ?: "所选文件夹"
            path = listOf(folderName)
            loadCurrent()
        }.onFailure {
            error = "无法访问上次选择的文件夹，请重新授权"
            forgetRoot()
        }
    }

    suspend fun chooseRoot(uri: Uri) {
        repository.persistPermission(uri)
        storageMode = StorageMode.SAF
        treeUri = uri
        currentUri = repository.rootDocumentUri(uri)
        folderName = repository.displayName(currentUri!!) ?: "所选文件夹"
        path = listOf(folderName)
        backStack.clear()
        clearSelection()
        preferences.edit().putString(KEY_TREE_URI, uri.toString()).apply()
        preferences.edit().putString(KEY_STORAGE_MODE, StorageMode.SAF.name).apply()
        loadCurrent()
    }

    suspend fun openInternalStorage(): Boolean {
        if (!hasDirectStorageAccess()) return false
        val root = Environment.getExternalStorageDirectory()
        storageMode = StorageMode.DIRECT
        treeUri = Uri.fromFile(root)
        currentUri = Uri.fromFile(root)
        folderName = "内部存储"
        path = listOf(folderName)
        backStack.clear()
        clearSelection()
        preferences.edit()
            .putString(KEY_STORAGE_MODE, StorageMode.DIRECT.name)
            .putString(KEY_DIRECT_ROOT, root.absolutePath)
            .remove(KEY_TREE_URI)
            .apply()
        loadCurrent()
        return true
    }

    suspend fun openFixedLocation(label: String, directoryName: String?): Boolean {
        if (storageMode != StorageMode.DIRECT) return false
        val root = Environment.getExternalStorageDirectory()
        val target = directoryName?.let { File(root, it) } ?: root
        if (!target.isDirectory) return false

        backStack.clear()
        if (target != root) {
            backStack.addLast(Uri.fromFile(root) to "内部存储")
        }
        treeUri = Uri.fromFile(root)
        currentUri = Uri.fromFile(target)
        folderName = label
        path = if (target == root) listOf("内部存储") else listOf("内部存储", label)
        clearSelection()
        loadCurrent()
        return true
    }

    suspend fun open(entry: BrowserEntry) {
        if (!entry.isDirectory) return
        val current = currentUri ?: return
        backStack.addLast(current to folderName)
        currentUri = entry.uri
        folderName = entry.name
        path = path + entry.name
        clearSelection()
        loadCurrent()
    }

    suspend fun goUp(): Boolean {
        if (selectionMode) {
            clearSelection()
            return true
        }
        val previous = backStack.removeLastOrNull() ?: return false
        currentUri = previous.first
        folderName = previous.second
        path = if (path.size > 1) path.dropLast(1) else path
        clearSelection()
        loadCurrent()
        return true
    }

    suspend fun refresh() {
        loadCurrent()
    }

    private suspend fun loadCurrent() {
        val current = currentUri ?: return
        loading = true
        error = null
        runCatching {
            when (storageMode) {
                StorageMode.DIRECT -> {
                    val directory = current.localFile()
                        ?: throw IllegalStateException("直连目录不可用")
                    repository.listDirectChildren(directory)
                }
                StorageMode.SAF -> repository.listChildren(
                    treeUri ?: throw IllegalStateException("文件夹未授权"),
                    current
                )
                null -> emptyList()
            }
        }.onSuccess { loaded ->
            entries = loaded
            val visibleUris = loaded.mapTo(mutableSetOf()) { it.uri }
            selected = selected.filterTo(mutableSetOf()) { it in visibleUris }
        }.onFailure {
            error = it.message ?: "无法读取此文件夹"
        }
        loading = false
    }

    fun startSelection(uri: Uri) {
        selectionMode = true
        selected = selected + uri
    }

    fun toggleSelection(uri: Uri) {
        if (!selectionMode) selectionMode = true
        selected = if (uri in selected) selected - uri else selected + uri
    }

    fun setSelection(uris: Set<Uri>) {
        selectionMode = true
        selected = uris
    }

    fun deselectAll() {
        selected = emptySet()
    }

    fun clearSelection() {
        selected = emptySet()
        selectionMode = false
    }

    suspend fun createFolder(name: String): Boolean {
        val cleanName = validFileName(name) ?: return false
        val current = currentUri ?: return false
        val directDirectory = if (hasDirectStorageAccess()) {
            repository.localFileForUri(current)
        } else {
            null
        }
        val created = when {
            directDirectory != null ->
                repository.createDirectoryDirect(directDirectory, cleanName)
            storageMode == StorageMode.SAF ->
                repository.createDirectory(current, cleanName)
            else -> false
        }
        if (created) loadCurrent()
        return created
    }

    suspend fun renameSelected(name: String): Boolean {
        val selectedUri = selected.singleOrNull() ?: return false
        val cleanName = validFileName(name) ?: return false
        val directFile = if (hasDirectStorageAccess()) {
            repository.localFileForUri(selectedUri)
        } else {
            null
        }
        val renamed = when {
            directFile != null ->
                repository.renameDirect(Uri.fromFile(directFile), cleanName)
            storageMode == StorageMode.SAF ->
                repository.rename(selectedUri, cleanName)
            else -> false
        }
        if (renamed) {
            clearSelection()
            loadCurrent()
        }
        return renamed
    }

    suspend fun deleteSelected(): Int {
        val useTrash = AppSettingsRepository.read(appContext).useTrash
        val trash = TrashRepository(appContext)
        var deleted = 0
        selected.forEach { uri ->
            val directFile = if (hasDirectStorageAccess()) {
                repository.localFileForUri(uri)
            } else {
                null
            }
            val success = when {
                useTrash && directFile != null ->
                    trash.moveToTrash(directFile)
                useTrash -> false
                directFile != null ->
                    repository.deleteDirect(Uri.fromFile(directFile))
                storageMode == StorageMode.SAF ->
                    repository.delete(uri)
                else -> false
            }
            if (success) deleted++
        }
        clearSelection()
        loadCurrent()
        return deleted
    }

    suspend fun transferSelected(
        destinationTreeUri: Uri,
        move: Boolean
    ): TransferSummary {
        val sources = entries.filter { it.uri in selected }
        val destinationRoot = runCatching {
            repository.rootDocumentUri(destinationTreeUri)
        }.getOrNull()
        var completed = 0
        var sourceRetained = 0
        var failed = 0
        var firstError: String? = null
        for (source in sources) {
            if (
                move &&
                destinationRoot != null &&
                currentUri?.let { repository.isSameDirectory(it, destinationRoot) } == true
            ) {
                failed++
                firstError = firstError ?: "不能把文件移动到当前文件夹"
                continue
            }
            val result = when (storageMode) {
                StorageMode.DIRECT -> repository.copyDirectToSaf(
                    source.uri,
                    destinationTreeUri,
                    move
                )
                StorageMode.SAF -> {
                    val sourceTree = treeUri ?: break
                    repository.copyOrMove(
                        source,
                        sourceTree,
                        destinationTreeUri,
                        move
                    )
                }
                null -> TransferResult(TransferOutcome.FAILED, "存储位置不可用")
            }
            when (result.outcome) {
                TransferOutcome.COMPLETED -> completed++
                TransferOutcome.SOURCE_RETAINED -> sourceRetained++
                TransferOutcome.FAILED -> failed++
            }
            firstError = firstError ?: result.message
        }
        clearSelection()
        loadCurrent()
        return TransferSummary(completed, sourceRetained, failed, firstError)
    }

    suspend fun transferSelectedToDirectory(
        destinationDirectoryUri: Uri,
        destinationTreeUri: Uri,
        move: Boolean
    ): TransferSummary {
        val sources = entries.filter { it.uri in selected }
        if (
            move &&
            currentUri?.let {
                repository.isSameDirectory(it, destinationDirectoryUri)
            } == true
        ) {
            return TransferSummary(
                completed = 0,
                sourceRetained = 0,
                failed = sources.size,
                firstError = "不能把文件移动到当前文件夹"
            )
        }
        var completed = 0
        var sourceRetained = 0
        var failed = 0
        var firstError: String? = null
        for (source in sources) {
            val directSource = if (hasDirectStorageAccess()) {
                repository.localFileForUri(source.uri)
            } else {
                null
            }
            val directDestination = if (hasDirectStorageAccess()) {
                repository.localFileForUri(destinationDirectoryUri)
            } else {
                null
            }
            val result = when {
                directSource != null && directDestination != null ->
                    repository.copyOrMoveDirect(
                        sourceUri = Uri.fromFile(directSource),
                        destinationDirectoryUri = Uri.fromFile(directDestination),
                        move = move
                    )
                storageMode == StorageMode.SAF -> repository.copyOrMove(
                    source = source,
                    sourceTreeUri = treeUri ?: break,
                    destinationTreeUri = destinationTreeUri,
                    move = move,
                    destinationDirectoryUri = destinationDirectoryUri
                )
                else -> TransferResult(TransferOutcome.FAILED, "存储位置不可用")
            }
            when (result.outcome) {
                TransferOutcome.COMPLETED -> completed++
                TransferOutcome.SOURCE_RETAINED -> sourceRetained++
                TransferOutcome.FAILED -> failed++
            }
            firstError = firstError ?: result.message
        }
        clearSelection()
        loadCurrent()
        return TransferSummary(completed, sourceRetained, failed, firstError)
    }

    fun forgetRoot() {
        storageMode = null
        treeUri = null
        currentUri = null
        entries = emptyList()
        path = emptyList()
        backStack.clear()
        clearSelection()
        preferences.edit()
            .remove(KEY_TREE_URI)
            .remove(KEY_STORAGE_MODE)
            .remove(KEY_DIRECT_ROOT)
            .apply()
    }

    fun hasDirectStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                (
                    Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(
                            appContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    )
        }

    private fun validFileName(name: String): String? {
        val clean = name.trim()
        if (clean.isBlank() || clean == "." || clean == "..") return null
        if (clean.any { it == '/' || it == '\\' || it == '\u0000' }) return null
        return clean
    }

    companion object {
        private const val KEY_TREE_URI = "tree_uri"
        private const val KEY_STORAGE_MODE = "storage_mode"
        private const val KEY_DIRECT_ROOT = "direct_root"
    }
}
