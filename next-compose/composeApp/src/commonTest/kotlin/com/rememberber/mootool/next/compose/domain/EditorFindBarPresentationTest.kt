package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorFindBarPresentationTest {
    @Test
    fun findActionsRequireNonBlankQuery() {
        assertFalse(EditorFindBarPresentation.findQueryActionEnabled(""))
        assertFalse(EditorFindBarPresentation.findStepActionEnabled("  "))
        assertTrue(EditorFindBarPresentation.findQueryActionEnabled("x"))
        assertTrue(EditorFindBarPresentation.findStepActionEnabled(" x "))
    }
}
