package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtobufHistoryMetadataTest {
    @Test
    fun decodesLegacyPipeAndReEncodesJson() {
        val legacy = "json|jsonToBinary|Person|Hex"
        val meta = ProtobufHistoryMetadata.decode(legacy)!!
        assertEquals("json", meta.tab)
        assertEquals("jsonToBinary", meta.operation)
        assertEquals("Person", meta.messageName)
        assertEquals("Hex", meta.format)
        val json = ProtobufHistoryMetadata.encodeLegacyPipe(legacy)
        assertTrue(json.startsWith("{"))
        assertEquals(meta, ProtobufHistoryMetadata.decode(json))
    }
}
