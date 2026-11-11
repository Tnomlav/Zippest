package com.decompress.app.data

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameTextTest {

    @Test
    fun nfcMakesPrecomposedAndCombiningNamesEqual() {
        val precomposed = "caf\u00E9"          // café
        val combining = "cafe\u0301"            // cafe + combining acute
        assertFalse(precomposed == combining)
        assertTrue(NameText.sameName(precomposed, combining))
    }

    @Test
    fun foldForSearchRemovesDiacriticsAndCase() {
        assertEquals("cafe", NameText.foldForSearch("Café"))
        assertEquals("uber", NameText.foldForSearch("Über"))
        assertEquals("file", NameText.foldForSearch("FILE"))
    }

    @Test
    fun searchFoldMatchesAccentedNames() {
        val haystack = NameText.foldForSearch("café.txt")
        val needle = NameText.foldForSearch("cafe")
        assertTrue(haystack.contains(needle))
    }

    @Test
    fun rtlScriptDetection() {
        assertTrue(NameText.hasRtlScript("ملف-عربي.txt"))
        assertTrue(NameText.hasRtlScript("עברית.txt"))
        assertFalse(NameText.hasRtlScript("日本語.txt"))
        assertFalse(NameText.hasRtlScript("café.txt"))
    }

    @Test
    fun sanitizeBidiStripsOverrides() {
        val spoofed = "evil\u202Etxt.exe"
        assertEquals("eviltxt.exe", NameText.sanitizeBidi(spoofed))
        assertEquals("abc", NameText.sanitizeBidi("a\u200Fb\u2066c"))
    }

    @Test
    fun truncateKeepsEmojiWhole() {
        val name = "a😀b😀c"
        val truncated = NameText.truncateByCodePoints(name, 3)
        assertEquals("a😀b", truncated)
        assertFalse(truncated.contains('\uFFFD'))
    }

    @Test
    fun collatorSortsCaseInsensitively() {
        val collator = NameText.collator(Locale.ENGLISH)
        val names = listOf("beta", "Alpha", "gamma").sortedWith(collator)
        assertEquals(listOf("Alpha", "beta", "gamma"), names)
    }

    @Test
    fun collatorHandlesChineseNames() {
        val collator = NameText.collator(Locale.SIMPLIFIED_CHINESE)
        val names = listOf("压缩包", "文件", "图片").sortedWith(collator)
        assertEquals(3, names.size)
        assertTrue(names.containsAll(listOf("压缩包", "文件", "图片")))
    }
}
