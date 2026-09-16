package com.rememberber.mootool.next.compose.editor

import java.awt.event.InputMethodEvent
import java.text.AttributedString
import javax.swing.JLabel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImeShortcutGateTest {
    @BeforeTest
    fun resetGate() {
        ImeShortcutGate.reset()
    }

    @AfterTest
    fun clearGate() {
        ImeShortcutGate.reset()
    }

    @Test
    fun composingBlocksShortcuts() {
        ImeShortcutGate.setComposingForTest(true)
        assertTrue(ImeShortcutGate.blocksShortcuts())
        ImeShortcutGate.setComposingForTest(false)
        assertFalse(ImeShortcutGate.blocksShortcuts())
    }

    @Test
    fun preeditWithoutCommitKeepsComposing() {
        val source = JLabel()
        val iterator = AttributedString("ni").iterator
        val ime = InputMethodEvent(
            source,
            InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
            iterator,
            0,
            null,
            null
        )
        ImeShortcutGate.onInputMethodEvent(ime)
        assertTrue(ImeShortcutGate.composing)
        assertTrue(ImeShortcutGate.blocksShortcuts())
    }

    @Test
    fun commitSuppressesShortcutsBrieflyThenReleases() {
        val source = JLabel()
        val iterator = AttributedString("你").iterator
        val ime = InputMethodEvent(
            source,
            InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
            iterator,
            1,
            null,
            null
        )
        ImeShortcutGate.onInputMethodEvent(ime)
        assertFalse(ImeShortcutGate.composing)
        assertTrue(ImeShortcutGate.blocksShortcuts(), "commit should suppress leftover Enter/Cmd+Enter")
        Thread.sleep(ImeShortcutGate.COMMIT_SUPPRESS_MS + 40)
        assertFalse(ImeShortcutGate.blocksShortcuts())
    }
}
