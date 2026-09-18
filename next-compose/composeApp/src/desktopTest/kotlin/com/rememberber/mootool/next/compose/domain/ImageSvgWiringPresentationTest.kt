package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageSvgWiringPresentationTest {
    @Test
    fun parseFieldsClampToElectronBounds() {
        assertEquals(64, ImageSvgWiringPresentation.parseColorsField("999", 16))
        assertEquals(16, ImageSvgWiringPresentation.parseColorsField("abc", 16))
        assertEquals(128, ImageSvgWiringPresentation.parseSpeckleField("200", 4))
        assertEquals(0, ImageSvgWiringPresentation.parseSpeckleField("", 0))
    }

    @Test
    fun svgBatchRequiresSelectionAndIdle() {
        assertFalse(ImageSvgWiringPresentation.canStartSvgBatch(selectedCount = 0, busy = false))
        assertFalse(ImageSvgWiringPresentation.canStartSvgBatch(selectedCount = 2, busy = true))
        assertTrue(ImageSvgWiringPresentation.canStartSvgBatch(selectedCount = 2, busy = false))
    }

    @Test
    fun runWriteSvgFileRoundTrip() {
        val file = Files.createTempFile("svg-out-", ".svg")
        try {
            val outcome = ImageSvgWiringPresentation.runWriteSvgFile(file, "<svg/>")
            assertTrue(outcome is ImageSvgWiringPresentation.WriteSvgOutcome.Success)
            assertEquals("<svg/>", Files.readString(file))
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
