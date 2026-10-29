package com.decompress.app.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameEditingTest {
    @Test
    fun selectsSuffixAfterLastDot() {
        val value = fileNameWithSuffixSelected("backup.tar.GZ")
        assertEquals(TextRange(10, 13), value.selection)
        assertEquals(".GZ", value.text.substring(10, 13))
    }

    @Test
    fun placesCursorAtEndWhenThereIsNoDot() {
        val value = fileNameWithSuffixSelected("README")
        assertEquals(TextRange(6, 6), value.selection)
    }

    @Test
    fun treatsLeadingDotFileAsHavingASuffix() {
        val value = fileNameWithSuffixSelected(".env")
        assertEquals(TextRange(0, 4), value.selection)
    }

    @Test
    fun replacesOnlySelectedSuffixAndKeepsItSelected() {
        val initial = fileNameWithSuffixSelected("photo.jpeg")
        val replaced = replaceSelectedFileSuffix(initial, "7z")
        assertEquals("photo.7z", replaced.text)
        assertEquals(TextRange(5, 8), replaced.selection)
    }

    @Test
    fun sanitizesCustomSuffix() {
        assertEquals(".tar.gz", sanitizeFileSuffix(".tar.gz"))
        assertEquals(".tar.gz", sanitizeFileSuffix("tar.gz"))
        assertEquals(".custom", sanitizeFileSuffix(" cus/tom "))
    }
}
