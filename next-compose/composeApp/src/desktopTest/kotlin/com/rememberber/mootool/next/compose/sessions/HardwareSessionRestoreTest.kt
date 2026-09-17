package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.domain.HardwareTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class HardwareSessionRestoreTest {
    @Test
    fun restoresTabAndRevealSensitiveAndClearsSnapshot() {
        val session = HardwareSession()
        session.tab = HardwareTab.Network
        session.revealSensitive = true
        session.snapshot = com.rememberber.mootool.next.compose.domain.HardwareEngine.collect(loadSampleMs = 1)
        session.loading = true
        session.error = "err"

        session.restore(HardwareSessionSnapshot(tab = "cpu", revealSensitive = false))

        assertEquals(HardwareTab.Cpu, session.tab)
        assertFalse(session.revealSensitive)
        assertNull(session.snapshot)
        assertFalse(session.loading)
        assertEquals("", session.error)
    }
}
