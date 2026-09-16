package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FindReplaceShortcutPolicyTest {
    @Test
    fun opensFind_for_meta_f_or_r_without_modifiers() {
        assertTrue(FindReplaceShortcutPolicy.opensFindReplace(Key.F, meta = true, shift = false, alt = false))
        assertTrue(FindReplaceShortcutPolicy.opensFindReplace(Key.R, meta = true, shift = false, alt = false))
    }

    @Test
    fun does_not_open_find_for_shift_format_or_without_meta() {
        assertFalse(FindReplaceShortcutPolicy.opensFindReplace(Key.F, meta = true, shift = true, alt = false))
        assertFalse(FindReplaceShortcutPolicy.opensFindReplace(Key.R, meta = true, shift = true, alt = false))
        assertFalse(FindReplaceShortcutPolicy.opensFindReplace(Key.F, meta = false, shift = false, alt = false))
    }
}
