package com.decompress.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsInstrumentedTest {
    private lateinit var repository: AppSettingsRepository

    @Before
    fun setUp() {
        repository = AppSettingsRepository(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        repository.reset()
    }

    @After
    fun tearDown() {
        repository.reset()
    }

    @Test
    fun settingsPersistAcrossRepositoryInstances() {
        repository.update {
            it.copy(
                themeMode = ThemeMode.DARK,
                showHiddenFiles = true,
                showThumbnails = false,
                fillPasswordFromClipboard = true,
                notifyOnComplete = false,
                defaultDestinationMode = DefaultDestinationMode.CUSTOM,
                defaultConflictPolicy = ConflictPolicy.OVERWRITE,
                defaultEncoding = FileNameEncoding.GBK,
                defaultSortMode = BrowserSortMode.DATE,
                useTrash = false,
                trashRetention = TrashRetention.DAYS_90,
                customDestinationTreeUri = "content://test/tree/destination",
                customDestinationRootUri = "content://test/document/destination",
                customDestinationName = "Destination"
            )
        }

        val restored = AppSettingsRepository(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).settings

        assertEquals(ThemeMode.DARK, restored.themeMode)
        assertTrue(restored.showHiddenFiles)
        assertFalse(restored.showThumbnails)
        assertTrue(restored.fillPasswordFromClipboard)
        assertFalse(restored.notifyOnComplete)
        assertEquals(DefaultDestinationMode.CUSTOM, restored.defaultDestinationMode)
        assertEquals(ConflictPolicy.OVERWRITE, restored.defaultConflictPolicy)
        assertEquals(FileNameEncoding.GBK, restored.defaultEncoding)
        assertEquals(BrowserSortMode.DATE, restored.defaultSortMode)
        assertFalse(restored.useTrash)
        assertEquals(TrashRetention.DAYS_90, restored.trashRetention)
        assertEquals("Destination", restored.customDestinationName)
    }

    @Test
    fun resetRestoresUsableDefaults() {
        repository.update {
            it.copy(
                themeMode = ThemeMode.LIGHT,
                showHiddenFiles = true,
                notifyOnComplete = false
            )
        }

        repository.reset()

        assertEquals(AppSettings(), repository.settings)
    }
}
