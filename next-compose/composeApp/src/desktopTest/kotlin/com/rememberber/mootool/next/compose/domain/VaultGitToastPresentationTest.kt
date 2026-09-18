package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultGitToastPresentationTest {
    @Test
    fun shouldToastFailures() {
        assertTrue(VaultGitToastPresentation.shouldToastPanelFailure())
        assertTrue(VaultGitToastPresentation.shouldToastActionFailure())
        assertTrue(VaultGitToastPresentation.shouldToastFlushBlocked())
        assertTrue(VaultGitToastPresentation.shouldToastInvalidRemote())
    }

    @Test
    fun panelFailureMessagePrefersThrowable() {
        assertEquals("disk", VaultGitToastPresentation.panelFailureMessage(IllegalStateException("disk"), "fb"))
        assertEquals("fb", VaultGitToastPresentation.panelFailureMessage(IllegalStateException(), "fb"))
    }

    @Test
    fun actionFailureMessageUsesResultOrFallback() {
        assertEquals("pull failed", VaultGitToastPresentation.actionFailureMessage("pull failed", "fb"))
        assertEquals("fb", VaultGitToastPresentation.actionFailureMessage("", "fb"))
    }
}
