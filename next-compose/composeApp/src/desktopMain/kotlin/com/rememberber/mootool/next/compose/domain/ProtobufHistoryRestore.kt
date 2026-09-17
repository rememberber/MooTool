package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ProtobufSession

object ProtobufHistoryRestore {
    fun apply(session: ProtobufSession, item: HistoryRecord) {
        val meta = ProtobufHistoryMetadata.decode(item.options)
        if (meta == null) {
            applyFallback(session, item)
            return
        }
        if (meta.tab in listOf("json", "wire", "convert")) session.tab = meta.tab
        if (meta.messageName.isNotEmpty() && meta.tab == "json") session.messageName = meta.messageName
        if (meta.format == "Hex" || meta.format == "Base64") {
            val format = ProtobufSession.formatOf(meta.format)
            if (meta.tab == "wire") session.wireFormat = format else session.format = format
        }
        when (meta.operation) {
            "jsonToBinary" -> {
                session.json = item.input
                session.binary = item.output
            }
            "binaryToJson" -> {
                session.binary = item.input
                session.json = item.output
            }
            "format" -> session.proto = item.output
            "decode" -> {
                session.wireInput = item.input
                session.wireOutput = item.output
            }
            "hexToBase64" -> {
                session.hex = item.input
                session.base64 = item.output
            }
            "base64ToHex" -> {
                session.base64 = item.input
                session.hex = item.output
            }
        }
        session.error = ""
    }

    private fun applyFallback(session: ProtobufSession, item: HistoryRecord) {
        session.proto = item.output.ifBlank { item.input }
        session.error = ""
    }
}
