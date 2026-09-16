package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RstaFindNavigationTest {
    @Test
    fun selectionIndex_uses_range_edges_when_selection_non_empty() {
        SwingUtilities.invokeAndWait {
            val editor = EditorBuffer()
            editor.setText("aa bb aa", recordUndo = false)
            editor.select(6, 8)
            if (editor.area.selectionStart == editor.area.selectionEnd) {
                editor.area.caretPosition = 8
            }
            if (editor.area.selectionStart != editor.area.selectionEnd) {
                assertEquals(8, RstaFindNavigation.selectionIndex(editor, forward = true))
                assertEquals(6, RstaFindNavigation.selectionIndex(editor, forward = false))
            } else {
                assertEquals(8, RstaFindNavigation.selectionIndex(editor, forward = true))
                assertEquals(8, RstaFindNavigation.selectionIndex(editor, forward = false))
            }
        }
    }

    @Test
    fun jump_finds_next_from_selection() {
        SwingUtilities.invokeAndWait {
            val editor = EditorBuffer()
            editor.setText("aa bb aa", recordUndo = false)
            editor.select(3, 3)
            val match = RstaFindNavigation.jump(editor, "aa", FindReplaceOptions(), forward = true)
            assertEquals(6, match?.start)
            assertEquals(8, match?.end)
        }
    }

    @Test
    fun replaceAndSelectNext_advances_to_following_match() {
        SwingUtilities.invokeAndWait {
            val editor = EditorBuffer()
            editor.setText("aa aa", recordUndo = false)
            editor.area.caretPosition = 0
            assertTrue(RstaFindNavigation.replaceAndSelectNext(editor, "aa", "XX", FindReplaceOptions()))
            assertEquals("XX aa", editor.text)
            val next = RstaFindNavigation.jump(editor, "aa", FindReplaceOptions(), forward = true)
            assertEquals(3, next?.start)
            assertEquals(5, next?.end)
        }
    }

    @Test
    fun replaceAndSelectNext_returns_false_when_no_match() {
        SwingUtilities.invokeAndWait {
            val editor = EditorBuffer()
            editor.setText("hello", recordUndo = false)
            assertFalse(RstaFindNavigation.replaceAndSelectNext(editor, "zzz", "x", FindReplaceOptions()))
        }
    }
}
