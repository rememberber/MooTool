package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.domain.FindMatch
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.FindReplaceOptions

/** RSTA `EditorBuffer` 查找跳转/替换，对齐 Electron `findAround` / `replaceCurrent`。 */
internal object RstaFindNavigation {
    fun selectionIndex(editor: EditorBuffer, forward: Boolean): Int {
        val area = editor.area
        val start = area.selectionStart
        val end = area.selectionEnd
        if (start == end) return start
        return if (forward) maxOf(start, end) else minOf(start, end)
    }

    fun jump(
        editor: EditorBuffer,
        query: String,
        options: FindReplaceOptions,
        forward: Boolean,
    ): FindMatch? {
        if (query.isBlank()) return null
        return FindReplace.findNext(
            editor.text,
            query,
            options,
            selectionIndex(editor, forward),
            forward,
        )
    }

    /** 单次替换后从替换结果末尾继续选中下一处（无下一处则保留本次替换选区）。 */
    fun replaceAndSelectNext(
        editor: EditorBuffer,
        query: String,
        replacement: String,
        options: FindReplaceOptions,
    ): Boolean {
        if (query.isBlank()) return false
        val area = editor.area
        val (next, replaced) = FindReplace.replaceCurrent(
            editor.text,
            query,
            replacement,
            options,
            selectionIndex(editor, forward = true),
            area.selectionStart,
            area.selectionEnd,
        )
        if (replaced == null) return false
        editor.setText(next, recordUndo = true)
        val following = FindReplace.findNext(next, query, options, replaced.end, forward = true)
        if (following != null) {
            editor.select(following.start, following.end)
        } else {
            editor.select(replaced.start, replaced.end)
        }
        return true
    }
}
