package com.rememberber.mootool.next.compose.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaneHandleTest {
    @Test
    fun keyboardNudgesMatchAxis() {
        assertEquals(-16f, paneKeyboardDelta("left", vertical = false))
        assertEquals(16f, paneKeyboardDelta("right", vertical = false))
        assertNull(paneKeyboardDelta("up", vertical = false))
        assertEquals(-16f, paneKeyboardDelta("up", vertical = true))
        assertEquals(16f, paneKeyboardDelta("down", vertical = true))
        assertNull(paneKeyboardDelta("left", vertical = true))
        assertNull(paneKeyboardDelta("enter", vertical = false))
    }
}
