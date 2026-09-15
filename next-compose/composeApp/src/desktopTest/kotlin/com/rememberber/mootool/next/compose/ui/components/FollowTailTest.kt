package com.rememberber.mootool.next.compose.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowTailTest {
    @Test
    fun pinnedNearBottomAndEmpty() {
        assertTrue(FollowTail.isPinned(value = 0, maxValue = 0))
        assertTrue(FollowTail.isPinned(value = 200, maxValue = 200))
        assertTrue(FollowTail.isPinned(value = 160, maxValue = 200))
        assertFalse(FollowTail.isPinned(value = 100, maxValue = 200))
        assertFalse(FollowTail.isPinned(value = 0, maxValue = 400))
    }

    @Test
    fun newStreamResetsPinWithoutIdleClear() {
        assertFalse(FollowTail.shouldResetPin(null))
        assertFalse(FollowTail.shouldResetPin(""))
        assertTrue(FollowTail.shouldResetPin("run-1"))
        assertTrue(FollowTail.shouldResetPin("ping"))
    }
}
