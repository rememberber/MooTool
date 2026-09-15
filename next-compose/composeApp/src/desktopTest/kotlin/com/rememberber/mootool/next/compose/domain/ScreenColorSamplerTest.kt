package com.rememberber.mootool.next.compose.domain

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenColorSamplerTest {
    @Test
    fun samplesNegativeOriginAndZooms() {
        val image = BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB)
        image.setRGB(1, 0, 0xDE8F7D)
        image.setRGB(0, 0, 0x0A141E)
        val capture = ScreenCapture(originX = -10, originY = 20, image = image)
        assertEquals(RgbColor(222, 143, 125), ScreenColorSampler.colorAt(capture, -9, 20))
        assertEquals(RgbColor(10, 20, 30), ScreenColorSampler.colorAt(capture, -10, 20))
        val zoomed = ScreenColorSampler.zoom(capture, -9, 20, radius = 1, scale = 4)
        assertEquals(12, zoomed.width)
        assertEquals(12, zoomed.height)
        assertEquals(0xDE8F7D, zoomed.getRGB(4, 4) and 0xffffff)
    }

    @Test
    fun permissionCopyOpensMacScreenCapturePane() {
        assertTrue(ScreenCaptureAccess.macPrivacySettingsUri().contains("Privacy_ScreenCapture"))
        val opened = ScreenCaptureAccess.userMessage({ key -> key }, ColorException("permission", "blank", openedSettings = true))
        assertTrue(opened.contains("color.error.permissionSettings"))
        val denied = ScreenCaptureAccess.userMessage({ key -> key }, ColorException("permission", "blank"))
        assertEquals("color.error.permission", denied)
    }
}
