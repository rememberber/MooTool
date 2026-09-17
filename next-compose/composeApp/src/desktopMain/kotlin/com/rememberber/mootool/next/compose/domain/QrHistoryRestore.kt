package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.QrSession
import java.util.Base64

/** F19 QR 历史恢复（对齐 Electron `QrCodeTool` history click：生成恢复 data URL/重算 PNG）。 */
object QrHistoryRestore {
    fun apply(session: QrSession, item: HistoryRecord) {
        val meta = QrHistoryMetadata.decode(item.options)
        session.error = ""
        session.notice = ""
        if (meta?.operation == "recognize") {
            session.tab = QrTab.Recognize
            session.recognitionName = item.input
            session.recognitionResult = item.output
            return
        }
        session.tab = QrTab.Generate
        session.content = item.input
        session.size = meta?.size?.let { QrEngine.normalizeSize(it) } ?: session.size
        session.correction =
            QrErrorCorrection.entries.find { it.name == meta?.correction } ?: session.correction
        session.pngBytes = pngBytesFromHistoryOutput(item.output, session.content, session.size, session.correction)
    }

    internal fun pngBytesFromHistoryOutput(
        output: String,
        content: String,
        size: Int,
        correction: QrErrorCorrection,
    ): ByteArray? {
        decodeDataUrlPng(output)?.let { return it }
        if (content.isBlank()) return null
        return runCatching { QrEngine.generatePng(content, size, correction, logo = null) }.getOrNull()
    }

    private fun decodeDataUrlPng(output: String): ByteArray? {
        val trimmed = output.trim()
        val prefix = "data:image/png;base64,"
        if (!trimmed.startsWith(prefix, ignoreCase = true)) return null
        val payload = trimmed.substring(prefix.length)
        return runCatching { Base64.getDecoder().decode(payload) }.getOrNull()
    }
}
