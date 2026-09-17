package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CronHistoryMetadataTest {
    @Test
    fun encodesAndParsesElectronStyleJson() {
        val encoded = CronHistoryMetadata.encodeTimeZone("Asia/Shanghai")
        assertEquals("Asia/Shanghai", CronHistoryMetadata.parseTimeZone(encoded))
        assertEquals("UTC", CronHistoryMetadata.parseTimeZone("""{"timeZone":"UTC"}"""))
    }

    @Test
    fun acceptsLegacyPlainZoneInOptions() {
        assertEquals("America/New_York", CronHistoryMetadata.parseTimeZone("America/New_York"))
        assertNull(CronHistoryMetadata.parseTimeZone(""))
    }
}
