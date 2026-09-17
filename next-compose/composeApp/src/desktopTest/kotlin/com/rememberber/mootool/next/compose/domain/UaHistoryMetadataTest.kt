package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class UaHistoryMetadataTest {
    @Test
    fun encode_returnsElectronMarker() {
        assertEquals("UaParse", UaHistoryMetadata.encode())
        assertEquals(UaHistoryMetadata.MARKER, UaHistoryMetadata.encode())
    }
}
