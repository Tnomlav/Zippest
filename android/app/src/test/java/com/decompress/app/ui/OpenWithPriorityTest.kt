package com.decompress.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenWithPriorityTest {
    @Test
    fun galleryIsClassifiedAsDirectOpen() {
        assertEquals(
            OpenWithCategory.DIRECT,
            openWithCategory("相册", "com.vivo.gallery", "GalleryActivity")
        )
        assertEquals(
            OpenWithCategory.DIRECT,
            openWithCategory("Photos", "com.google.android.apps.photos", "Viewer")
        )
    }

    @Test
    fun sendAndShareTargetsAreClassifiedSeparately() {
        assertEquals(
            OpenWithCategory.SHARE,
            openWithCategory("发送到朋友圈", "com.tencent.mm", "ShareActivity")
        )
        assertEquals(
            OpenWithCategory.SHARE,
            openWithCategory("Send to friend", "example.social", "SendActivity")
        )
    }
}
