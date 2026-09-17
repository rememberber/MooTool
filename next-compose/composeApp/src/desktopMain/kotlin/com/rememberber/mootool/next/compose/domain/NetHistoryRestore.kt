package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.NetSession

object NetHistoryRestore {
    fun apply(session: NetSession, item: HistoryRecord) {
        session.output = item.output
        session.error = ""
        when (item.operation) {
            NetHistoryMetadata.OP_IPV4_TO_LONG -> {
                session.ipv4 = item.input
                session.longValue = item.output
                return
            }
            NetHistoryMetadata.OP_LONG_TO_IPV4 -> {
                session.longValue = item.input
                session.ipv4 = item.output
                return
            }
        }
        if (item.input.isBlank()) return
        when (NetHistoryMetadata.parseAction(item.operation, item.options)) {
            NetworkAction.Ping -> session.pingTarget = item.input
            NetworkAction.PingRange -> session.ipRange = item.input
            NetworkAction.PortScan -> {
                session.portScanTarget = item.input
                session.portSpec = NetHistoryMetadata.decodePortSpec(item.options)
            }
            NetworkAction.Resolve -> session.hostTarget = item.input
            NetworkAction.Whois -> session.whoisTarget = item.input
            else -> Unit
        }
    }
}
