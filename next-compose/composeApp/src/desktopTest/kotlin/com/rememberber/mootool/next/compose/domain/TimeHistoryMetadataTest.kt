package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeHistoryMetadataTest {
    @Test
    fun encodesAndDecodesZoneAndUnit() {
        val json = TimeHistoryMetadata.encode("Asia/Shanghai", TimestampUnit.Millisecond)
        val meta = TimeHistoryMetadata.decode(json)!!
        assertEquals("Asia/Shanghai", meta.zone)
        assertEquals(TimestampUnit.Millisecond, TimeHistoryMetadata.unitFromMeta(meta))
    }

    @Test
    fun decodeBlankReturnsNull() {
        assertNull(TimeHistoryMetadata.decode(""))
        assertNull(TimeHistoryMetadata.decode("   "))
    }
}
