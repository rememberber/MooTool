package com.rememberber.mootool.next.compose.ui.workbench

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolModalLifecycleTest {
    @Test
    fun shouldRunToolLeaveCleanup_when_not_detached() {
        assertTrue(shouldRunToolLeaveCleanup(isDetached = false))
    }

    @Test
    fun shouldRunToolLeaveCleanup_skips_when_detached_placeholder_disposes() {
        assertFalse(shouldRunToolLeaveCleanup(isDetached = true))
    }
}
