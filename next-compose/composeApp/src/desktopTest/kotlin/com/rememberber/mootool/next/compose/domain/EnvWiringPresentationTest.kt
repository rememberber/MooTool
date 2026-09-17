package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvWiringPresentationTest {
    @Test
    fun refreshBlockedWhileLoadingOrSaving() {
        assertFalse(EnvWiringPresentation.refreshEnabled(loading = true, saving = false))
        assertFalse(EnvWiringPresentation.refreshEnabled(loading = false, saving = true))
        assertTrue(EnvWiringPresentation.refreshEnabled(loading = false, saving = false))
    }

    @Test
    fun exportRequiresSnapshot() {
        assertFalse(EnvWiringPresentation.exportEnabled(hasSnapshot = false))
        assertTrue(EnvWiringPresentation.exportEnabled(hasSnapshot = true))
    }

    @Test
    fun saveEditorRequiresNonBlankKey() {
        assertFalse(EnvWiringPresentation.saveEditorEnabled(trimmedKey = "", saving = false))
        assertFalse(EnvWiringPresentation.saveEditorEnabled(trimmedKey = "A", saving = true))
        assertTrue(EnvWiringPresentation.saveEditorEnabled(trimmedKey = "A", saving = false))
    }
}
