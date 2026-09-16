package com.rememberber.mootool.next.compose.sessions

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonSessionSnapshotTest {
    @Test
    fun snapshot_roundTrips_inspector_fields() {
        val session = JsonSession()
        session.className = "UserDto"
        session.pathResult = """["a","b"]"""
        session.jsonPath = "$.items"
        session.vaultSelectedPath = "folder/sample.json"
        session.findReplacedCount = 7

        val restored = JsonSession().also { it.restore(session.snapshot()) }

        assertEquals(7, restored.findReplacedCount)
        assertEquals("UserDto", restored.className)
        assertEquals("""["a","b"]""", restored.pathResult)
        assertEquals("$.items", restored.jsonPath)
        assertEquals("folder/sample.json", restored.vaultSelectedPath)
    }
}
