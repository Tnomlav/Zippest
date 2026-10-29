package com.decompress.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TrashRepositoryDeviceTest {
    private lateinit var context: Context
    private lateinit var root: File
    private lateinit var repository: TrashRepository

    @Before
    fun setUp() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            root = File(context.cacheDir, "trash-test-${System.nanoTime()}").apply { mkdirs() }
            repository = TrashRepository(context)
            repository.empty()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            repository.empty()
            root.deleteRecursively()
        }
    }

    @Test
    fun movesRestoresAndHandlesNameConflicts() = runBlocking {
        val original = File(root, "notes.txt").apply { writeText("before delete") }
        assertTrue(repository.moveToTrash(original))
        assertFalse(original.exists())
        val item = repository.list().single()
        assertEquals("notes.txt", item.name)
        assertEquals(original.absolutePath, item.originalPath)

        original.writeText("new file")
        assertTrue(repository.restore(item.id))
        assertEquals("new file", original.readText())
        assertEquals("before delete", File(root, "notes (1).txt").readText())
        assertTrue(repository.list().isEmpty())
    }

    @Test
    fun movesDirectoriesAndCanEmptyTheTrash() = runBlocking {
        val directory = File(root, "folder").apply { mkdirs() }
        File(directory, "child.txt").writeText("child")
        assertTrue(repository.moveToTrash(directory))
        assertFalse(directory.exists())
        val item = repository.list().single()
        assertTrue(item.isDirectory)
        assertTrue(item.size >= 5)

        assertEquals(1, repository.empty())
        assertTrue(repository.list().isEmpty())
    }

    @Test
    fun purgesExpiredItemsAccordingToRetention() = runBlocking {
        val original = File(root, "old.txt").apply { writeText("old") }
        assertTrue(repository.moveToTrash(original))
        val item = repository.list().single()
        val trashRoot = (context.getExternalFilesDir(null) ?: context.filesDir)
            .resolve("trash")
        val metadataFile = File(trashRoot, "${item.id}/metadata.json")
        val metadata = JSONObject(metadataFile.readText())
            .put("deletedAt", System.currentTimeMillis() - 8L * 24L * 60L * 60L * 1000L)
        metadataFile.writeText(metadata.toString())

        assertEquals(1, repository.purgeExpired(7))
        assertTrue(repository.list().isEmpty())
    }
}
