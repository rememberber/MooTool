package com.rememberber.mootool.next.compose.editor

import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorBufferShortcutTest {
    @Test
    fun menuFindFormatAndSaveStrokesRunCallbacks() {
        var find = 0
        var format = 0
        var save = 0
        val buffer = EditorBuffer("{}")
        buffer.bindAppShortcuts(
            EditorAppShortcuts(
                onFind = { find += 1 },
                onFormat = { format += 1 },
                onSave = { save += 1 }
            )
        )
        val menu = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        val input = buffer.area.getInputMap(JComponent.WHEN_FOCUSED)
        assertEquals(EditorBuffer.ACTION_FIND, input.get(KeyStroke.getKeyStroke(KeyEvent.VK_F, menu)))
        assertEquals(EditorBuffer.ACTION_FIND, input.get(KeyStroke.getKeyStroke(KeyEvent.VK_R, menu)))
        assertEquals(
            EditorBuffer.ACTION_FORMAT,
            input.get(KeyStroke.getKeyStroke(KeyEvent.VK_F, menu or InputEvent.SHIFT_DOWN_MASK))
        )
        assertEquals(EditorBuffer.ACTION_SAVE, input.get(KeyStroke.getKeyStroke(KeyEvent.VK_S, menu)))
        buffer.area.actionMap.get(EditorBuffer.ACTION_FIND).actionPerformed(ActionEvent(buffer.area, 0, "find"))
        buffer.area.actionMap.get(EditorBuffer.ACTION_FORMAT).actionPerformed(ActionEvent(buffer.area, 0, "format"))
        buffer.area.actionMap.get(EditorBuffer.ACTION_SAVE).actionPerformed(ActionEvent(buffer.area, 0, "save"))
        assertEquals(1, find)
        assertEquals(1, format)
        assertEquals(1, save)
    }
}
