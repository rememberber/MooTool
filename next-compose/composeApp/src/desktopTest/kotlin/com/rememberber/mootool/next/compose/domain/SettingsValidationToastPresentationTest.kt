package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class SettingsValidationToastPresentationTest {
    @Test
    fun shouldToastValidationFailure() {
        assertTrue(SettingsValidationToastPresentation.shouldToastValidationFailure())
    }
}
