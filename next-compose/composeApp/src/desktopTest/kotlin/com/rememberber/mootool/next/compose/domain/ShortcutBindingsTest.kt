package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShortcutBindingsTest {
    @Test
    fun sameBindingIsConflictIgnoringCaseAndSpaces() {
        assertTrue(ShortcutBindings.conflict("Meta+K", " meta+k "))
        assertTrue(ShortcutBindings.conflict("Meta+Comma", "Meta+Comma"))
        assertFalse(ShortcutBindings.conflict("Meta+K", "Meta+Comma"))
        assertFalse(ShortcutBindings.conflict("", "Meta+K"))
        assertFalse(ShortcutBindings.conflict("Meta+K", "   "))
    }
}
