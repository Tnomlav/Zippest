package com.decompress.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class QuickLocationsTest {
    @Test
    fun defaultQuickLocationsSurvivePersistenceEncoding() {
        val locations = defaultQuickLocations()

        assertFalse(locations.any { it.id == "music" })
        assertEquals(
            locations,
            decodeQuickLocations(encodeQuickLocations(locations))
        )
    }

    @Test
    fun customLabelsAndPathsSurvivePersistenceEncoding() {
        val locations = listOf(
            QuickLocation(
                id = "custom-1",
                label = "项目 | 归档",
                path = "Documents/客户/2026",
                icon = QuickLocationIcon.FOLDER
            )
        )

        assertEquals(
            locations,
            decodeQuickLocations(encodeQuickLocations(locations))
        )
    }

    @Test
    fun quickLocationPathsAcceptCommonInternalStorageForms() {
        assertEquals("Download/项目", normalizeQuickLocationPath("Download/项目"))
        assertEquals(
            "Download/项目",
            normalizeQuickLocationPath("/storage/emulated/0/Download/项目/")
        )
        assertEquals("Documents", normalizeQuickLocationPath("/sdcard//Documents"))
        assertEquals("", normalizeQuickLocationPath("primary:"))
    }

    @Test
    fun quickLocationPathsRejectTraversal() {
        assertNull(normalizeQuickLocationPath("Download/../Android"))
        assertEquals("Download", relativePathFromInternalTreeDocumentId("primary:Download"))
        assertNull(relativePathFromInternalTreeDocumentId("ABCD-1234:Download"))
    }
}
