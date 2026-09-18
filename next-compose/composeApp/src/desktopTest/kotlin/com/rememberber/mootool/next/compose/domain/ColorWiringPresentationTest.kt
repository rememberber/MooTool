package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorWiringPresentationTest {
    @Test
    fun screenPickBlockedWhilePicking() {
        assertFalse(ColorWiringPresentation.canScreenPick(picking = true))
        assertTrue(ColorWiringPresentation.canScreenPick(picking = false))
    }

    @Test
    fun applyRequiresNonBlankCode() {
        assertFalse(ColorWiringPresentation.canApplyCode("  "))
        assertTrue(ColorWiringPresentation.canApplyCode("#fff"))
    }

    @Test
    fun shouldToastOperationFailure() {
        assertTrue(ColorWiringPresentation.shouldToastOperationFailure(IllegalStateException()))
    }

    @Test
    fun shouldToastErrorMessage() {
        assertTrue(ColorWiringPresentation.shouldToastErrorMessage())
    }
}
