package com.rememberber.mootool.next.compose.ui.workbench

import com.rememberber.mootool.next.compose.domain.DetachedToolPresentation
import com.rememberber.mootool.next.compose.model.ToolId

object WorkbenchNavPresentation {
    fun showDetachedPlaceholder(active: ToolId, detached: Set<ToolId>): Boolean =
        DetachedToolPresentation.showMainWindowPlaceholder(active, detached)

    fun recentToolIds(activeId: ToolId, current: List<String>): List<String> =
        DetachPolicy.recentToolIds(activeId, current)

    fun showSidebarDetachAction(collapsed: Boolean, hideTitles: Boolean, detachable: Boolean): Boolean =
        DetachPolicy.showSidebarWindowAction(collapsed, hideTitles, detachable)
}
