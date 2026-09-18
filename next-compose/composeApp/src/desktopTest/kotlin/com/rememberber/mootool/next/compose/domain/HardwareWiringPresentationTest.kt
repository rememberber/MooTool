package com.rememberber.mootool.next.compose.domain

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HardwareWiringPresentationTest {
    @Test
    fun refreshDisabledWhileLoading() {
        assertFalse(HardwareWiringPresentation.refreshEnabled(loading = true))
        assertTrue(HardwareWiringPresentation.refreshEnabled(loading = false))
        assertFalse(HardwareWiringPresentation.refreshActionEnabled(loading = true))
        assertTrue(HardwareWiringPresentation.refreshActionEnabled(loading = false))
    }

    @Test
    fun copyRequiresGroupsAndIdle() {
        assertFalse(HardwareWiringPresentation.copyReportEnabled(loading = true, hasGroups = true))
        assertFalse(HardwareWiringPresentation.copyReportEnabled(loading = false, hasGroups = false))
        assertTrue(HardwareWiringPresentation.copyReportEnabled(loading = false, hasGroups = true))
        assertTrue(
            HardwareWiringPresentation.copyReportActionEnabled(loading = false, hasGroups = true),
        )
    }

    @Test
    fun interfacesCommandRequiresIdle() {
        assertFalse(HardwareWiringPresentation.interfacesCommandEnabled(loading = true, running = false))
        assertFalse(HardwareWiringPresentation.interfacesCommandEnabled(loading = false, running = true))
        assertTrue(HardwareWiringPresentation.interfacesCommandEnabled(loading = false, running = false))
    }

    @Test
    fun runCollectReturnsSnapshot() {
        val outcome = HardwareWiringPresentation.runCollect(loadSampleMs = 0)
        assertTrue(outcome is HardwareWiringPresentation.CollectOutcome.Success)
        assertTrue((outcome as HardwareWiringPresentation.CollectOutcome.Success).snapshot.sections.isNotEmpty())
    }

    @Test
    fun shouldToastCollectFailureSkipsCancel() {
        assertFalse(HardwareWiringPresentation.shouldToastCollectFailure(CancellationException()))
        assertTrue(HardwareWiringPresentation.shouldToastCollectFailure(IllegalStateException()))
        assertTrue(HardwareWiringPresentation.shouldToastLocalFailure())
    }
}
