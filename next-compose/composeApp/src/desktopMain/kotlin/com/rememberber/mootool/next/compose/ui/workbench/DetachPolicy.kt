package com.rememberber.mootool.next.compose.ui.workbench

import com.rememberber.mootool.next.compose.model.ToolId

data class DetachedFocusRequest(val toolId: ToolId, val generation: Int)

object DetachPolicy {
    fun shouldRequestDetachedFocus(toolId: ToolId, detached: Set<ToolId>): Boolean =
        toolId.detachable && toolId in detached

    fun showSidebarWindowAction(collapsed: Boolean, hideTitles: Boolean, detachable: Boolean): Boolean =
        detachable && !collapsed && !hideTitles

    fun recentToolIds(activeId: ToolId, current: List<String>): List<String> =
        if (activeId == ToolId.Mootool) {
            current.filter { it != ToolId.Mootool.id }.take(5)
        } else {
            (listOf(activeId.id) + current.filter { it != activeId.id && it != ToolId.Mootool.id }).take(5)
        }
}
