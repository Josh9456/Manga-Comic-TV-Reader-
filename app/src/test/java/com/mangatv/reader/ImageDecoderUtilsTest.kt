package com.mangatv.reader

import com.mangatv.reader.domain.archive.ImageDecoderUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageDecoderUtilsTest {

    @Test
    fun testIsImageFileValidExtensions() {
        assertTrue(ImageDecoderUtils.isImageFile("page1.jpg"))
        assertTrue(ImageDecoderUtils.isImageFile("PAGE2.JPEG"))
        assertTrue(ImageDecoderUtils.isImageFile("scan_03.png"))
        assertTrue(ImageDecoderUtils.isImageFile("04.webp"))
        assertTrue(ImageDecoderUtils.isImageFile("05.avif"))
    }

    @Test
    fun testIsImageFileRejectsInvalidAndHidden() {
        assertFalse(ImageDecoderUtils.isImageFile("ComicInfo.xml"))
        assertFalse(ImageDecoderUtils.isImageFile(".DS_Store"))
        assertFalse(ImageDecoderUtils.isImageFile("__MACOSX/._page1.jpg"))
        assertFalse(ImageDecoderUtils.isImageFile("notes.txt"))
    }
}
