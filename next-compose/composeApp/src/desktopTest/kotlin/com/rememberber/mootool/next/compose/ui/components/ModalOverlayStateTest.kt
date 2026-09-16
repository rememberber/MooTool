package com.rememberber.mootool.next.compose.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ModalOverlayStateTest {
    @Test
    fun enterLeaveKeepsCountNonNegative() {
        val start = ModalOverlayState.count
        ModalOverlayState.enter()
        ModalOverlayState.enter()
        assertEquals(start + 2, ModalOverlayState.count)
        ModalOverlayState.leave()
        assertEquals(start + 1, ModalOverlayState.count)
        ModalOverlayState.leave()
        assertEquals(start, ModalOverlayState.count)
    }
}
