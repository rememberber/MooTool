package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** F11 通用历史 `operation` / `options`（对齐 Electron `NetworkAction` 字符串）。 */
object NetHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    const val OP_IPV4_TO_LONG = "ipv4-to-long"
    const val OP_LONG_TO_IPV4 = "long-to-ipv4"

    fun actionWireId(action: NetworkAction): String = when (action) {
        NetworkAction.Interfaces -> "interfaces"
        NetworkAction.Connections -> "connections"
        NetworkAction.Ping -> "ping"
        NetworkAction.PingRange -> "ping-range"
        NetworkAction.PortScan -> "port-scan"
        NetworkAction.FlushDns -> "flush-dns"
        NetworkAction.Resolve -> "resolve"
        NetworkAction.Whois -> "whois"
    }

    fun parseAction(operation: String, options: String): NetworkAction? {
        val op = operation.trim()
        if (op.isNotEmpty()) {
            wireToAction(op)?.let { return it }
            enumValueOrNull<NetworkAction>(op)?.let { return it }
        }
        val fromOptions = options.trim()
        if (fromOptions.isNotEmpty()) {
            wireToAction(fromOptions)?.let { return it }
            enumValueOrNull<NetworkAction>(fromOptions)?.let { return it }
        }
        return null
    }

    fun encodePortSpec(portSpec: String?): String {
        val ports = portSpec?.trim().orEmpty()
        if (ports.isEmpty()) return ""
        return json.encodeToString(NetHistoryMeta(ports = ports))
    }

    fun decodePortSpec(options: String): String {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return ""
        return runCatching { json.decodeFromString<NetHistoryMeta>(trimmed).ports.trim() }.getOrDefault("")
    }

    private fun wireToAction(wire: String): NetworkAction? = when (wire) {
        "interfaces" -> NetworkAction.Interfaces
        "connections" -> NetworkAction.Connections
        "ping" -> NetworkAction.Ping
        "ping-range" -> NetworkAction.PingRange
        "port-scan" -> NetworkAction.PortScan
        "flush-dns" -> NetworkAction.FlushDns
        "resolve" -> NetworkAction.Resolve
        "whois" -> NetworkAction.Whois
        else -> null
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
        runCatching { enumValueOf<T>(name) }.getOrNull()
}

@Serializable
private data class NetHistoryMeta(val ports: String = "")
