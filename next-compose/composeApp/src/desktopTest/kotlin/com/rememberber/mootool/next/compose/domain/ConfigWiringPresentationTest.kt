package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigWiringPresentationTest {
    @Test
    fun convertDirectionsRequireSource() {
        assertFalse(ConfigWiringPresentation.canToYaml(""))
        assertTrue(ConfigWiringPresentation.canToYaml("a=b"))
        assertFalse(ConfigWiringPresentation.canToProperties(""))
        assertTrue(ConfigWiringPresentation.canToProperties("a: b"))
    }
}
