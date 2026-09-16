package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateCheckSurfacingTest {
    @Test
    fun manualCheckAlwaysSurfacesResultAndErrors() {
        assertTrue(UpdateCheckSurfacing.shouldApplyUiResult(false, UpdateCheckStatus.Latest))
        assertTrue(UpdateCheckSurfacing.shouldApplyUiResult(false, UpdateCheckStatus.Unpublished))
        assertTrue(UpdateCheckSurfacing.shouldApplyUiError(false))
    }

    @Test
    fun automaticCheckOnlySurfacesAvailable() {
        assertTrue(UpdateCheckSurfacing.shouldApplyUiResult(true, UpdateCheckStatus.Available))
        assertFalse(UpdateCheckSurfacing.shouldApplyUiResult(true, UpdateCheckStatus.Latest))
        assertFalse(UpdateCheckSurfacing.shouldApplyUiResult(true, UpdateCheckStatus.Unpublished))
        assertFalse(UpdateCheckSurfacing.shouldApplyUiError(true))
    }
}
