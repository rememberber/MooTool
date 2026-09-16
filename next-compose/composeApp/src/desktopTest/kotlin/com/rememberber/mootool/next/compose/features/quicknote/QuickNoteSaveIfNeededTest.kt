package com.rememberber.mootool.next.compose.features.quicknote

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteSaveIfNeededTest {
    @Test
    fun cleanDocumentSkipsSaveEvenIfSaveWouldFail() {
        var called = false
        assertTrue(quickNoteSaveIfNeeded(dirty = false) {
            called = true
            false
        })
        assertFalse(called)
    }

    @Test
    fun dirtyDocumentRunsSave() {
        var called = false
        assertTrue(quickNoteSaveIfNeeded(dirty = true) {
            called = true
            true
        })
        assertTrue(called)
    }
}
