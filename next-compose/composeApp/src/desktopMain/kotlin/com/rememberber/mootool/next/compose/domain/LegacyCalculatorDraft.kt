package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.CalculatorSession

object LegacyCalculatorDraft {
    private val arithmetic = Regex("""^(.+?)\s*=\s*([^=]+)$""")
    private val decFromHex = Regex("""^DEC\(([^)]+)\)\s*=\s*(\d+)$""", RegexOption.IGNORE_CASE)
    private val hexFromDec = Regex("""^HEX\((\d+)\)\s*=\s*([0-9a-fA-F]+)$""", RegexOption.IGNORE_CASE)
    private val binFromDec = Regex("""^BIN\((\d+)\)\s*=\s*([01]+)$""", RegexOption.IGNORE_CASE)
    private val decFromBin = Regex("""^DEC\(([01]+)\)\s*=\s*(\d+)$""", RegexOption.IGNORE_CASE)

    fun apply(session: CalculatorSession, raw: String) {
        val messages = LegacyConsoleDraft.extractMessages(raw)
        if (messages.isNotEmpty()) {
            session.log = messages.asReversed().take(12)
            applyFromMessage(session, messages.last())
            return
        }
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        session.log = lines
        applyFromMessage(session, lines.last())
    }

    private fun applyFromMessage(session: CalculatorSession, message: String) {
        val line = message.lineSequence().lastOrNull { it.isNotBlank() }?.trim() ?: return
        decFromHex.matchEntire(line)?.let { match ->
            session.hex = match.groupValues[1].trim()
            session.decimal = match.groupValues[2].trim()
            session.result = match.groupValues[2].trim()
            return
        }
        hexFromDec.matchEntire(line)?.let { match ->
            session.decimal = match.groupValues[1].trim()
            session.hex = match.groupValues[2].trim()
            session.result = match.groupValues[2].trim()
            return
        }
        binFromDec.matchEntire(line)?.let { match ->
            session.decimal = match.groupValues[1].trim()
            session.binary = match.groupValues[2].trim()
            session.result = match.groupValues[2].trim()
            return
        }
        decFromBin.matchEntire(line)?.let { match ->
            session.binary = match.groupValues[1].trim()
            session.decimal = match.groupValues[2].trim()
            session.result = match.groupValues[2].trim()
            return
        }
        arithmetic.matchEntire(line)?.let { match ->
            val expr = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            if (expr.isNotEmpty()) session.expression = expr
            if (value.isNotEmpty()) session.result = value
        }
    }
}
