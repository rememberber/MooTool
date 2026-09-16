package com.rememberber.mootool.next.compose.sessions

import kotlin.test.Test
import kotlin.test.assertEquals

class HostSessionSnapshotTest {
    @Test
    fun snapshot_roundTrips_findReplacedCount() {
        val session = HostSession()
        session.findReplacedCount = 5
        val restored = HostSession().also { it.restore(session.snapshotState()) }
        assertEquals(5, restored.findReplacedCount)
    }
}
