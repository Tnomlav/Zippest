package com.decompress.app.data

import com.decompress.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArchiveErrorKindTest {

    @Test
    fun passwordKindHasResource() {
        assertEquals(
            R.string.error_password,
            ArchiveException("whatever", kind = ArchiveErrorKind.PASSWORD).archiveErrorKind().messageRes()
        )
    }

    @Test
    fun tooLargeKindHasResource() {
        assertEquals(
            R.string.error_too_large,
            ArchiveException("whatever", kind = ArchiveErrorKind.TOO_LARGE).archiveErrorKind().messageRes()
        )
    }

    @Test
    fun noSpaceKindHasResource() {
        assertEquals(
            R.string.error_no_space,
            ArchiveErrorKind.NO_SPACE.messageRes()
        )
    }

    @Test
    fun otherKindHasNoResource() {
        assertNull(ArchiveErrorKind.OTHER.messageRes())
    }
}
