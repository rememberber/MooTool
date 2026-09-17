package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageSvgWiringPresentationTest {
    @Test
    fun parseFieldsClampToElectronBounds() {
        assertEquals(64, ImageSvgWiringPresentation.parseColorsField("999", 16))
        assertEquals(16, ImageSvgWiringPresentation.parseColorsField("abc", 16))
        assertEquals(128, ImageSvgWiringPresentation.parseSpeckleField("200", 4))
        assertEquals(0, ImageSvgWiringPresentation.parseSpeckleField("", 0))
    }
}
