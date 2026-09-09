package com.rememberber.mootool.next.compose.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetEngineTest {
    @Test
    fun convertsIpv4AndUnsignedLongFixtures() {
        assertEquals(2130706433L, NetEngine.ipv4ToLong("127.0.0.1"))
        assertEquals("127.0.0.1", NetEngine.longToIpv4("2130706433"))
        assertEquals(0L, NetEngine.ipv4ToLong("0.0.0.0"))
        assertEquals("0.0.0.0", NetEngine.longToIpv4(0L))
        assertEquals(4294967295L, NetEngine.ipv4ToLong("255.255.255.255"))
        assertEquals("255.255.255.255", NetEngine.longToIpv4("4294967295"))
        assertEquals(2130706433L, NetEngine.ipv4ToLong("127.0.0.01"))
        assertFailsWith<NetConvertException> { NetEngine.ipv4ToLong("256.0.0.1") }
        assertFailsWith<NetConvertException> { NetEngine.ipv4ToLong("1.2.3") }
        assertFailsWith<NetConvertException> { NetEngine.ipv4ToLong("1.2.3.4.5") }
        assertFailsWith<NetConvertException> { NetEngine.ipv4ToLong("::1") }
        assertFailsWith<NetConvertException> { NetEngine.ipv4ToLong("127.0.0.") }
        assertFailsWith<NetConvertException> { NetEngine.longToIpv4("-1") }
        assertFailsWith<NetConvertException> { NetEngine.longToIpv4("4294967296") }
        assertFailsWith<NetConvertException> { NetEngine.longToIpv4("1.5") }
    }

    @Test
    fun parsesIpv4RangeAndPortSpec() {
        val range = NetEngine.parseIpv4Range("192.168.10")
        assertEquals(254, range.size)
        assertEquals("192.168.10.1", range.first())
        assertEquals("192.168.10.254", range.last())
        assertEquals(NetEngine.parseIpv4Range("10.0.0."), NetEngine.parseIpv4Range("10.0.0"))
        assertFailsWith<NetException> { NetEngine.parseIpv4Range("999.1.1") }
        assertFailsWith<NetException> { NetEngine.parseIpv4Range("example.com") }

        assertEquals(NetEngine.commonPorts.keys.toList(), NetEngine.parsePortSpec(""))
        assertEquals(listOf(22, 80, 3306), NetEngine.parsePortSpec("22,80,3306"))
        assertEquals((1..5).toList(), NetEngine.parsePortSpec("1-5"))
        assertFailsWith<NetException> { NetEngine.parsePortSpec("0") }
        assertFailsWith<NetException> { NetEngine.parsePortSpec("65536") }
        assertFailsWith<NetException> { NetEngine.parsePortSpec("1-4097") }
    }

    @Test
    fun localAddressesAndLocalhostResolveKeepIpv6() = runBlocking {
        val addresses = NetEngine.localAddresses()
        assertTrue(addresses.ipv4.contains("127.0.0.1") || addresses.ipv6.any { it == "::1" || it.startsWith("::1%") })
        val resolved = NetEngine.run(NetworkAction.Resolve, "localhost", timeoutMs = 5_000)
        assertNull(resolved.errorCode)
        assertTrue(resolved.output.contains("127.0.0.1") || resolved.output.contains("::1"))
        assertTrue(resolved.output.contains("IPv4") || resolved.output.contains("IPv6"))
        assertTrue(!resolved.output.contains("8.8.8.8") || resolved.output.contains("127.0.0.1") || resolved.output.contains("::1"))
    }

    @Test
    fun rejectsIllegalHostsAndDoesNotFillExampleIps() = runBlocking {
        val badHost = NetEngine.run(NetworkAction.Ping, "bad host!", timeoutMs = 3_000)
        assertEquals(NetworkErrorCode.INVALID_TARGET, badHost.errorCode)
        assertEquals("INVALID_TARGET", badHost.output)
        val badWhois = NetEngine.run(NetworkAction.Whois, "你好", timeoutMs = 3_000)
        assertEquals(NetworkErrorCode.INVALID_TARGET, badWhois.errorCode)
        val unknown = NetEngine.run(NetworkAction.Resolve, "this-host-should-not-exist.invalid", timeoutMs = 8_000)
        assertNotEquals(NetworkErrorCode.INVALID_TARGET, unknown.errorCode)
        assertTrue(unknown.errorCode == NetworkErrorCode.COMMAND_FAILED || unknown.errorCode == NetworkErrorCode.TIMEOUT)
        assertTrue(!unknown.output.contains("8.8.8.8"))
        assertTrue(!unknown.output.contains("1.2.3.4"))
    }

    @Test
    fun pingLocalhostCanBeCancelledAndPortScanSeesBoundPort() = runBlocking {
        val handle = NetCommandHandle()
        val ping = async(Dispatchers.IO) {
            NetEngine.run(NetworkAction.Ping, "127.0.0.1", timeoutMs = 30_000, handle = handle)
        }
        delay(150)
        handle.cancel()
        val cancelled = ping.await()
        assertEquals(NetworkErrorCode.ABORTED, cancelled.errorCode)

        val interfaces = NetEngine.run(NetworkAction.Interfaces, timeoutMs = 5_000)
        if (interfaces.errorCode == null) {
            assertTrue(
                interfaces.output.contains("inet") ||
                    interfaces.output.contains("ether") ||
                    interfaces.output.contains("adapter", ignoreCase = true),
                interfaces.output.take(240)
            )
        } else {
            assertTrue(interfaces.output.isNotBlank())
            assertTrue(!interfaces.output.contains("8.8.8.8"))
        }

        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            val port = server.localPort
            val scanned = NetEngine.run(NetworkAction.PortScan, "127.0.0.1", port.toString(), timeoutMs = 4_000)
            assertNull(scanned.errorCode)
            assertTrue(scanned.output.contains("$port/tcp open"), scanned.output)
        } finally {
            server.close()
        }
    }
}
