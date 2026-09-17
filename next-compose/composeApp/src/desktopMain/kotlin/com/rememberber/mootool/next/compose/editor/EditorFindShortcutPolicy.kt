package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.input.key.Key
import com.rememberber.mootool.next.compose.model.ToolId

/**
 * 壳层 `Cmd/Ctrl+F` / `R` 与 [EditorFindToolScope] 对齐：Electron JSON/随手记/Host 双键；HTTP 仅 F；
 * F05 为 Compose 扩展，仍走 [FindReplaceShortcutPolicy] 但只读查找条。
 */
object EditorFindShortcutPolicy {
    fun opensShellFind(
        toolId: ToolId,
        key: Key,
        meta: Boolean,
        shift: Boolean,
        alt: Boolean,
    ): Boolean {
        if (!EditorFindToolScope.supportsComposeFind(toolId)) return false
        if (toolId == ToolId.Http) {
            return meta && !shift && !alt && key == Key.F
        }
        return FindReplaceShortcutPolicy.opensFindReplace(key, meta, shift, alt)
    }
}
