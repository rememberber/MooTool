package com.rememberber.mootool.next.compose.features.http

import com.rememberber.mootool.next.compose.sessions.HttpSession
import javax.swing.SwingUtilities

/**
 * Electron `HttpTool` 在 `.http-request-pane` 内不触发响应区 `openFind`（URL 行在 pane 外仍可打开）。
 * Compose：`requestPaneComposeFocused` 覆盖 Tabs/键值表；`bodyEditor` 覆盖 Body RSTA（Swing 焦点不进 focusGroup）。
 */
internal object HttpFindShortcutPolicy {
    fun shouldOpenResponseFindFromShell(session: HttpSession): Boolean =
        !session.requestPaneComposeFocused && !requestBodyEditorHasFocus(session)

    private fun requestBodyEditorHasFocus(session: HttpSession): Boolean {
        if (SwingUtilities.isEventDispatchThread()) {
            return session.bodyEditor.area.hasFocus()
        }
        var focused = false
        SwingUtilities.invokeAndWait { focused = session.bodyEditor.area.hasFocus() }
        return focused
    }
}
