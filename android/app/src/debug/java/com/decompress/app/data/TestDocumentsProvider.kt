package com.decompress.app.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import java.io.File
import java.io.FileNotFoundException

class TestDocumentsProvider : ContentProvider() {
    private lateinit var rootDirectory: File

    override fun onCreate(): Boolean {
        rootDirectory = File(context!!.filesDir, "test-documents").apply { mkdirs() }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val segments = uri.pathSegments
        val columns = projection ?: DOCUMENT_PROJECTION
        if (segments.lastOrNull() == "children") {
            val parentId = segments[segments.lastIndex - 1]
            val parent = fileFor(parentId)
            if (!parent.isDirectory) throw FileNotFoundException(parentId)
            return MatrixCursor(columns).apply {
                parent.listFiles().orEmpty().sortedBy { it.name }.forEach { child ->
                    addDocumentRow(this, child.relativeTo(rootDirectory).invariantSeparatorsPath)
                }
            }
        }
        val documentIndex = segments.indexOf("document")
        if (documentIndex >= 0 && documentIndex + 1 < segments.size) {
            return MatrixCursor(columns).apply {
                addDocumentRow(this, segments[documentIndex + 1])
            }
        }
        return MatrixCursor(projection ?: ROOT_PROJECTION).apply {
            newRow().apply {
                add(Root.COLUMN_ROOT_ID, ROOT_ID)
                add(Root.COLUMN_DOCUMENT_ID, ROOT_ID)
                add(Root.COLUMN_TITLE, "Zippest test storage")
                add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
                add(Root.COLUMN_MIME_TYPES, "*/*")
            }
        }
    }

    override fun getType(uri: Uri): String {
        val id = documentIdFromUri(uri)
        return if (fileFor(id).isDirectory) Document.MIME_TYPE_DIR else "application/octet-stream"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = fileFor(documentIdFromUri(uri))
        if (!file.exists()) throw FileNotFoundException(file.toString())
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val file = fileFor(documentIdFromUri(uri))
        if (!file.deleteRecursively()) throw FileNotFoundException(file.toString())
        return 1
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            "android:createDocument" -> {
                val parentUri = extras?.uriExtra("uri") ?: return null
                val parentId = documentIdFromUri(parentUri)
                val mimeType =
                    extras.getString(Document.COLUMN_MIME_TYPE) ?: return null
                val displayName =
                    extras.getString(Document.COLUMN_DISPLAY_NAME) ?: return null
                val parent = fileFor(parentId)
                val target = File(parent, displayName)
                if (target.exists()) return null
                val created = if (mimeType == Document.MIME_TYPE_DIR) {
                    target.mkdirs()
                } else {
                    target.createNewFile()
                }
                if (!created) return null
                bundleFor(
                    target.relativeTo(rootDirectory).invariantSeparatorsPath
                )
            }
            "android:renameDocument" -> {
                val sourceUri =
                    extras?.uriExtra("uri") ?: return null
                val source = fileFor(documentIdFromUri(sourceUri))
                val displayName =
                    extras.getString(Document.COLUMN_DISPLAY_NAME) ?: return null
                val target = File(source.parentFile, displayName)
                if (target.exists() || !source.renameTo(target)) return null
                bundleFor(target.relativeTo(rootDirectory).invariantSeparatorsPath)
            }
            "android:deleteDocument" -> {
                val documentUri =
                    extras?.uriExtra("uri") ?: return null
                val file = fileFor(documentIdFromUri(documentUri))
                if (!file.deleteRecursively()) throw FileNotFoundException(file.toString())
                Bundle.EMPTY
            }
            "android:isChildDocument" -> {
                val parentUri =
                    extras?.uriExtra("uri") ?: return null
                val childUri =
                    extras?.uriExtra("targetUri") ?: return null
                val parentId = documentIdFromUri(parentUri)
                val childId = documentIdFromUri(childUri)
                Bundle().apply {
                    putBoolean("result", childId == parentId || childId.startsWith("$parentId/"))
                }
            }
            else -> super.call(method, arg, extras)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    private fun bundleFor(documentId: String): Bundle =
        Bundle().apply {
            putParcelable(
                "uri",
                DocumentsContract.buildDocumentUri(AUTHORITY, documentId)
            )
        }

    @Suppress("DEPRECATION")
    private fun Bundle.uriExtra(key: String): Uri? = getParcelable(key)

    private fun addDocumentRow(cursor: MatrixCursor, documentId: String) {
        val file = fileFor(documentId)
        if (!file.exists()) return
        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, documentId)
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(
                Document.COLUMN_MIME_TYPE,
                if (file.isDirectory) Document.MIME_TYPE_DIR else "application/octet-stream"
            )
            add(Document.COLUMN_SIZE, if (file.isFile) file.length() else 0L)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(
                Document.COLUMN_FLAGS,
                if (file.isDirectory) {
                    Document.FLAG_DIR_SUPPORTS_CREATE
                } else {
                    Document.FLAG_SUPPORTS_WRITE or
                        Document.FLAG_SUPPORTS_DELETE or
                        Document.FLAG_SUPPORTS_RENAME
                }
            )
        }
    }

    private fun documentIdFromUri(uri: Uri): String {
        val segments = uri.pathSegments
        val documentIndex = segments.indexOf("document")
        if (documentIndex >= 0 && documentIndex + 1 < segments.size) {
            return segments[documentIndex + 1]
        }
        return DocumentsContract.getDocumentId(uri)
    }

    private fun fileFor(documentId: String): File {
        if (documentId == ROOT_ID) return rootDirectory
        val parts = documentId.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) {
            throw FileNotFoundException(documentId)
        }
        return File(rootDirectory, documentId)
    }

    companion object {
        const val AUTHORITY = "com.decompress.app.test.documents"
        const val ROOT_ID = "root"
        private val ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_TITLE,
            Root.COLUMN_FLAGS,
            Root.COLUMN_MIME_TYPES
        )
        private val DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS
        )
    }
}
