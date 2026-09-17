package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetWiringPresentationTest {
    @Test
    fun portScanStartTrimsAndValidates() {
        val ready = NetWiringPresentation.portScanStart(" 127.0.0.1 ", " 22,80 ")
        assertTrue(ready is NetWiringPresentation.PortScanStart.Ready)
        ready as NetWiringPresentation.PortScanStart.Ready
        assertEquals("127.0.0.1", ready.target)
        assertEquals("22,80", ready.portSpec)
    }

    @Test
    fun pingStartTrimsAndValidatesHost() {
        val ready = NetWiringPresentation.pingStart(" example.com ")
        assertTrue(ready is NetWiringPresentation.HostCommandStart.Ready)
        ready as NetWiringPresentation.HostCommandStart.Ready
        assertEquals("example.com", ready.target)
        assertTrue(NetWiringPresentation.pingStart("") is NetWiringPresentation.HostCommandStart.Blocked)
    }

    @Test
    fun portScanStartRejectsEmptyAndInvalid() {
        assertTrue(
            NetWiringPresentation.portScanStart("", "22") is NetWiringPresentation.PortScanStart.Blocked,
        )
        assertTrue(
            NetWiringPresentation.portScanStart("127.0.0.1", "") is NetWiringPresentation.PortScanStart.Blocked,
        )
        assertTrue(
            NetWiringPresentation.portScanStart("127.0.0.1", "99999")
                is NetWiringPresentation.PortScanStart.Blocked,
        )
    }

    @Test
    fun outputAndRunGuards() {
        assertTrue(NetWiringPresentation.stopEnabled(running = true))
        assertFalse(NetWiringPresentation.outputActionsEnabled(outputNotBlank = false))
        assertTrue(
            NetWiringPresentation.runCommandEnabled(
                idle = true,
                startReady = NetWiringPresentation.pingStart("127.0.0.1")
                    is NetWiringPresentation.HostCommandStart.Ready,
            ),
        )
        assertFalse(NetWiringPresentation.runCommandEnabled(idle = false, startReady = true))
    }

    @Test
    fun runLocalAddressesReturnsSnapshot() {
        val snapshot = NetWiringPresentation.runLocalAddresses()
        assertTrue(snapshot.ipv4.isNotEmpty() || snapshot.ipv6.isNotEmpty())
    }
}
