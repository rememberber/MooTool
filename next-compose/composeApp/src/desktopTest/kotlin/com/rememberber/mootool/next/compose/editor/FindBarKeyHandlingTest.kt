package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertEquals

class FindBarKeyHandlingTest {
    @Test
    fun row_keys_map_to_navigation_and_close() {
        assertEquals(FindBarKeyAction.Previous, previewFindBarRowKey(Key.DirectionUp, KeyEventType.KeyDown, false))
        assertEquals(FindBarKeyAction.Next, previewFindBarRowKey(Key.DirectionDown, KeyEventType.KeyDown, false))
        assertEquals(FindBarKeyAction.Close, previewFindBarRowKey(Key.Escape, KeyEventType.KeyDown, false))
        assertEquals(FindBarKeyAction.None, previewFindBarRowKey(Key.Escape, KeyEventType.KeyUp, false))
        assertEquals(FindBarKeyAction.None, previewFindBarRowKey(Key.DirectionUp, KeyEventType.KeyDown, true))
    }

    @Test
    fun find_query_enter_triggers_find_next() {
        assertEquals(FindBarKeyAction.FindNext, previewFindQueryEnter(Key.Enter, KeyEventType.KeyDown, false))
        assertEquals(FindBarKeyAction.FindNext, previewFindQueryEnter(Key.NumPadEnter, KeyEventType.KeyDown, false))
        assertEquals(FindBarKeyAction.None, previewFindQueryEnter(Key.Enter, KeyEventType.KeyDown, true))
    }
}
