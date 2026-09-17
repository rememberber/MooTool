package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TranslationWiringPresentationTest {
    @Test
    fun wiresGoogleProviderAndClampsTimeout() {
        val input = TranslationWiringPresentation.buildInput(
            requestId = "r1",
            text = "hello",
            sourceLangWire = "English",
            targetLangWire = "zh-CN",
            providerWire = "google",
            timeoutWire = 999_999,
        )
        assertEquals("en", input.sourceLang)
        assertEquals("zh-CN", input.targetLang)
        assertEquals(TranslationProvider.Google, input.preferredProvider)
        assertEquals(120_000, input.timeoutMs)
    }

    @Test
    fun wiresBingProviderAndNormalizesSameLanguagePair() {
        val input = TranslationWiringPresentation.buildInput(
            requestId = "r2",
            text = "hi",
            sourceLangWire = "en",
            targetLangWire = "en",
            providerWire = "BING",
            timeoutWire = 500,
        )
        assertEquals("auto", input.sourceLang)
        assertEquals("en", input.targetLang)
        assertEquals(TranslationProvider.Bing, input.preferredProvider)
        assertEquals(1_000, input.timeoutMs)
    }
}
