package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.NetSession
import kotlin.test.Test
import kotlin.test.assertEquals

class NetHistoryRestoreTest {
    @Test
    fun restoresIpv4ConversionFields() {
        val session = NetSession()
        NetHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "net",
                operation = NetHistoryMetadata.OP_IPV4_TO_LONG,
                summary = "2130706433",
                input = "127.0.0.1",
                output = "2130706433",
                createdAt = "",
            ),
        )
        assertEquals("127.0.0.1", session.ipv4)
        assertEquals("2130706433", session.longValue)
    }

    @Test
    fun restoresCommandTargetsWithWireOperationAndPortSpec() {
        val session = NetSession()
        val options = NetHistoryMetadata.encodePortSpec("3306,80-82")
        NetHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "net",
                operation = "port-scan",
                summary = "scan",
                input = "127.0.0.1",
                output = "open",
                options = options,
                createdAt = "",
            ),
        )
        assertEquals("127.0.0.1", session.portScanTarget)
        assertEquals("3306,80-82", session.portSpec)
        assertEquals("open", session.output)

        NetHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "net",
                operation = "ping-range",
                summary = "range",
                input = "192.168.10",
                output = "done",
                createdAt = "",
            ),
        )
        assertEquals("192.168.10", session.ipRange)
    }

    @Test
    fun restoresLegacyEnumOptionsWhenOperationWasLocalized() {
        val session = NetSession()
        NetHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "net",
                operation = "PING",
                summary = "PING",
                input = "10.0.0.1",
                output = "ok",
                options = NetworkAction.Ping.name,
                createdAt = "",
            ),
        )
        assertEquals("10.0.0.1", session.pingTarget)
    }
}
