package com.rememberber.mootool.next.compose.features.host

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rememberber.mootool.next.compose.domain.FindMatch
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import com.rememberber.mootool.next.compose.editor.findMatchAtSelection

/** Host 正文（Compose `TextFieldValue`）查找跳转，对齐 Electron `findAround`。 */
internal object HostFindNavigation {
    fun currentMatch(matches: List<FindMatch>, field: TextFieldValue): FindMatch? =
        findMatchAtSelection(matches, field.selection.start, field.selection.end)

    fun jump(
        content: String,
        query: String,
        options: FindReplaceOptions,
        field: TextFieldValue,
        forward: Boolean,
    ): TextFieldValue? {
        val selection = field.selection
        val fromIndex = if (forward) {
            maxOf(selection.start, selection.end)
        } else {
            minOf(selection.start, selection.end)
        }
        val match = FindReplace.findNext(content, query, options, fromIndex, forward) ?: return null
        return field.copy(selection = TextRange(match.start, match.end))
    }

    fun afterReplace(
        content: String,
        field: TextFieldValue,
        match: FindMatch?,
    ): TextFieldValue =
        if (match != null) {
            field.copy(text = content, selection = TextRange(match.start, match.end))
        } else {
            field.copy(text = content)
        }

    /** 单次替换后从替换结果末尾继续查找下一处（对齐 Electron `replaceCurrent` + `findNextMatch`）。 */
    fun afterReplaceAndSelectNext(
        content: String,
        query: String,
        options: FindReplaceOptions,
        field: TextFieldValue,
        replaced: FindMatch?,
    ): TextFieldValue {
        val withReplacement = afterReplace(content, field, replaced)
        if (replaced == null) return withReplacement
        val next = FindReplace.findNext(content, query, options, replaced.end, forward = true) ?: return withReplacement
        return withReplacement.copy(selection = TextRange(next.start, next.end))
    }
}
