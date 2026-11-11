package com.decompress.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveErrorKindTest {

    @Test
    fun passwordKindMapsWithoutChineseKeyword() {
        val message = ArchiveException("whatever", kind = ArchiveErrorKind.PASSWORD).archiveMessage()
        assertTrue(message, message.contains("密码错误"))
    }

    @Test
    fun tooLargeKindMaps() {
        val message = ArchiveException("whatever", kind = ArchiveErrorKind.TOO_LARGE).archiveMessage()
        assertTrue(message, message.contains("安全上限"))
    }

    @Test
    fun noSpaceKindMaps() {
        val message = ArchiveException("whatever", kind = ArchiveErrorKind.NO_SPACE).archiveMessage()
        assertTrue(message, message.contains("存储空间不足"))
    }

    @Test
    fun otherKindFallsBackToDetail() {
        val message = ArchiveException("boom").archiveMessage()
        assertTrue(message, message.contains("boom"))
    }
}
