package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CodeRunSessionSnapshot
import com.rememberber.mootool.next.compose.sessions.SessionManager
import com.rememberber.mootool.next.compose.sessions.TimeSession
import com.rememberber.mootool.next.compose.storage.HistoryRepository

/**
 * Applies Java-edition `t_func_content` drafts (imported as `operation = draft`) into live tool sessions.
 * History rows still go through [com.rememberber.mootool.next.compose.storage.HistoryRepository].
 */
object LegacyToolDraftApplier {
    fun apply(
        sessionManager: SessionManager,
        rows: List<ImportedLegacyHistory>,
        history: HistoryRepository? = null,
    ): Int {
        val drafts = rows.filter { it.operation == "draft" && it.input.isNotBlank() }
        if (drafts.isEmpty()) return 0

        var applied = 0
        var codeSnapshot = sessionManager.codeRunSession().snapshotState()
        var codeChanged = false
        drafts.forEach { row ->
            val runtimeKey = codeRuntimeDraftKey(row.legacyFunc) ?: return@forEach
            codeSnapshot = patchCodeRunDraft(codeSnapshot, runtimeKey, row.input)
            codeChanged = true
            applied++
        }
        if (codeChanged) {
            sessionManager.codeRunSession().restore(codeSnapshot)
            sessionManager.persistCodeRun()
        }

        applied += applyLast(drafts, "regex") { row ->
            sessionManager.regexSession().source = clip(row.input)
            sessionManager.persistRegex()
        }
        applied += applyLast(drafts, "jsonbeauty", "json") { row ->
            val session = sessionManager.jsonSession()
            val text = clip(row.input)
            session.editor.setText(text, recordUndo = false)
            session.savedText = text
            sessionManager.persistJson()
        }
        applied += applyLast(drafts, "textdiffleft") { row ->
            val session = sessionManager.diffSession()
            session.left = clip(row.input)
            session.result = DiffEngine.compare(session.left, session.right, session.ignoreWhitespace)
            sessionManager.persistDiff()
        }
        applied += applyLast(drafts, "textdiffright") { row ->
            val session = sessionManager.diffSession()
            session.right = clip(row.input)
            session.result = DiffEngine.compare(session.left, session.right, session.ignoreWhitespace)
            sessionManager.persistDiff()
        }
        applied += applyLast(drafts, "timeconvert") { row ->
            applyTimeDraft(sessionManager.timeSession(), row.input, history)
            sessionManager.persistTime()
        }
        applied += applyLast(drafts, "calculator") { row ->
            LegacyCalculatorDraft.apply(sessionManager.calculatorSession(), row.input)
            sessionManager.persistCalculator()
        }
        applied += applyLast(drafts, "qrcode", "qr") { row ->
            val content = LegacyConsoleDraft.extractQrGenerateContent(row.input)
                ?: row.input.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
                ?: row.input.trim()
            sessionManager.qrSession().content = content
            sessionManager.persistQr()
        }

        return applied
    }

    private fun applyLast(drafts: List<ImportedLegacyHistory>, vararg keys: String, block: (ImportedLegacyHistory) -> Unit): Int {
        val wanted = keys.map { it.lowercase() }.toSet()
        val row = drafts.lastOrNull { normalizedFunc(it.legacyFunc) in wanted } ?: return 0
        block(row)
        return 1
    }

    private fun normalizedFunc(legacyFunc: String): String =
        legacyFunc.trim().lowercase().replace("_", "").replace("-", "")

    private fun clip(text: String): String = text.take(CodeRunEngine.MAX_CODE_BYTES)

    private fun codeRuntimeDraftKey(legacyFunc: String): String? {
        val compact = normalizedFunc(legacyFunc)
        return when (compact) {
            "java", "javaconsole", "coderun" -> "java"
            "groovy" -> "groovy"
            "python" -> "python"
            "node", "nodejs" -> "node"
            else -> null
        }
    }

    private fun patchCodeRunDraft(snapshot: CodeRunSessionSnapshot, key: String, content: String): CodeRunSessionSnapshot {
        val text = clip(content)
        return when (key) {
            "java" -> snapshot.copy(javaCode = text)
            "groovy" -> snapshot.copy(groovyCode = text)
            "python" -> snapshot.copy(pythonCode = text)
            "node" -> snapshot.copy(nodeCode = text)
            else -> snapshot
        }
    }

    private fun applyTimeDraft(session: TimeSession, content: String, history: HistoryRepository?) {
        val parsed = LegacyTimeConvertDraft.parse(content)
        if (parsed.isNotEmpty()) {
            parsed.forEach { entry ->
                history?.save(
                    ToolId.TimeConvert.id,
                    entry.summary,
                    entry.summary,
                    entry.input,
                    entry.output,
                    LegacyTimeConvertDraft.optionsJson(entry.zone, entry.unit),
                )
            }
            applyParsedTimeEntry(session, parsed.last())
            return
        }
        val line = content.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return
        if (line.all { it.isDigit() || (line.startsWith("-") && line.drop(1).all { it.isDigit() }) }) {
            session.timestamp = line
            session.unit = if (line.length >= 13) TimestampUnit.Millisecond else TimestampUnit.Second
        } else {
            session.localTime = line
        }
    }

    private fun applyParsedTimeEntry(session: TimeSession, entry: LegacyTimeConvertDraft.ParsedEntry) {
        session.zone = entry.zone.ifBlank { session.zone }
        session.unit = if (entry.unit == "millisecond") TimestampUnit.Millisecond else TimestampUnit.Second
        val input = entry.input.trim()
        val output = entry.output.trim()
        if (input.all { it.isDigit() || (input.startsWith("-") && input.drop(1).all { it.isDigit() }) }) {
            session.timestamp = input
            if (output.isNotEmpty()) session.localTime = output
        } else {
            session.localTime = input
            session.timestamp = output
        }
    }

}
