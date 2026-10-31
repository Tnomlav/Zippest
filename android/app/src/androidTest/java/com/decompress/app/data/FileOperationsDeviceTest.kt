package com.decompress.app.data

import android.app.Instrumentation
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileOperationsDeviceTest {
    private lateinit var context: Context
    private lateinit var controller: FileBrowserController
    private lateinit var baseDirectory: File

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        allowAllFilesAccess(InstrumentationRegistry.getInstrumentation())
        baseDirectory = File(
            Environment.getExternalStorageDirectory(),
            "Download/ZippestDeviceTest-${System.currentTimeMillis()}"
        )
        assertTrue(baseDirectory.mkdirs())
        controller = FileBrowserController(context)
        assertTrue(controller.openInternalStorage())
    }

    @After
    fun tearDown() {
        if (::baseDirectory.isInitialized) {
            baseDirectory.deleteRecursively()
        }
    }

    @Test
    fun createRenameAndDeleteUseTheRealFileSystem() = runBlocking {
        openFolder("Download", baseDirectory.name)

        assertTrue(controller.createFolder("created"))
        val created = File(baseDirectory, "created")
        assertTrue(created.isDirectory)

        controller.refresh()
        val createdEntry = controller.entries.single { it.name == "created" }
        controller.startSelection(createdEntry.uri)
        assertTrue(controller.renameSelected("renamed"))

        val renamed = File(baseDirectory, "renamed")
        assertTrue(renamed.isDirectory)
        assertFalse(created.exists())

        controller.refresh()
        val renamedEntry = controller.entries.single { it.name == "renamed" }
        controller.startSelection(renamedEntry.uri)
        val trash = TrashRepository(context).apply { empty() }
        assertEquals(1, controller.deleteSelected())
        assertFalse(renamed.exists())
        val trashed = trash.list().single()
        assertEquals("renamed", trashed.name)
        assertTrue(trash.restore(trashed.id))
        assertTrue(renamed.isDirectory)
    }

    @Test
    fun renameEntryReturnsTheLiveRenamedFile() = runBlocking {
        openFolder("Download", baseDirectory.name)
        val source = File(baseDirectory, "sample.7z").apply { writeText("archive") }
        controller.refresh()
        val entry = controller.entries.single { it.name == source.name }

        val renamed = controller.renameEntry(entry, "sample.zip")

        assertNotNull(renamed)
        assertEquals("sample.zip", renamed!!.name)
        assertEquals(Uri.fromFile(File(baseDirectory, "sample.zip")), renamed.uri)
        assertFalse(source.exists())
        assertEquals("archive", File(baseDirectory, "sample.zip").readText())
    }

    @Test
    fun externalStorageDocumentMapsToDirectFilePath() {
        val relative = "Download/${baseDirectory.name}"
        val treeUri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:$relative"
        )
        val documentUri = SafFileRepository(context).rootDocumentUri(treeUri)
        val mapped = SafFileRepository(context).localFileForUri(documentUri)

        assertEquals(baseDirectory.canonicalPath, mapped?.canonicalPath)
    }

    @Test
    fun copyAndMoveCompleteAcrossDirectStorageAndSafDestination() = runBlocking {
        val sourceDirectory = File(baseDirectory, "source").apply { assertTrue(mkdirs()) }
        val copySource = File(sourceDirectory, "copy.txt").apply { writeText("copy") }
        val moveSource = File(sourceDirectory, "move.txt").apply { writeText("move") }
        val destinationTree = createProviderDirectory("destination")
        val destinationRoot = SafFileRepository(context).rootDocumentUri(destinationTree)

        openFolder("Download", baseDirectory.name, sourceDirectory.name)
        controller.startSelection(Uri.fromFile(copySource))
        val copySummary = controller.transferSelected(destinationTree, move = false)
        assertEquals(1, copySummary.completed)
        assertTrue(copySource.exists())
        assertEquals("copy", readProviderText(destinationRoot, "copy.txt"))

        openFolder("Download", baseDirectory.name, sourceDirectory.name)
        controller.startSelection(Uri.fromFile(moveSource))
        val moveSummary = controller.transferSelected(destinationTree, move = true)
        assertEquals(1, moveSummary.completed)
        assertFalse(moveSource.exists())
        assertEquals("move", readProviderText(destinationRoot, "move.txt"))
    }

    @Test
    fun sameFolderMoveIsRejectedWithoutDeletingTheSource() = runBlocking {
        val sourceDirectory = File(baseDirectory, "same-folder").apply {
            assertTrue(mkdirs())
        }
        val source = File(sourceDirectory, "kept.txt").apply { writeText("keep") }
        val sameTree = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Download/${baseDirectory.name}/same-folder"
        )

        openFolder("Download", baseDirectory.name, sourceDirectory.name)
        controller.startSelection(Uri.fromFile(source))
        val summary = controller.transferSelected(sameTree, move = true)

        assertEquals(1, summary.failed)
        assertTrue(source.exists())
        assertEquals("keep", source.readText())
    }

    @Test
    fun inAppDestinationCopyAndMoveStayInsideTheCurrentTree() = runBlocking {
        val sourceDirectory = File(baseDirectory, "in-app-source").apply {
            assertTrue(mkdirs())
        }
        val destinationDirectory = File(baseDirectory, "in-app-destination").apply {
            assertTrue(mkdirs())
        }
        val copySource = File(sourceDirectory, "copy.txt").apply { writeText("copy") }
        val moveSource = File(sourceDirectory, "move.txt").apply { writeText("move") }

        openFolder("Download", baseDirectory.name, sourceDirectory.name)
        controller.startSelection(Uri.fromFile(copySource))
        val copySummary = controller.transferSelectedToDirectory(
            destinationDirectoryUri = Uri.fromFile(destinationDirectory),
            destinationTreeUri = controller.treeUri!!,
            move = false
        )
        assertEquals(1, copySummary.completed)
        assertTrue(copySource.exists())
        assertEquals("copy", File(destinationDirectory, "copy.txt").readText())

        openFolder("Download", baseDirectory.name, sourceDirectory.name)
        controller.startSelection(Uri.fromFile(moveSource))
        val moveSummary = controller.transferSelectedToDirectory(
            destinationDirectoryUri = Uri.fromFile(destinationDirectory),
            destinationTreeUri = controller.treeUri!!,
            move = true
        )
        assertEquals(1, moveSummary.completed)
        assertFalse(moveSource.exists())
        assertEquals("move", File(destinationDirectory, "move.txt").readText())
    }

    @Test
    fun explicitInPlaceTransferUsesTheRecordedSourceAndRestoresIt() = runBlocking {
        val sourceDirectory = File(baseDirectory, "source").apply { mkdirs() }
        val destinationDirectory = File(baseDirectory, "destination").apply { mkdirs() }
        val source = File(sourceDirectory, "copy.txt").apply { writeText("copy") }
        openFolder("Download", baseDirectory.name)
        val sourceLocation = requireNotNull(controller.captureLocation())
        val sourceEntry = controller.entries.single { it.name == sourceDirectory.name }
        val destinationEntry = controller.entries.single {
            it.name == destinationDirectory.name
        }

        controller.open(destinationEntry)
        val summary = controller.transferEntriesToDirectory(
            sources = listOf(sourceEntry),
            sourceDirectoryUri = sourceLocation.currentUri,
            destinationDirectoryUri = controller.currentUri!!,
            destinationTreeUri = controller.treeUri!!,
            move = true
        )
        assertTrue(controller.restoreLocation(sourceLocation))

        assertEquals(1, summary.completed)
        assertEquals(sourceLocation.currentUri, controller.currentUri)
        assertFalse(sourceDirectory.exists())
        assertEquals(
            "copy",
            File(destinationDirectory, sourceDirectory.name)
                .resolve("copy.txt")
                .readText()
        )
    }

    @Test
    fun safRenameAndDeleteWorkThroughDocumentsProvider() = runBlocking {
        val repository = SafFileRepository(context)
        val treeUri = createProviderDirectory("saf")
        val root = repository.rootDocumentUri(treeUri)
        val fileUri = DocumentsContract.createDocument(
            context.contentResolver,
            root,
            "text/plain",
            "before.txt"
        )!!
        context.contentResolver.openOutputStream(fileUri)?.use { output ->
            output.write("saf".toByteArray())
        }
        val entry = repository.listChildren(treeUri, root)
            .single { it.name == "before.txt" }

        val renamedUri = repository.renameWithUri(entry.uri, "after.txt")
        assertNotNull(renamedUri)
        assertEquals("saf", context.contentResolver.openInputStream(renamedUri!!)!!
            .bufferedReader()
            .use { it.readText() })
        val renamedEntry = repository.listChildren(
            treeUri,
            root
        ).single { it.name == "after.txt" }
        assertEquals(renamedUri, renamedEntry.uri)
        assertTrue(repository.delete(renamedUri))
        assertTrue(repository.listChildren(treeUri, root).isEmpty())
    }

    @Test
    fun safCopyAndMovePreserveContentAndSourceState() = runBlocking {
        val repository = SafFileRepository(context)
        val sourceTree = createProviderDirectory("saf-source")
        val destinationTree = createProviderDirectory("saf-destination")
        val sourceRoot = repository.rootDocumentUri(sourceTree)
        val destinationRoot = repository.rootDocumentUri(destinationTree)
        val fileUri = DocumentsContract.createDocument(
            context.contentResolver,
            sourceRoot,
            "text/plain",
            "source.txt"
        )!!
        context.contentResolver.openOutputStream(fileUri)?.use { output ->
            output.write("payload".toByteArray())
        }
        val source = repository.listChildren(sourceTree, sourceRoot).single()

        val copyResult = repository.copyOrMove(
            source,
            sourceTree,
            destinationTree,
            move = false
        )
        assertEquals(TransferOutcome.COMPLETED, copyResult.outcome)
        assertEquals(1, repository.listChildren(sourceTree, sourceRoot).size)
        assertEquals(
            "payload",
            repository.listChildren(destinationTree, destinationRoot)
                .single { it.name == "source.txt" }
                .let {
                    context.contentResolver.openInputStream(it.uri)!!
                        .bufferedReader()
                        .use { reader -> reader.readText() }
                }
        )

        val moveResult = repository.copyOrMove(
            source,
            sourceTree,
            destinationTree,
            move = true
        )
        assertEquals(TransferOutcome.COMPLETED, moveResult.outcome)
        assertTrue(repository.listChildren(sourceTree, sourceRoot).isEmpty())
        assertEquals(
            setOf("source (1).txt", "source.txt"),
            repository.listChildren(destinationTree, destinationRoot)
                .map { it.name }
                .toSet()
        )
    }

    @Test
    fun inAppSafDestinationUsesADocumentSubfolder() = runBlocking {
        val repository = SafFileRepository(context)
        val sourceTree = createProviderDirectory("saf-in-app")
        val sourceRoot = repository.rootDocumentUri(sourceTree)
        val destination = DocumentsContract.createDocument(
            context.contentResolver,
            sourceRoot,
            Document.MIME_TYPE_DIR,
            "Destination"
        )!!
        DocumentsContract.createDocument(
            context.contentResolver,
            sourceRoot,
            "text/plain",
            "source.txt"
        )!!.let { fileUri ->
            context.contentResolver.openOutputStream(fileUri)?.use {
                it.write("saf-payload".toByteArray())
            }
        }

        controller.chooseRoot(sourceTree)
        controller.refresh()
        val source = controller.entries.single { it.name == "source.txt" }
        controller.startSelection(source.uri)
        val summary = controller.transferSelectedToDirectory(
            destinationDirectoryUri = destination,
            destinationTreeUri = sourceTree,
            move = false
        )

        assertEquals(1, summary.completed)
        val copied = repository.listChildren(sourceTree, destination).single()
        assertEquals("source.txt", copied.name)
        assertEquals(
            "saf-payload",
            context.contentResolver.openInputStream(copied.uri)!!
                .bufferedReader()
                .use { it.readText() }
        )
    }

    private suspend fun openFolder(vararg names: String) {
        controller.openInternalStorage()
        for (name in names) {
            controller.refresh()
            val entry = controller.entries.single { it.name == name }
            controller.open(entry)
        }
        controller.refresh()
    }

    private fun allowAllFilesAccess(instrumentation: Instrumentation) {
        repeat(20) {
            instrumentation.uiAutomation.executeShellCommand(
                "appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow"
            ).close()
            if (Environment.isExternalStorageManager()) return
            Thread.sleep(250)
        }
    }

    private fun createProviderDirectory(name: String): Uri {
        val rootTree = DocumentsContract.buildTreeDocumentUri(
            TestDocumentsProvider.AUTHORITY,
            TestDocumentsProvider.ROOT_ID
        )
        val root = SafFileRepository(context).rootDocumentUri(rootTree)
        val created = DocumentsContract.createDocument(
            context.contentResolver,
            root,
            Document.MIME_TYPE_DIR,
            name
        )!!
        return DocumentsContract.buildTreeDocumentUri(
            TestDocumentsProvider.AUTHORITY,
            DocumentsContract.getDocumentId(created)
        )
    }

    private fun readProviderText(parent: Uri, name: String): String {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            parent,
            DocumentsContract.getDocumentId(parent)
        )
        var fileUri: Uri? = null
        context.contentResolver.query(
            children,
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) {
                    fileUri = DocumentsContract.buildDocumentUriUsingTree(
                        parent,
                        cursor.getString(0)
                    )
                    break
                }
            }
        }
        return context.contentResolver.openInputStream(
            fileUri ?: error("Missing provider document $name")
        )!!
            .bufferedReader()
            .use { it.readText() }
    }
}
