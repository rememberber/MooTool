package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.HardwareSession
import com.rememberber.mootool.next.compose.sessions.HardwareSessionSnapshot

/** F25 会话恢复（Tab / 序列号显示；重载时清空 snapshot）。 */
object HardwareSessionRestore {
    fun apply(session: HardwareSession, snapshot: HardwareSessionSnapshot) {
        val tabWire = HardwareSessionMetadata.normalizeTab(snapshot.tab)
        session.tab = HardwareTab.entries.find { it.name.equals(tabWire, ignoreCase = true) } ?: HardwareTab.System
        session.revealSensitive = snapshot.revealSensitive
        session.snapshot = null
        session.loading = false
        session.error = ""
    }
}
