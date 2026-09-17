package com.rememberber.mootool.next.compose.domain

/** F20 设置 → `TranslationEngine` 入参（Google/Bing 首选与超时 clamp，可单测）。 */
object TranslationWiringPresentation {
    fun buildInput(
        requestId: String,
        text: String,
        sourceLangWire: String,
        targetLangWire: String,
        providerWire: String,
        timeoutWire: Int,
    ): TranslationInput {
        val languages = TranslationEngine.normalizeLanguagePair(sourceLangWire, targetLangWire)
        return TranslationInput(
            requestId = requestId,
            text = text,
            sourceLang = languages.first,
            targetLang = languages.second,
            preferredProvider = TranslationEngine.parseProvider(providerWire),
            timeoutMs = TranslationEngine.clampTimeout(timeoutWire),
        )
    }
}
