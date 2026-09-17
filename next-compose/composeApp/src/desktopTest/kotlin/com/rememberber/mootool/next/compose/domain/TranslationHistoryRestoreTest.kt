package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.sessions.TranslationSession
import com.rememberber.mootool.next.compose.storage.TranslationHistoryItem
import kotlin.test.Test
import kotlin.test.assertEquals
class TranslationHistoryRestoreTest {
    @Test
    fun applyRestoresTextAndSkipsAutoTranslateViaRestoredSource() {
        val session = TranslationSession()
        session.source = "old"
        session.sequence = 2
        val item = TranslationHistoryItem(
            id = "1",
            sourceText = "hello",
            targetText = "你好",
            sourceLang = "en",
            targetLang = "zh-CN",
            translatorType = "bing",
        )
        TranslationHistoryRestore.applyToSession(session, item)
        assertEquals(3, session.sequence)
        assertEquals("hello", session.source)
        assertEquals("你好", session.target)
        assertEquals("hello", session.restoredSource)
        assertEquals("bing", session.providerUsed)
    }

    @Test
    fun patchSettingsNormalizesLanguagesAndProvider() {
        val defaults = AppSettings()
        val item = TranslationHistoryItem(
            id = "1",
            sourceText = "a",
            targetText = "b",
            sourceLang = "English",
            targetLang = "en",
            translatorType = "GOOGLE",
        )
        val patched = TranslationHistoryRestore.patchSettingsProvider(
            TranslationHistoryRestore.patchSettingsLanguages(defaults, item),
            item,
        )
        assertEquals("auto", patched.tools.translationSourceLang)
        assertEquals("en", patched.tools.translationTargetLang)
        assertEquals("google", patched.tools.translationProvider)
    }

    @Test
    fun patchSettingsKeepsProviderWhenHistoryTypeUnknown() {
        val settings = AppSettings(tools = AppSettings().tools.copy(translationProvider = "bing"))
        val item = TranslationHistoryItem(
            id = "1",
            sourceText = "a",
            targetText = "b",
            sourceLang = "en",
            targetLang = "zh-CN",
            translatorType = "legacy",
        )
        assertEquals("bing", TranslationHistoryRestore.patchSettingsProvider(settings, item).tools.translationProvider)
    }
}
