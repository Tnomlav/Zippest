package com.decompress.app.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream

enum class TransferOutcome {
    COMPLETED,
    SOURCE_RETAINED,
    FAILED
}

data class TransferResult(
    val outcome: TransferOutcome,
    val message: String? = null
)

class SafFileRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun persistPermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            resolver.takePersistableUriPermission(treeUri, flags)
        }
    }

    fun rootDocumentUri(treeUri: Uri): Uri {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
    }

    suspend fun displayName(documentUri: Uri): String? = withContext(Dispatchers.IO) {
        resolver.query(
            documentUri,
            arrayOf(Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    suspend fun listChildren(
        treeUri: Uri,
        directoryUri: Uri
    ): List<BrowserEntry> = withContext(Dispatchers.IO) {
        val directoryId = DocumentsContract.getDocumentId(directoryUri)
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId)
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED
        )
        val result = mutableListOf<BrowserEntry>()

        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndex(Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idIndex)
                val mimeType = cursor.getString(mimeIndex) ?: ""
                val isDirectory = mimeType == Document.MIME_TYPE_DIR
                result += BrowserEntry(
                    uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                    name = cursor.getString(nameIndex) ?: "未命名",
                    isDirectory = isDirectory,
                    size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        0L
                    },
                    lastModified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                        cursor.getLong(modifiedIndex)
                    } else {
                        0L
                    },
                    mimeType = mimeType
                )
            }
        }

        result.sortedWith(
            compareByDescending<BrowserEntry> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    suspend fun createDirectory(parentUri: Uri, name: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                DocumentsContract.createDocument(
                    resolver,
                    parentUri,
                    Document.MIME_TYPE_DIR,
                    name
                ) != null
            }.getOrDefault(false)
        }

    suspend fun delete(documentUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            DocumentsContract.deleteDocument(resolver, documentUri)
        }.getOrDefault(false)
    }

    suspend fun rename(documentUri: Uri, name: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                DocumentsContract.renameDocument(resolver, documentUri, name) != null
            }.getOrDefault(false)
        }

    suspend fun listDirectChildren(directory: File): List<BrowserEntry> =
        withContext(Dispatchers.IO) {
            if (!directory.isDirectory) {
                throw IllegalStateException("无法读取文件夹：${directory.name}")
            }
            directory.listFiles()
                .orEmpty()
                .map { file -> directEntry(file) }
                .sortedWith(
                    compareByDescending<BrowserEntry> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )
        }

    fun directEntry(file: File): BrowserEntry {
        val extension = file.extension.lowercase()
        val mimeType = when {
            file.isDirectory -> Document.MIME_TYPE_DIR
            else -> MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"
        }
        return BrowserEntry(
            uri = Uri.fromFile(file),
            name = file.name,
            isDirectory = file.isDirectory,
            size = if (file.isFile) file.length() else 0L,
            lastModified = file.lastModified(),
            mimeType = mimeType
        )
    }

    suspend fun createDirectoryDirect(parent: File, name: String): Boolean =
        withContext(Dispatchers.IO) {
            File(parent, name).mkdirs()
        }

    suspend fun deleteDirect(documentUri: Uri): Boolean = withContext(Dispatchers.IO) {
        documentUri.localFile()?.deleteRecursively() == true
    }

    suspend fun renameDirect(documentUri: Uri, name: String): Boolean =
        withContext(Dispatchers.IO) {
            val source = documentUri.localFile() ?: return@withContext false
            if (source.name == name) return@withContext true
            val target = File(source.parentFile ?: return@withContext false, name)
            if (target.exists()) return@withContext false
            source.renameTo(target)
        }

    fun isSameDirectory(first: Uri, second: Uri): Boolean {
        if (first == second) return true
        if (first.authority == second.authority) {
            val firstId = runCatching {
                DocumentsContract.getDocumentId(first)
            }.getOrNull()
            val secondId = runCatching {
                DocumentsContract.getDocumentId(second)
            }.getOrNull()
            if (firstId != null && firstId == secondId) return true
        }
        val firstDirectory = localDirectory(first) ?: return false
        val secondDirectory = localDirectory(second) ?: return false
        return runCatching {
            firstDirectory.canonicalFile == secondDirectory.canonicalFile
        }.getOrDefault(false)
    }

    suspend fun copyDirectToSaf(
        sourceUri: Uri,
        destinationTreeUri: Uri,
        move: Boolean
    ): TransferResult = withContext(Dispatchers.IO) {
        val source = sourceUri.localFile()
            ?: return@withContext TransferResult(
                TransferOutcome.FAILED,
                "源文件不可用"
            )
        var createdDestination: Uri? = null
        try {
            val destinationRoot = rootDocumentUri(destinationTreeUri)
            createdDestination = transferDirectToSaf(
                source = source,
                destinationParent = destinationRoot,
                destinationTreeUri = destinationTreeUri
            )
        } catch (error: Exception) {
            createdDestination?.let { uri ->
                runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            }
            TransferResult(
                TransferOutcome.FAILED,
                error.message ?: "复制失败"
            )
        }
        if (move) {
            val deleted = runCatching { source.deleteRecursively() }.getOrDefault(false)
            if (!deleted) {
                return@withContext TransferResult(
                    TransferOutcome.SOURCE_RETAINED,
                    "已复制，但无法删除源文件"
                )
            }
        }
        TransferResult(TransferOutcome.COMPLETED)
    }

    private suspend fun transferDirectToSaf(
        source: File,
        destinationParent: Uri,
        destinationTreeUri: Uri
    ): Uri {
        val destination = createUniqueDocument(
            parentUri = destinationParent,
            treeUri = destinationTreeUri,
            name = source.name,
            mimeType = if (source.isDirectory) {
                Document.MIME_TYPE_DIR
            } else {
                directEntry(source).mimeType ?: "application/octet-stream"
            }
        ) ?: throw IllegalStateException("无法在目标文件夹创建 ${source.name}")

        if (source.isDirectory) {
            source.listFiles().orEmpty().forEach { child ->
                transferDirectToSaf(
                    source = child,
                    destinationParent = destination,
                    destinationTreeUri = destinationTreeUri
                )
            }
        } else {
            val input = FileInputStream(source)
            BufferedInputStream(input).use { bufferedInput ->
                val output = resolver.openOutputStream(destination)
                    ?: throw IllegalStateException("无法写入 ${source.name}")
                BufferedOutputStream(output).use { bufferedOutput ->
                    bufferedInput.copyTo(bufferedOutput, BUFFER_SIZE)
                }
            }
        }
        return destination
    }

    suspend fun copyOrMove(
        source: BrowserEntry,
        sourceTreeUri: Uri,
        destinationTreeUri: Uri,
        move: Boolean
    ): TransferResult = withContext(Dispatchers.IO) {
        var createdDestination: Uri? = null
        try {
            val destinationRoot = rootDocumentUri(destinationTreeUri)
            if (source.isDirectory && isPotentialSelfCopy(source, destinationRoot)) {
                throw IllegalArgumentException("不能把文件夹移动到它自身内部")
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                source.isDirectory &&
                source.uri != destinationRoot &&
                runCatching {
                    DocumentsContract.isChildDocument(
                        resolver,
                        source.uri,
                        destinationRoot
                    )
                }.getOrDefault(false)
            ) {
                throw IllegalArgumentException("不能把文件夹移动到它自身内部")
            }
            createdDestination = transferRecursive(
                source = source,
                sourceTreeUri = sourceTreeUri,
                destinationParent = destinationRoot,
                destinationTreeUri = destinationTreeUri
            )
        } catch (error: Exception) {
            createdDestination?.let { uri ->
                runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            }
            TransferResult(
                TransferOutcome.FAILED,
                error.message ?: "复制失败"
            )
        }
        if (move) {
            val deleted = runCatching {
                DocumentsContract.deleteDocument(resolver, source.uri)
            }.getOrDefault(false)
            if (!deleted) {
                return@withContext TransferResult(
                    TransferOutcome.SOURCE_RETAINED,
                    "已复制，但无法删除源文件"
                )
            }
        }
        TransferResult(TransferOutcome.COMPLETED)
    }

    private fun isPotentialSelfCopy(source: BrowserEntry, destinationRoot: Uri): Boolean {
        if (source.uri.authority != destinationRoot.authority) return false
        return runCatching {
            val sourceId = DocumentsContract.getDocumentId(source.uri)
            val destinationId = DocumentsContract.getDocumentId(destinationRoot)
            destinationId == sourceId || destinationId.startsWith("$sourceId/")
        }.getOrDefault(false)
    }

    private suspend fun transferRecursive(
        source: BrowserEntry,
        sourceTreeUri: Uri,
        destinationParent: Uri,
        destinationTreeUri: Uri
    ): Uri {
        val destination = createUniqueDocument(
            parentUri = destinationParent,
            treeUri = destinationTreeUri,
            name = source.name,
            mimeType = if (source.isDirectory) {
                Document.MIME_TYPE_DIR
            } else {
                source.mimeType ?: "application/octet-stream"
            }
        ) ?: throw IllegalStateException("无法在目标文件夹创建 ${source.name}")

        if (source.isDirectory) {
            listChildren(sourceTreeUri, source.uri).forEach { child ->
                transferRecursive(
                    source = child,
                    sourceTreeUri = sourceTreeUri,
                    destinationParent = destination,
                    destinationTreeUri = destinationTreeUri
                )
            }
        } else {
            val input = resolver.openInputStream(source.uri)
                ?: throw IllegalStateException("无法读取 ${source.name}")
            BufferedInputStream(input).use { bufferedInput ->
                val output = resolver.openOutputStream(destination)
                    ?: throw IllegalStateException("无法写入 ${source.name}")
                BufferedOutputStream(output).use { bufferedOutput ->
                    bufferedInput.copyTo(bufferedOutput, BUFFER_SIZE)
                }
            }
        }
        return destination
    }

    private fun createUniqueDocument(
        parentUri: Uri,
        treeUri: Uri,
        name: String,
        mimeType: String
    ): Uri? {
        val names = childNames(parentUri, treeUri)
        var candidate = name
        if (candidate in names) {
            val dot = name.lastIndexOf('.')
            val base = if (dot > 0) name.substring(0, dot) else name
            val suffix = if (dot > 0) name.substring(dot) else ""
            var index = 1
            while ("$base ($index)$suffix" in names) index++
            candidate = "$base ($index)$suffix"
        }
        return DocumentsContract.createDocument(
            resolver,
            parentUri,
            mimeType,
            candidate
        )
    }

    private fun childNames(parentUri: Uri, treeUri: Uri): Set<String> {
        val parentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val names = mutableSetOf<String>()
        resolver.query(
            childrenUri,
            arrayOf(Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                cursor.getString(nameIndex)?.let(names::add)
            }
        }
        return names
    }

    private fun localDirectory(uri: Uri): File? {
        uri.localFile()?.let { return it }
        if (uri.authority != "com.android.externalstorage.documents") return null
        val documentId = runCatching {
            DocumentsContract.getDocumentId(uri)
        }.getOrNull() ?: return null
        val volume = documentId.substringBefore(':', "")
        val relativePath = documentId.substringAfter(':', "")
        if (volume.isBlank()) return null
        val volumeRoot = if (volume == "primary") {
            android.os.Environment.getExternalStorageDirectory()
        } else {
            File("/storage", volume)
        }
        return if (relativePath.isBlank()) volumeRoot else File(volumeRoot, relativePath)
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
