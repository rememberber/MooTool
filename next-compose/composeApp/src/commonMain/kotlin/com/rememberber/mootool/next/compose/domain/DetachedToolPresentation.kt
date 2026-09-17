package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.ToolId

/** 分离工具窗标题与主窗占位（对齐 Electron `detached-tool-placeholder` / `tool-view-shell--detached`）。 */
object DetachedToolPresentation {
    fun windowTitle(toolLabel: String, productName: String): String = "$toolLabel · $productName"

    fun showMainWindowPlaceholder(toolId: ToolId, detached: Set<ToolId>): Boolean =
        toolId.detachable && toolId in detached
}
