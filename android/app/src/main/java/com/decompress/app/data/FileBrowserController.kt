package com.decompress.app.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FileBrowserController(context: Context) {
    private val repository = SafFileRepository(context)
    private val preferences =
        context.getSharedPreferences("decompress-browser", Context.MODE_PRIVATE)
    private val backStack = ArrayDeque<Pair<Uri, String>>()

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
        val saved = preferences.getString(KEY_TREE_URI, null) ?: return
        val uri = Uri.parse(saved)
        runCatching {
            repository.persistPermission(uri)
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
        treeUri = uri
        currentUri = repository.rootDocumentUri(uri)
        folderName = repository.displayName(currentUri!!) ?: "所选文件夹"
        path = listOf(folderName)
        backStack.clear()
        clearSelection()
        preferences.edit().putString(KEY_TREE_URI, uri.toString()).apply()
        loadCurrent()
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
        val tree = treeUri ?: return
        val current = currentUri ?: return
        loading = true
        error = null
        runCatching {
            repository.listChildren(tree, current)
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
        val current = currentUri ?: return false
        val created = repository.createDirectory(current, name)
        if (created) loadCurrent()
        return created
    }

    suspend fun deleteSelected(): Int {
        var deleted = 0
        selected.forEach { uri ->
            if (repository.delete(uri)) deleted++
        }
        clearSelection()
        loadCurrent()
        return deleted
    }

    suspend fun transferSelected(destinationTreeUri: Uri, move: Boolean): Int {
        val sourceTree = treeUri ?: return 0
        val sources = entries.filter { it.uri in selected }
        var transferred = 0
        for (source in sources) {
            if (repository.copyOrMove(source, sourceTree, destinationTreeUri, move)) {
                transferred++
            }
        }
        clearSelection()
        loadCurrent()
        return transferred
    }

    fun forgetRoot() {
        treeUri = null
        currentUri = null
        entries = emptyList()
        path = emptyList()
        backStack.clear()
        clearSelection()
        preferences.edit().remove(KEY_TREE_URI).apply()
    }

    companion object {
        private const val KEY_TREE_URI = "tree_uri"
    }
}
