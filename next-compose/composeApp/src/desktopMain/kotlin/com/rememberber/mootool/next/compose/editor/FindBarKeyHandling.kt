package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme

/** 对齐 Electron `FindReplaceBar` 行级快捷键（↑/↓/Esc）。 */
internal enum class FindBarKeyAction {
    FindNext,
    Previous,
    Next,
    Close,
    None,
}

internal fun previewFindBarRowKey(key: Key, type: KeyEventType, imeBlocked: Boolean): FindBarKeyAction {
    if (type != KeyEventType.KeyDown || imeBlocked) return FindBarKeyAction.None
    return when (key) {
        Key.DirectionUp -> FindBarKeyAction.Previous
        Key.DirectionDown -> FindBarKeyAction.Next
        Key.Escape -> FindBarKeyAction.Close
        else -> FindBarKeyAction.None
    }
}

internal fun previewFindQueryEnter(key: Key, type: KeyEventType, imeBlocked: Boolean): FindBarKeyAction {
    if (type != KeyEventType.KeyDown || imeBlocked) return FindBarKeyAction.None
    if (key == Key.Enter || key == Key.NumPadEnter) return FindBarKeyAction.FindNext
    return FindBarKeyAction.None
}

internal fun Modifier.onFindBarRowKeys(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    when (previewFindBarRowKey(event.key, event.type, event.blockedByIme())) {
        FindBarKeyAction.Previous -> {
            onPrevious()
            true
        }
        FindBarKeyAction.Next -> {
            onNext()
            true
        }
        FindBarKeyAction.Close -> {
            onClose()
            true
        }
        else -> false
    }
}

internal fun Modifier.onFindQueryEnterKey(
    onFindNext: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (previewFindQueryEnter(event.key, event.type, event.blockedByIme()) == FindBarKeyAction.FindNext) {
        onFindNext()
        true
    } else {
        false
    }
}
