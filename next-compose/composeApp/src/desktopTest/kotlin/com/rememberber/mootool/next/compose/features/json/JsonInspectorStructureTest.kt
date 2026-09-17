package com.rememberber.mootool.next.compose.features.json

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonInspectorStructureTest {
    @Test
    fun utf8ByteLabel_matchesTauriFormat() {
        assertEquals("—", jsonInspectorUtf8ByteLabel(null))
        assertEquals("15 B", jsonInspectorUtf8ByteLabel(15))
    }

    @Test
    fun duplicateKeys_usesRawTextScanner() {
        val paths = jsonInspectorDuplicateKeys("""{"a":1,"a":2}""", false)
        assertEquals(listOf("$.a"), paths)
    }

    @Test
    fun structureMetricText_usesEmDashForMissing() {
        assertEquals("—", jsonInspectorStructureMetricText(null as Int?))
        assertEquals("Object", jsonInspectorStructureMetricText("Object"))
        assertEquals("—", jsonInspectorStructureMetricText(""))
    }
}
