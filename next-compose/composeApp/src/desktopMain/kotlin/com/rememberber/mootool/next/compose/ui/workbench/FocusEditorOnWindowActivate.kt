package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import com.rememberber.mootool.next.compose.features.json.jsonEditorAutoFocusEnabled
import com.rememberber.mootool.next.compose.features.quicknote.quickNoteEditorAutoFocusEnabled
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.SwingUtilities

/** 窗口获得焦点时把焦点还给 Swing 编辑器（对照 Electron `useFocusOnWindowActivate`）。 */
@Composable
fun FocusEditorOnWindowActivate(enabled: Boolean, onFocus: () -> Unit) {
    val window = LocalAwtWindow.current
    val focus = rememberUpdatedState(onFocus)
    DisposableEffect(window, enabled) {
        if (!enabled || window == null) return@DisposableEffect onDispose {}
        val run = {
            SwingUtilities.invokeLater { focus.value() }
        }
        run()
        val listener = object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent?) = run()
            override fun windowLostFocus(e: WindowEvent?) = Unit
        }
        window.addWindowFocusListener(listener)
        onDispose { window.removeWindowFocusListener(listener) }
    }
}

@Composable
fun JsonEditorWindowFocus(session: JsonSession, toolActive: Boolean) {
    FocusEditorOnWindowActivate(
        enabled = jsonEditorAutoFocusEnabled(session, toolActive),
        onFocus = { session.editor.requestFocus() },
    )
}

@Composable
fun QuickNoteEditorWindowFocus(session: QuickNoteSession, toolActive: Boolean) {
    FocusEditorOnWindowActivate(
        enabled = quickNoteEditorAutoFocusEnabled(session, toolActive),
        onFocus = { session.editor.requestFocus() },
    )
}
