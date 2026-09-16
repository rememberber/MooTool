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

    @Test
    fun resizeLineMatchesElectronPaneResizer() {
        assertEquals(0f, paneHandleLineAlpha(hovered = false, focused = false, dragging = false))
        assertEquals(0.5f, paneHandleLineAlpha(hovered = true, focused = false, dragging = false))
        assertEquals(0.5f, paneHandleLineAlpha(hovered = false, focused = true, dragging = false))
        assertEquals(0.9f, paneHandleLineAlpha(hovered = true, focused = true, dragging = true))
        assertEquals(1f, paneHandleLineThickness(dragging = false))
        assertEquals(2f, paneHandleLineThickness(dragging = true))
    }
}
