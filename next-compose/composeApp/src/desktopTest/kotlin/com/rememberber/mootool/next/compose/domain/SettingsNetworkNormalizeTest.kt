package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsNetworkNormalizeTest {
    @Test
    fun applyTrimsProxyFieldsAndDropsInvalidPort() {
        val raw = AppSettings.Default.copy(
            network = AppSettings.Default.network.copy(
                proxyHost = " 127.0.0.1 ",
                proxyPort = " 70000 ",
                proxyUsername = " moo ",
                proxyPassword = " secret ",
            ),
        )
        val normalized = SettingsNetworkNormalize.apply(raw)
        assertEquals("127.0.0.1", normalized.network.proxyHost)
        assertEquals("", normalized.network.proxyPort)
        assertEquals("moo", normalized.network.proxyUsername)
        assertEquals("secret", normalized.network.proxyPassword)
    }

    @Test
    fun commitProxyPortMatchesSettingsBlurSemantics() {
        assertEquals(ProxyPortCommitResult.Cleared, SettingsNetworkNormalize.commitProxyPort("   "))
        assertEquals(
            ProxyPortCommitResult.Accepted("7890"),
            SettingsNetworkNormalize.commitProxyPort(" 7890 "),
        )
        assertEquals(ProxyPortCommitResult.Rejected, SettingsNetworkNormalize.commitProxyPort("0"))
        assertEquals(ProxyPortCommitResult.Rejected, SettingsNetworkNormalize.commitProxyPort("70000"))
        assertEquals(ProxyPortCommitResult.Rejected, SettingsNetworkNormalize.commitProxyPort("abc"))
    }

    @Test
    fun commitTimeoutMsClampsToSupportedBounds() {
        assertEquals(
            TimeoutCommitResult.Accepted(1_000),
            SettingsNetworkNormalize.commitTimeoutMs("50", 1_000, 120_000),
        )
        assertEquals(
            TimeoutCommitResult.Accepted(120_000),
            SettingsNetworkNormalize.commitTimeoutMs("999999", 1_000, 120_000),
        )
        assertEquals(
            TimeoutCommitResult.Accepted(30_000),
            SettingsNetworkNormalize.commitTimeoutMs(" 30000 ", 1_000, 120_000),
        )
        assertEquals(
            TimeoutCommitResult.Rejected,
            SettingsNetworkNormalize.commitTimeoutMs("not-a-number", 1_000, 120_000),
        )
    }
}
