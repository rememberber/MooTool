package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.input.key.Key

/** 对齐 Electron JSON/随手记/Host：`Cmd/Ctrl+F` 与 `Cmd/Ctrl+R`（无 Shift/Alt）打开查找替换。 */
object FindReplaceShortcutPolicy {
    fun opensFindReplace(key: Key, meta: Boolean, shift: Boolean, alt: Boolean): Boolean =
        meta && !shift && !alt && (key == Key.F || key == Key.R)
}
