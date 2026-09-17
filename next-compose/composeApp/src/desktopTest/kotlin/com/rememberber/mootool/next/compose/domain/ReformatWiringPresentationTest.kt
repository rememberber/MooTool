package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReformatWiringPresentationTest {
    @Test
    fun canRunFormatRequiresInputAndNotBusy() {
        assertFalse(ReformatWiringPresentation.canRunFormat(busy = true, inputNotBlank = true))
        assertFalse(ReformatWiringPresentation.canRunFormat(busy = false, inputNotBlank = false))
        assertTrue(ReformatWiringPresentation.canRunFormat(busy = false, inputNotBlank = true))
    }

    @Test
    fun defaultSaveFileNameMatchesSaveResult() {
        assertEquals(
            "App.java",
            ReformatWiringPresentation.defaultSaveFileName("App.java", ReformatType.Java),
        )
        assertEquals(
            "formatted.conf",
            ReformatWiringPresentation.defaultSaveFileName("", ReformatType.Nginx),
        )
    }
}
