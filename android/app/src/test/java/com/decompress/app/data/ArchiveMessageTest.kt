package com.decompress.app.data

import com.decompress.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveMessageTest {

    @Test
    fun enospcBecomesNoSpaceKind() {
        val kind = RuntimeException("write failed: ENOSPC (No space left on device)")
            .archiveErrorKind()
        assertEquals(ArchiveErrorKind.NO_SPACE, kind)
        assertEquals(R.string.error_no_space, kind.messageRes())
    }

    @Test
    fun neverEndsUpAsChineseLiteral() {
        val kind = IllegalStateException("This is not a zip file").archiveErrorKind()
        assertEquals(ArchiveErrorKind.OTHER, kind)
    }

    @Test
    fun wrongPasswordBecomesPasswordKind() {
        val kind = RuntimeException("Wrong password for entry").archiveErrorKind()
        assertEquals(ArchiveErrorKind.PASSWORD, kind)
    }
}
