package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ReformatWiringPresentationTest {
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
