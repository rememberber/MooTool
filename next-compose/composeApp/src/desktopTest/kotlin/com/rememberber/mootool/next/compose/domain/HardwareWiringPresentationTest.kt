package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HardwareWiringPresentationTest {
    @Test
    fun refreshDisabledWhileLoading() {
        assertFalse(HardwareWiringPresentation.refreshEnabled(loading = true))
        assertTrue(HardwareWiringPresentation.refreshEnabled(loading = false))
    }

    @Test
    fun copyRequiresGroupsAndIdle() {
        assertFalse(HardwareWiringPresentation.copyReportEnabled(loading = true, hasGroups = true))
        assertFalse(HardwareWiringPresentation.copyReportEnabled(loading = false, hasGroups = false))
        assertTrue(HardwareWiringPresentation.copyReportEnabled(loading = false, hasGroups = true))
    }
}
