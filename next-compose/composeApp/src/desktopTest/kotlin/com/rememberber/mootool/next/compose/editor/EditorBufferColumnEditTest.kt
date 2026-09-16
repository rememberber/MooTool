package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.domain.ColumnEditEngine
import com.rememberber.mootool.next.compose.domain.ColumnRange
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputMethodEvent
import java.awt.event.MouseEvent
import java.text.AttributedString
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Assume

class EditorBufferColumnEditTest {
    @Test
    fun columnInsertIsOneUndoStep() {
        val buffer = EditorBuffer("")
        buffer.setText("aa\nbb", recordUndo = false)
        buffer.replaceAllText(ColumnEditEngine.replace(buffer.text, ColumnRange(0, 1, 0, 0), "x", buffer.area.tabSize))
        assertEquals("xaa\nxbb", buffer.text)
        assertTrue(buffer.canUndo())
        buffer.undo()
        assertEquals("aa\nbb", buffer.text)
    }

    @Test
    fun shownComponentAltDragSelectsColumnsAndImeCommitsIntoSelection() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)
        val buffer = EditorBuffer("aa\nbb")
        buffer.columnEdits.enabled = true
        buffer.columnEdits.dragWithoutAlt = true
        val frame = JFrame("next-compose-column-edit")
        frame.contentPane.add(buffer.scrollPane)
        frame.setSize(480, 320)
        try {
            SwingUtilities.invokeAndWait {
                frame.isVisible = true
                buffer.area.font = Font(Font.MONOSPACED, Font.PLAIN, 16)
                buffer.area.size = buffer.area.preferredSize
                buffer.area.validate()
                val start = buffer.area.modelToView2D(0)
                val end = buffer.area.modelToView2D(buffer.area.getLineStartOffset(1) + 1)
                assertNotNull(start)
                assertNotNull(end)
                val press = MouseEvent(
                    buffer.area,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    start.x.toInt() + 1,
                    start.y.toInt() + 2,
                    1,
                    false,
                    MouseEvent.BUTTON1
                )
                val drag = MouseEvent(
                    buffer.area,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    end.x.toInt() + 1,
                    end.y.toInt() + 2,
                    1,
                    false,
                    MouseEvent.BUTTON1
                )
                buffer.area.dispatchEvent(press)
                buffer.area.dispatchEvent(drag)
            }
            val range = buffer.columnEdits.selection
            assertNotNull(range)
            assertEquals(0, range.top)
            assertEquals(1, range.bottom)
            SwingUtilities.invokeAndWait {
                val iterator = AttributedString("中").iterator
                val ime = InputMethodEvent(
                    buffer.area,
                    InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                    iterator,
                    1,
                    null,
                    null
                )
                buffer.area.dispatchEvent(ime)
            }
            assertTrue(buffer.text.contains("中"))
            val lines = buffer.text.split('\n')
            assertEquals(2, lines.size)
            assertTrue(lines[0].contains("中"))
            assertTrue(lines[1].contains("中"))
            try {
                SwingUtilities.invokeAndWait {
                    frame.setLocation(80, 80)
                    frame.toFront()
                }
                Thread.sleep(120)
                val screen = Robot().createScreenCapture(Rectangle(frame.locationOnScreen, frame.size))
                val cwd = File(".").canonicalFile
                val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
                val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
                dir.mkdirs()
                val file = File(dir, "70-column-edit-jframe.png")
                ImageIO.write(screen, "png", file)
            } catch (_: Exception) {
                // Robot 截屏可能被 TCC 拒绝；列编辑/IME 断言仍有效。此帧不能代替产品窗口手势验收。
            }
        } finally {
            ImeShortcutGate.reset()
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }

    @Test
    fun shownComponentColumnTypeWritesEveryLineAndRequestsFocus() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)
        val buffer = EditorBuffer("aa\nbb")
        buffer.columnEdits.enabled = true
        buffer.columnEdits.dragWithoutAlt = true
        val frame = JFrame("next-compose-column-type")
        frame.contentPane.add(buffer.scrollPane)
        frame.setSize(480, 320)
        try {
            SwingUtilities.invokeAndWait {
                frame.isVisible = true
                buffer.area.font = Font(Font.MONOSPACED, Font.PLAIN, 16)
                buffer.area.size = buffer.area.preferredSize
                buffer.area.validate()
                val start = buffer.area.modelToView2D(0)
                val end = buffer.area.modelToView2D(buffer.area.getLineStartOffset(1) + 1)
                assertNotNull(start)
                assertNotNull(end)
                val press = MouseEvent(
                    buffer.area,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    start.x.toInt() + 1,
                    start.y.toInt() + 2,
                    1,
                    false,
                    MouseEvent.BUTTON1
                )
                val drag = MouseEvent(
                    buffer.area,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    end.x.toInt() + 1,
                    end.y.toInt() + 2,
                    1,
                    false,
                    MouseEvent.BUTTON1
                )
                buffer.area.dispatchEvent(press)
                buffer.area.dispatchEvent(drag)
            }
            assertNotNull(buffer.columnEdits.selection)
            repeat(12) {
                SwingUtilities.invokeAndWait { buffer.area.requestFocusInWindow() }
                if (buffer.area.isFocusOwner) return@repeat
                Thread.sleep(40)
            }
            SwingUtilities.invokeAndWait {
                val typed = java.awt.event.KeyEvent(
                    buffer.area,
                    java.awt.event.KeyEvent.KEY_TYPED,
                    System.currentTimeMillis(),
                    0,
                    java.awt.event.KeyEvent.VK_UNDEFINED,
                    'x'
                )
                buffer.area.dispatchEvent(typed)
            }
            val lines = buffer.text.split('\n')
            assertEquals(2, lines.size)
            assertTrue(lines[0].contains("x"), buffer.text)
            assertTrue(lines[1].contains("x"), buffer.text)
        } finally {
            ImeShortcutGate.reset()
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }

    @Test
    fun shownComponentImeCompositionWithoutCommitDoesNotChangeText() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)
        val buffer = EditorBuffer("aa\nbb")
        buffer.columnEdits.enabled = true
        buffer.columnEdits.dragWithoutAlt = true
        val frame = JFrame("next-compose-ime-composition")
        frame.contentPane.add(buffer.scrollPane)
        frame.setSize(480, 320)
        try {
            SwingUtilities.invokeAndWait {
                frame.isVisible = true
                buffer.area.font = Font(Font.MONOSPACED, Font.PLAIN, 16)
                buffer.area.size = buffer.area.preferredSize
                buffer.area.validate()
                val start = buffer.area.modelToView2D(0)
                val end = buffer.area.modelToView2D(buffer.area.getLineStartOffset(1) + 1)
                assertNotNull(start)
                assertNotNull(end)
                val press = MouseEvent(
                    buffer.area,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    start.x.toInt() + 1,
                    start.y.toInt() + 2,
                    1,
                    false,
                    MouseEvent.BUTTON1
                )
                val drag = MouseEvent(
                    buffer.area,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    end.x.toInt() + 1,
                    end.y.toInt() + 2,
                    1,
                    false,
                    MouseEvent.BUTTON1
                )
                buffer.area.dispatchEvent(press)
                buffer.area.dispatchEvent(drag)
            }
            assertNotNull(buffer.columnEdits.selection)
            val before = buffer.text
            SwingUtilities.invokeAndWait {
                val iterator = AttributedString("中").iterator
                val ime = InputMethodEvent(
                    buffer.area,
                    InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                    iterator,
                    0,
                    null,
                    null
                )
                buffer.area.dispatchEvent(ime)
            }
            assertEquals(before, buffer.text)
        } finally {
            ImeShortcutGate.reset()
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }
}

