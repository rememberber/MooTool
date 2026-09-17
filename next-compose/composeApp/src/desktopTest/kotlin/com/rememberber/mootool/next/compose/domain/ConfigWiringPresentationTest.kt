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

    @Test
    fun runToYamlRoundTrip() {
        val outcome = ConfigWiringPresentation.runToYaml("a=b")
        assertTrue(outcome is ConfigWiringPresentation.ConvertOutcome.Success)
        val yaml = (outcome as ConfigWiringPresentation.ConvertOutcome.Success).output
        assertTrue(yaml.contains("a:"))
        val back = ConfigWiringPresentation.runToProperties(yaml)
        assertTrue(back is ConfigWiringPresentation.ConvertOutcome.Success)
        assertTrue((back as ConfigWiringPresentation.ConvertOutcome.Success).output.contains("a=b"))
    }

    @Test
    fun runValidateAndFormat() {
        assertTrue(ConfigWiringPresentation.runValidate("a: 1").valid)
        assertFalse(ConfigWiringPresentation.runValidate("a: [").valid)
        val formatted = ConfigWiringPresentation.runFormat("a: 1\nb: 2")
        assertTrue(formatted is ConfigWiringPresentation.ConvertOutcome.Success)
    }
}
