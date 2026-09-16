package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.text.input.TextFieldValue
import javax.swing.SwingUtilities

/** 须在 EDT 调用；有非空选区时返回选中文本，供打开查找条时预填（对齐 Electron `openFindReplace`）。 */
internal fun EditorBuffer.selectedTextForFind(): String? {
    val start = area.selectionStart
    val end = area.selectionEnd
    if (end <= start) return null
    val text = text
    return text.substring(start.coerceIn(0, text.length), end.coerceIn(0, text.length))
}

/** Compose 多行文本（如 Host 正文）打开查找时预填选区。 */
internal fun TextFieldValue.selectedTextForFind(): String? {
    val sel = selection
    if (sel.collapsed) return null
    val start = minOf(sel.start, sel.end).coerceIn(0, text.length)
    val end = maxOf(sel.start, sel.end).coerceIn(0, text.length)
    if (end <= start) return null
    return text.substring(start, end).takeIf { it.isNotEmpty() }
}

internal fun openFindBarSeedingSelection(editor: EditorBuffer, open: (seedFromSelection: String?) -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        open(editor.selectedTextForFind())
    } else {
        SwingUtilities.invokeLater { open(editor.selectedTextForFind()) }
    }
}
