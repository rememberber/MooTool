package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpTimeoutSettingsTest {
    @Test
    fun clamp_bounds_to_one_to_120_seconds() {
        assertEquals(1_000, HttpTimeoutSettings.clamp(0))
        assertEquals(120_000, HttpTimeoutSettings.clamp(999_999))
        assertEquals(45_000, HttpTimeoutSettings.clamp(45_000))
    }

    @Test
    fun commit_updates_global_when_clamped_value_differs() {
        val result = HttpTimeoutSettings.commit(45_000, 30_000)
        assertEquals(45_000, result.sessionMs)
        assertTrue(result.updateGlobal)
    }

    @Test
    fun commit_skips_global_when_already_aligned() {
        val result = HttpTimeoutSettings.commit(30_000, 30_000)
        assertEquals(30_000, result.sessionMs)
        assertFalse(result.updateGlobal)
    }

    @Test
    fun commit_clamps_before_global_compare() {
        val result = HttpTimeoutSettings.commit(500, 30_000)
        assertEquals(1_000, result.sessionMs)
        assertTrue(result.updateGlobal)
    }
}
