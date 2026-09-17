package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.model.ToolId

/** 对照 Electron 与 Compose 壳层 / RSTA 查找快捷键覆盖范围。 */
object EditorFindToolScope {
    /** Electron `JsonTool` / `QuickNoteTool` / `HostTool` / HTTP 响应查找。 */
    val electronShellFindTools: Set<ToolId> = setOf(
        ToolId.Json,
        ToolId.QuickNote,
        ToolId.Host,
        ToolId.Http,
    )

    /** Compose 额外为 F05 源码 RSTA 接 `EditorFindOnlyBar`（Electron Runtime 无同等查找条）。 */
    val composeEditorFindTools: Set<ToolId> = electronShellFindTools + ToolId.Java

    fun supportsElectronFind(toolId: ToolId): Boolean = toolId in electronShellFindTools

    fun supportsComposeFind(toolId: ToolId): Boolean = toolId in composeEditorFindTools
}
