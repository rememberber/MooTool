package com.rememberber.mootool.next.compose.ui.workbench

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowBoundsPolicyTest {
    private val primary = DisplayWorkArea(0, 0, 1920, 1080)
    private val secondary = DisplayWorkArea(1920, 0, 1920, 1080)

    @Test
    fun missingPositionCentersWithMinimumSize() {
        val restored = WindowBoundsPolicy.clamp(null, null, 800, 500, listOf(primary))
        assertNull(restored.x)
        assertNull(restored.y)
        assertEquals(960, restored.width)
        assertEquals(640, restored.height)
    }

    @Test
    fun onScreenBoundsStayUnchanged() {
        val restored = WindowBoundsPolicy.clamp(120, 80, 1440, 920, listOf(primary))
        assertEquals(120, restored.x)
        assertEquals(80, restored.y)
        assertEquals(1440, restored.width)
        assertEquals(920, restored.height)
    }

    @Test
    fun secondMonitorStaysOnSecondMonitor() {
        val restored = WindowBoundsPolicy.clamp(2000, 100, 1440, 920, listOf(primary, secondary))
        assertEquals(2000, restored.x)
        assertEquals(100, restored.y)
    }

    @Test
    fun offScreenTitleBarClampsIntoNearestWorkArea() {
        val restored = WindowBoundsPolicy.clamp(5000, 100, 1440, 920, listOf(primary))
        assertEquals(1920 - WindowBoundsPolicy.MIN_TITLE_VISIBLE, restored.x)
        assertEquals(100, restored.y)
    }

    @Test
    fun titleBarAboveScreenClampsDown() {
        val restored = WindowBoundsPolicy.clamp(100, -200, 1440, 920, listOf(primary))
        assertEquals(100, restored.x)
        assertEquals(0, restored.y)
    }

    @Test
    fun emptyScreensKeepSavedCoordinates() {
        val restored = WindowBoundsPolicy.clamp(40, 50, 1440, 920, emptyList())
        assertEquals(40, restored.x)
        assertEquals(50, restored.y)
    }
}
