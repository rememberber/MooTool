package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultGitPullSchedulerTest {
    private var now = 1_000_000L

    @Test
    fun resetIntervalClockAllowsImmediatePull() {
        var pulls = 0
        val scheduler = VaultGitPullScheduler(
            enabled = { true },
            hasUnsavedEditorChanges = { false },
            intervalMilliseconds = { 60_000 },
            pull = {
                pulls++
                GitActionResult(true, "ok")
            },
            now = { now },
        )
        assertTrue(scheduler.evaluate())
        assertEquals(1, pulls)
        now += 30_000
        assertFalse(scheduler.evaluate())
        scheduler.resetIntervalClock()
        assertTrue(scheduler.evaluate())
        assertEquals(2, pulls)
    }
}
