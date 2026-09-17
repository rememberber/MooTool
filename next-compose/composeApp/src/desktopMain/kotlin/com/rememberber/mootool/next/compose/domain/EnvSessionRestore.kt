package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.VariablesSession
import com.rememberber.mootool.next.compose.sessions.VariablesSessionSnapshot

/** F08 会话恢复（tab/scope/搜索；重载时清空 snapshot 与 transient 状态）。 */
object EnvSessionRestore {
    fun apply(session: VariablesSession, snapshot: VariablesSessionSnapshot) {
        val tabWire = EnvSessionMetadata.normalizeTab(snapshot.tab)
        session.tab = when (tabWire) {
            EnvSessionMetadata.TAB_RUNTIME -> EnvTab.Runtime
            else -> EnvTab.Environment
        }
        val scopeWire = EnvSessionMetadata.normalizeScope(snapshot.scope)
        session.scope = when (scopeWire) {
            EnvSessionMetadata.SCOPE_USER -> EnvDisplayScope.User
            EnvSessionMetadata.SCOPE_SYSTEM -> EnvDisplayScope.System
            else -> EnvDisplayScope.Process
        }
        session.query = snapshot.query
        session.snapshot = null
        session.loading = false
        session.saving = false
        session.error = ""
        session.notice = ""
        session.editorOpen = false
        session.deleteKey = ""
        session.lastBackup = ""
        session.lastDiff = ""
    }
}
