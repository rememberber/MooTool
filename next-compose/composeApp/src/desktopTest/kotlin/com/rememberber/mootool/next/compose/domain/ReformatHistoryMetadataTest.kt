package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ReformatHistoryMetadataTest {
    @Test
    fun roundTripsFileTabOptions() {
        val encoded = ReformatHistoryMetadata.encode(ReformatType.Xml, "file", 6, "sample.xml")
        val parsed = ReformatHistoryMetadata.decode(encoded)
        assertEquals(ReformatType.Xml, parsed.type)
        assertEquals("file", parsed.tab)
        assertEquals(6, parsed.indent)
        assertEquals("sample.xml", parsed.fileName)
    }
}
