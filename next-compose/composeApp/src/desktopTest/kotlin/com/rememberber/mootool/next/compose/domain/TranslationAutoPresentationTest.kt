package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationAutoPresentationTest {
    @Test
    fun skipAutoWhenDisabledOrRestoredSourceMatches() {
        assertTrue(TranslationAutoPresentation.skipAutoTranslate(null, "hello", autoEnabled = false))
        assertTrue(TranslationAutoPresentation.skipAutoTranslate("hello", "hello", autoEnabled = true))
        assertFalse(TranslationAutoPresentation.skipAutoTranslate("hello", "world", autoEnabled = true))
        assertFalse(TranslationAutoPresentation.skipAutoTranslate(null, "hello", autoEnabled = true))
    }
}
