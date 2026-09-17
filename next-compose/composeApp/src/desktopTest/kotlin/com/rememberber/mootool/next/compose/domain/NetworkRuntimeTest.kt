package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.NetworkSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NetworkRuntimeTest {
    @Test
    fun httpProxyConfigTrimsAndSanitizesPort() {
        val proxy = NetworkSettings(
            proxyEnabled = true,
            proxyHost = " 127.0.0.1 ",
            proxyPort = " 7890 ",
            proxyUsername = " moo ",
            proxyPassword = " secret ",
        ).toHttpProxyConfig()
        assertEquals(true, proxy.enabled)
        assertEquals("127.0.0.1", proxy.host)
        assertEquals("7890", proxy.port)
        assertEquals("moo", proxy.username)
        assertEquals("secret", proxy.password)
    }

    @Test
    fun httpProxyConfigClearsInvalidPortButKeepsEnabledFlag() {
        val proxy = NetworkSettings(
            proxyEnabled = true,
            proxyHost = "proxy.local",
            proxyPort = "70000",
        ).toHttpProxyConfig()
        assertEquals(true, proxy.enabled)
        assertEquals("", proxy.port)
    }

    @Test
    fun httpProxyConfigDisabledWhenToggleOff() {
        val proxy = NetworkSettings(proxyEnabled = false, proxyHost = "127.0.0.1", proxyPort = "8080").toHttpProxyConfig()
        assertFalse(proxy.enabled)
    }
}
