package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationWiringPresentationTest {
    @Test
    fun languagePairFromSettingsNormalizesSameLang() {
        val (source, target) = TranslationWiringPresentation.languagePairFromSettings("en", "en")
        assertEquals("auto", source)
        assertEquals("en", target)
    }

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

    @Test
    fun canRunTranslateRequiresNonEmptyIdleWithinLimit() {
        assertFalse(TranslationWiringPresentation.canRunTranslate("", translating = false))
        assertFalse(TranslationWiringPresentation.canRunTranslate("hi", translating = true))
        assertTrue(TranslationWiringPresentation.canRunTranslate("hello", translating = false))
        assertFalse(
            TranslationWiringPresentation.canRunTranslate(
                "x".repeat(TranslationEngine.MAX_TEXT_UNITS + 1),
                translating = false,
            ),
        )
        assertTrue(TranslationWiringPresentation.translateActionEnabled("hello", translating = false))
    }

    @Test
    fun toolbarWordBookAndHistoryActionEnabled() {
        assertFalse(TranslationWiringPresentation.copyResultActionEnabled(""))
        assertTrue(TranslationWiringPresentation.copyResultActionEnabled("译文"))
        assertFalse(TranslationWiringPresentation.saveWordFromSourceActionEnabled("  "))
        assertTrue(TranslationWiringPresentation.saveWordFromSourceActionEnabled("word"))
        assertFalse(TranslationWiringPresentation.deleteWordActionEnabled(""))
        assertTrue(TranslationWiringPresentation.deleteWordConfirmActionEnabled("id-1"))
        assertFalse(TranslationWiringPresentation.applyWordActionEnabled(false))
        assertTrue(TranslationWiringPresentation.retranslateWordActionEnabled(true, "src"))
        assertFalse(TranslationWiringPresentation.retranslateWordActionEnabled(true, ""))
        assertFalse(TranslationWiringPresentation.saveWordEntryActionEnabled(""))
        assertFalse(TranslationWiringPresentation.clearHistoryActionEnabled(0))
        assertTrue(TranslationWiringPresentation.clearHistoryActionEnabled(1))
        assertFalse(TranslationWiringPresentation.clearHistoryConfirmActionEnabled(0))
        assertTrue(TranslationWiringPresentation.clearHistoryConfirmActionEnabled(2))
    }

    @Test
    fun runTranslateRejectsOverlongTextWithoutNetwork() {
        val input = TranslationWiringPresentation.buildInput(
            requestId = "long",
            text = "x".repeat(TranslationEngine.MAX_TEXT_UNITS + 1),
            sourceLangWire = "en",
            targetLangWire = "zh-CN",
            providerWire = "google",
            timeoutWire = 15_000,
        )
        val result = TranslationWiringPresentation.runTranslate(input)
        assertEquals(false, result.ok)
        assertEquals(TranslationErrorCode.INVALID_REQUEST, result.errorCode)
    }

    @Test
    fun runSaveWordWrapsSuccessAndFailure() {
        val ok = TranslationWiringPresentation.runSaveWord { "saved" }
        assertTrue(ok is TranslationWiringPresentation.SaveWordOutcome.Success)
        assertEquals("saved", (ok as TranslationWiringPresentation.SaveWordOutcome.Success).value)
        val fail = TranslationWiringPresentation.runSaveWord<String> { throw IllegalStateException("db") }
        assertTrue(fail is TranslationWiringPresentation.SaveWordOutcome.Failure)
    }

    @Test
    fun failureMessagePrefersThrowableMessage() {
        val t: (String) -> String = { key -> "fallback:$key" }
        assertEquals("db", TranslationWiringPresentation.failureMessage("translation.error.generic", IllegalStateException("db"), t))
        assertEquals("fallback:translation.error.generic", TranslationWiringPresentation.failureMessage("translation.error.generic", IllegalStateException(), t))
    }

    @Test
    fun shouldToastFailures() {
        assertFalse(TranslationWiringPresentation.shouldToastTranslateFailure(TranslationErrorCode.ABORTED))
        assertTrue(TranslationWiringPresentation.shouldToastTranslateFailure(TranslationErrorCode.NETWORK))
        assertTrue(TranslationWiringPresentation.shouldToastSaveFailure(IllegalStateException()))
    }
}
