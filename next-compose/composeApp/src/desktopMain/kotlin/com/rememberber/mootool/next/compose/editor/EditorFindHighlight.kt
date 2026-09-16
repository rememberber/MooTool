package com.rememberber.mootool.next.compose.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.rememberber.mootool.next.compose.domain.FindMatch
import javax.swing.SwingUtilities
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import com.rememberber.mootool.next.compose.ui.theme.MooColors
import com.rememberber.mootool.next.compose.ui.theme.toAwtColor

/** RSTA 编辑器内查找命中高亮（对齐 Electron `codeEditorSearchHighlight` / HTTP 响应查找）。 */
internal object EditorFindHighlight {
    fun spans(
        textLength: Int,
        matches: List<FindMatch>,
        selectionStart: Int,
        selectionEnd: Int,
    ): List<Triple<Int, Int, Boolean>> {
        if (matches.isEmpty() || textLength <= 0) return emptyList()
        val current = findMatchAtSelection(matches, selectionStart, selectionEnd)
        return matches.mapNotNull { match ->
            val start = match.start.coerceIn(0, textLength)
            val end = match.end.coerceIn(start, textLength)
            if (end <= start) return@mapNotNull null
            val isCurrent = current != null && match.start == current.start && match.end == current.end
            Triple(start, end, isCurrent)
        }
    }

    fun sync(
        editor: EditorBuffer,
        findOpen: Boolean,
        query: String,
        options: FindReplaceOptions,
        colors: MooColors,
    ) {
        if (!findOpen || query.isBlank()) {
            editor.clearMatches()
            return
        }
        val text = editor.text
        val matches = FindReplace.findAll(text, query, options)
        if (matches.isEmpty()) {
            editor.clearMatches()
            return
        }
        val area = editor.area
        val marked = spans(text.length, matches, area.selectionStart, area.selectionEnd)
        editor.markMatches(
            marked,
            colors.selected.toAwtColor(),
            colors.accent.copy(alpha = 0.45f).toAwtColor(),
        )
    }

    /** 切离工具页时去掉 Swing 高亮，避免 `findOpen` 仍为真时残留。 */
    @Composable
    fun ClearOnDispose(editor: EditorBuffer) {
        DisposableEffect(editor) {
            onDispose {
                val clear = { editor.clearMatches() }
                if (SwingUtilities.isEventDispatchThread()) clear() else SwingUtilities.invokeLater(clear)
            }
        }
    }
}
