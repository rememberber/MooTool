package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings

/** Trims network proxy fields on load/import; blur commit helpers align with Electron `TextSetting`. */
object SettingsNetworkNormalize {
    private const val MAX_PROXY_HOST = 256
    private const val MAX_PROXY_USER = 128
    private const val MAX_PROXY_PASSWORD = 512

    fun apply(settings: AppSettings): AppSettings {
        val network = settings.network
        return settings.copy(
            network = network.copy(
                proxyHost = network.proxyHost.trim().take(MAX_PROXY_HOST),
                proxyPort = sanitizeProxyPort(network.proxyPort),
                proxyUsername = network.proxyUsername.trim().take(MAX_PROXY_USER),
                proxyPassword = network.proxyPassword.trim().take(MAX_PROXY_PASSWORD),
            ),
        )
    }

    fun sanitizeProxyPort(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.toIntOrNull()?.takeIf { it in 1..65_535 }?.toString() ?: ""
    }

    fun commitProxyPort(raw: String): ProxyPortCommitResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ProxyPortCommitResult.Cleared
        val port = trimmed.toIntOrNull() ?: return ProxyPortCommitResult.Rejected
        if (port !in 1..65_535) return ProxyPortCommitResult.Rejected
        return ProxyPortCommitResult.Accepted(port.toString())
    }

    fun commitTimeoutMs(raw: String, minimum: Int, maximum: Int): TimeoutCommitResult {
        val trimmed = raw.trim()
        val parsed = trimmed.toIntOrNull() ?: return TimeoutCommitResult.Rejected
        return TimeoutCommitResult.Accepted(SettingsNumericBounds.clampNumber(parsed, minimum, maximum))
    }
}

sealed class ProxyPortCommitResult {
    data object Cleared : ProxyPortCommitResult()

    data class Accepted(val port: String) : ProxyPortCommitResult()

    data object Rejected : ProxyPortCommitResult()
}

sealed class TimeoutCommitResult {
    data class Accepted(val milliseconds: Int) : TimeoutCommitResult()

    data object Rejected : TimeoutCommitResult()
}
