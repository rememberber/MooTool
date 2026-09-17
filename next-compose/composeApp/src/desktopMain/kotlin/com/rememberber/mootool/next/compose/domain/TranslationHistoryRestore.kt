package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.sessions.TranslationSession
import com.rememberber.mootool.next.compose.storage.TranslationHistoryItem

/** F20 翻译历史/单词本回填：恢复文本与语言，抑制自动 debounce 重发（对齐 Electron `restore`）。 */
object TranslationHistoryRestore {
    fun applyToSession(session: TranslationSession, item: TranslationHistoryItem) {
        session.sequence += 1
        session.restoredSource = item.sourceText
        session.source = item.sourceText
        session.target = item.targetText
        session.providerUsed = item.translatorType
        session.fallbackUsed = false
        session.error = ""
        session.notice = ""
    }

    fun patchSettingsLanguages(settings: AppSettings, item: TranslationHistoryItem): AppSettings {
        val languages = TranslationEngine.normalizeLanguagePair(item.sourceLang, item.targetLang)
        return settings.copy(
            tools = settings.tools.copy(
                translationSourceLang = languages.first,
                translationTargetLang = languages.second,
            ),
        )
    }

    fun patchSettingsProvider(settings: AppSettings, item: TranslationHistoryItem): AppSettings {
        val provider = when (item.translatorType.trim().lowercase()) {
            "bing" -> "bing"
            "google" -> "google"
            else -> settings.tools.translationProvider
        }
        return settings.copy(tools = settings.tools.copy(translationProvider = provider))
    }
}
