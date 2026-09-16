package com.rememberber.mootool.next.compose.sessions

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteSessionSnapshotTest {
    @Test
    fun snapshot_roundTrips_vaultTreeOpen() {
        val session = QuickNoteSession()
        session.vaultTreeOpen = false
        val restored = QuickNoteSession().also { it.restore(session.snapshot()) }
        assertEquals(false, restored.vaultTreeOpen)
    }

    @Test
    fun snapshot_roundTrips_vaultSelectedPath() {
        val session = QuickNoteSession()
        session.vaultSelectedPath = "notes/folder"

        val restored = QuickNoteSession().also { it.restore(session.snapshot()) }

        assertEquals("notes/folder", restored.vaultSelectedPath)
    }

    @Test
    fun snapshot_roundTrips_findReplacedCount() {
        val session = QuickNoteSession()
        session.findReplacedCount = 3
        val restored = QuickNoteSession().also { it.restore(session.snapshot()) }
        assertEquals(3, restored.findReplacedCount)
    }
}
