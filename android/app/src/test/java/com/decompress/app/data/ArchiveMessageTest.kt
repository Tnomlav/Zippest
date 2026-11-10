package com.decompress.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveMessageTest {
    @Test
    fun `enospc becomes a storage hint`() {
        val message = RuntimeException("write failed: ENOSPC (No space left on device)").archiveMessage()
        assertTrue(message, message.contains("存储空间不足"))
    }

    @Test
    fun `no space wording becomes a storage hint`() {
        val message = IllegalStateException("No space left on device").archiveMessage()
        assertTrue(message, message.contains("存储空间不足"))
    }

    @Test
    fun `unknown errors keep their detail`() {
        assertEquals("some odd failure", RuntimeException("some odd failure").archiveMessage())
    }
}
