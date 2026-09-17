package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.MessageBoardSession
import com.rememberber.mootool.next.compose.sessions.MessageBoardSessionSnapshot

/** F19 会话恢复（主题/对齐/字号；重载时清空 presenting）。 */
object MessageBoardSessionRestore {
    fun apply(session: MessageBoardSession, snapshot: MessageBoardSessionSnapshot) {
        session.message = MessageBoardEngine.clip(snapshot.message)
        session.theme = MessageBoardEngine.themeId(MessageBoardSessionMetadata.normalizeTheme(snapshot.theme))
        session.alignment = MessageBoardEngine.alignmentId(
            MessageBoardSessionMetadata.normalizeAlignment(snapshot.alignment),
        )
        session.size = MessageBoardSessionMetadata.normalizeSize(snapshot.size)
        session.restored = true
        session.presenting = false
        session.displayAwake = false
        session.notice = ""
        session.error = ""
    }
}
