package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NetHistoryMetadataTest {
    @Test
    fun wireIdsMatchElectronNetworkActions() {
        assertEquals("ping-range", NetHistoryMetadata.actionWireId(NetworkAction.PingRange))
        assertEquals("port-scan", NetHistoryMetadata.actionWireId(NetworkAction.PortScan))
        assertEquals("flush-dns", NetHistoryMetadata.actionWireId(NetworkAction.FlushDns))
    }

    @Test
    fun roundTripsPortSpecOptions() {
        val encoded = NetHistoryMetadata.encodePortSpec("22,80-82")
        assertEquals("22,80-82", NetHistoryMetadata.decodePortSpec(encoded))
        assertEquals("", NetHistoryMetadata.decodePortSpec(""))
    }

    @Test
    fun parsesWireAndLegacyEnumActions() {
        assertEquals(NetworkAction.Resolve, NetHistoryMetadata.parseAction("resolve", ""))
        assertEquals(NetworkAction.Whois, NetHistoryMetadata.parseAction("", NetworkAction.Whois.name))
    }
}
