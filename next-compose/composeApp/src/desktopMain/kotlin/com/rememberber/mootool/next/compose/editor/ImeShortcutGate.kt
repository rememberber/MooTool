package com.rememberber.mootool.next.compose.editor

import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.InputMethodEvent
import java.text.CharacterIterator

/**
 * Tracks system IME composition so app/tool shortcuts yield until preedit commits.
 * Enter/Cmd+Enter during composition must not send HTTP or run code.
 */
object ImeShortcutGate {
    const val COMMIT_SUPPRESS_MS = 120L

    @Volatile
    var composing: Boolean = false
        private set

    @Volatile
    private var suppressUntilMs: Long = 0L

    @Volatile
    private var installed = false

    private val listener = AWTEventListener { event ->
        if (event is InputMethodEvent) onInputMethodEvent(event)
    }

    fun blocksShortcuts(): Boolean =
        composing || System.currentTimeMillis() < suppressUntilMs

    fun onInputMethodEvent(event: InputMethodEvent) {
        if (event.id != InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) return
        val text = event.text
        val committed = event.committedCharacterCount
        if (text == null) {
            composing = false
            return
        }
        var remaining = 0
        var index = 0
        var ch = text.first()
        while (ch != CharacterIterator.DONE) {
            if (index >= committed) remaining++
            ch = text.next()
            index++
        }
        composing = remaining > 0
        if (committed > 0) {
            suppressUntilMs = System.currentTimeMillis() + COMMIT_SUPPRESS_MS
        }
    }

    fun install() {
        if (installed) return
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.INPUT_METHOD_EVENT_MASK)
        installed = true
    }

    fun uninstall() {
        if (!installed) return
        Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
        installed = false
    }

    fun reset() {
        composing = false
        suppressUntilMs = 0L
    }

    fun setComposingForTest(value: Boolean) {
        composing = value
        if (!value) suppressUntilMs = 0L
    }
}
