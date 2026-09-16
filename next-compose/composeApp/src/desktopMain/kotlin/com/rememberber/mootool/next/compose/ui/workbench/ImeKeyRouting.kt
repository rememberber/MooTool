package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.rememberber.mootool.next.compose.editor.ImeShortcutGate

fun KeyEvent.blockedByIme(): Boolean =
    type == KeyEventType.KeyDown && ImeShortcutGate.blocksShortcuts()
