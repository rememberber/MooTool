package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageWiringPresentationTest {
    @Test
    fun processRequiresSelectionAndIdle() {
        assertFalse(ImageWiringPresentation.canProcessSelection(selectedCount = 0, busy = false))
        assertFalse(ImageWiringPresentation.canProcessSelection(selectedCount = 2, busy = true))
        assertTrue(ImageWiringPresentation.canProcessSelection(selectedCount = 1, busy = false))
    }
}
