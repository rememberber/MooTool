package com.rememberber.mootool.next.compose.domain

/** F20 自动翻译 debounce 与历史/单词本恢复抑制（对齐 Electron `restore` 不重发）。 */
object TranslationAutoPresentation {
    const val AUTO_DEBOUNCE_MS = 500L

    fun skipAutoTranslate(restoredSource: String?, source: String, autoEnabled: Boolean): Boolean {
        if (!autoEnabled) return true
        if (restoredSource != null && restoredSource == source) return true
        return false
    }
}
