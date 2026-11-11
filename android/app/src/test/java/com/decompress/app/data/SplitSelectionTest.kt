package com.decompress.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitSelectionTest {
    @Test
    fun `first numbered volume wins over the rest`() {
        val names = listOf("a.tar.gz.001", "a.tar.gz.002", "a.tar.gz.003", "other.zip")
        assertEquals(
            listOf("a.tar.gz.001", "other.zip"),
            splitSelectionKeepIndices(names).map { names[it] }
        )
    }

    @Test
    fun `first zip volume wins over z02 and the final zip`() {
        val names = listOf("a.z01", "a.z02", "a.zip")
        assertEquals(
            listOf("a.z01"),
            splitSelectionKeepIndices(names).map { names[it] }
        )
    }

    @Test
    fun `first rar part wins`() {
        val names = listOf("a.part1.rar", "a.part2.rar")
        assertEquals(
            listOf("a.part1.rar"),
            splitSelectionKeepIndices(names).map { names[it] }
        )
    }

    @Test
    fun `selection without the first volume is left alone`() {
        val names = listOf("a.tar.gz.002", "a.tar.gz.003")
        assertEquals(
            names,
            splitSelectionKeepIndices(names).map { names[it] }
        )
    }
}
