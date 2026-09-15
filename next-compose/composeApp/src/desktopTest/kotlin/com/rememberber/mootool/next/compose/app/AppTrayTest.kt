package com.rememberber.mootool.next.compose.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTrayTest {
    @Test
    fun trayImageIsPresentEvenWithoutNativeTray() {
        val image = AppTray.trayImage()
        assertEquals(16, image.width)
        assertEquals(16, image.height)
        assertTrue(image.getRGB(8, 8) != 0, "tray glyph should not be fully transparent")
    }

    @Test
    fun unavailableReasonMatchesSupportFlag() {
        if (AppTray.supported()) {
            assertEquals(null, AppTray.unavailableReason())
        } else {
            assertTrue(AppTray.unavailableReason()!!.contains("SystemTray"))
        }
    }
}
