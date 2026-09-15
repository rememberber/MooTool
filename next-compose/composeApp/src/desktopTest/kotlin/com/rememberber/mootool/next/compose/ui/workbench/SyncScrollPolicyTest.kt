package com.rememberber.mootool.next.compose.ui.workbench

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncScrollPolicyTest {
    @Test
    fun copiesAbsoluteOffsetAndClampsToShorterPane() {
        assertEquals(120, SyncScrollPolicy.targetOffset(120, 800))
        assertEquals(500, SyncScrollPolicy.targetOffset(700, 500))
        assertEquals(0, SyncScrollPolicy.targetOffset(-4, 500))
        assertEquals(0, SyncScrollPolicy.targetOffset(40, 0))
    }

    @Test
    fun skipsNoopAndLockedUpdates() {
        assertFalse(SyncScrollPolicy.shouldApply(syncing = true, currentTarget = 0, nextTarget = 40))
        assertFalse(SyncScrollPolicy.shouldApply(syncing = false, currentTarget = 40, nextTarget = 40))
        assertTrue(SyncScrollPolicy.shouldApply(syncing = false, currentTarget = 40, nextTarget = 80))
    }

    @Test
    fun clampedShorterPaneDoesNotRewindLongerPane() {
        assertFalse(SyncScrollPolicy.shouldFollowClamped(sourceValue = 500, sourceMax = 500, targetValue = 900))
        assertTrue(SyncScrollPolicy.shouldFollowClamped(sourceValue = 200, sourceMax = 500, targetValue = 900))
        assertTrue(SyncScrollPolicy.shouldFollowClamped(sourceValue = 900, sourceMax = 1000, targetValue = 500))
        assertTrue(SyncScrollPolicy.shouldFollowClamped(sourceValue = 0, sourceMax = 0, targetValue = 40))
    }
}
