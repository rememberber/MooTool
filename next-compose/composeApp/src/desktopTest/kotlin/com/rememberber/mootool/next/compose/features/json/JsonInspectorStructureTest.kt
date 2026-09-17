package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.JsonEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonInspectorStructureTest {
    @Test
    fun utf8ByteLabel_matchesTauriFormat() {
        assertEquals("—", jsonInspectorUtf8ByteLabel(null))
        assertEquals("15 B", jsonInspectorUtf8ByteLabel(15))
    }

    @Test
    fun inferSchemaEnabledOnlyWhenStructureParses() {
        assertFalse(jsonInspectorInferSchemaEnabled(null))
        val analysis = JsonEngine.analyzeStructure("""{"a":1}""") { key, _ -> key }
        assertTrue(jsonInspectorInferSchemaEnabled(analysis))
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
