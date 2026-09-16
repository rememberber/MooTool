package com.rememberber.mootool.next.compose.features.host

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HostFindNavigationTest {
    @Test
    fun jump_selects_next_match_from_caret() {
        val field = TextFieldValue("aa bb aa", TextRange(3))
        val jumped = HostFindNavigation.jump("aa bb aa", "aa", FindReplaceOptions(), field, forward = true)
        assertNotNull(jumped)
        assertEquals(TextRange(6, 8), jumped.selection)
    }

    @Test
    fun replaceCurrent_updates_selection_to_replacement() {
        val field = TextFieldValue("hello", TextRange(0))
        val updated = HostFindNavigation.afterReplace("heXXo", field, com.rememberber.mootool.next.compose.domain.FindMatch(2, 4))
        assertEquals("heXXo", updated.text)
        assertEquals(TextRange(2, 4), updated.selection)
    }

    @Test
    fun afterReplaceAndSelectNext_advances_to_following_match() {
        val field = TextFieldValue("aa aa", TextRange(0, 2))
        val replaced = com.rememberber.mootool.next.compose.domain.FindMatch(0, 2)
        val updated = HostFindNavigation.afterReplaceAndSelectNext(
            "XX aa",
            "aa",
            FindReplaceOptions(),
            field,
            replaced
        )
        assertEquals("XX aa", updated.text)
        assertEquals(TextRange(3, 5), updated.selection)
    }
}
