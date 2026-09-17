package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CodeRunSession

object CodeRunHistoryRestore {
    fun apply(session: CodeRunSession, item: HistoryRecord) {
        val meta = CodeRunHistoryMetadata.decode(item.options)
        val runtime = meta?.runtime?.takeIf { it.isNotBlank() }?.let(CodeRunHistoryMetadata::parseRuntime)
            ?: inferRuntimeFromSummary(item.summary)
        applyRuntimeTab(session, runtime)
        session.editor(runtime).setText(item.input, recordUndo = false)
        session.error = ""
    }

    private fun inferRuntimeFromSummary(summary: String): CodeRuntime {
        val prefix = summary.substringBefore('·').trim()
        return CodeRuntime.entries.firstOrNull { CodeRunEngine.displayName(it) == prefix }
            ?: CodeRuntime.Java
    }

    private fun applyRuntimeTab(session: CodeRunSession, runtime: CodeRuntime) {
        when (runtime) {
            CodeRuntime.Groovy -> {
                session.tab = "java"
                session.javaMode = "groovy"
            }
            CodeRuntime.Java -> {
                session.tab = "java"
                session.javaMode = "java"
            }
            CodeRuntime.Python -> session.tab = "python"
            CodeRuntime.Node -> session.tab = "node"
        }
    }
}
