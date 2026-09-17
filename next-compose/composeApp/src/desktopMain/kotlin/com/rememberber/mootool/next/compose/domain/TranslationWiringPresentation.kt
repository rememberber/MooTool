package com.rememberber.mootool.next.compose.domain

/** F20 设置 → `TranslationEngine` 入参（Google/Bing 首选与超时 clamp，可单测）。 */
object TranslationWiringPresentation {
    fun languagePairFromSettings(sourceLangWire: String, targetLangWire: String): Pair<String, String> =
        TranslationEngine.normalizeLanguagePair(sourceLangWire, targetLangWire)

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

    fun runTranslate(input: TranslationInput, proxy: HttpProxyConfig = HttpProxyConfig()): TranslationResult =
        TranslationEngine.translate(input, proxy)

    /** F20「立即翻译」：非空、未超限、无在途请求（对齐 Electron 禁用空源）。 */
    fun canRunTranslate(text: String, translating: Boolean): Boolean =
        !translating &&
            text.isNotBlank() &&
            text.length <= TranslationEngine.MAX_TEXT_UNITS
}
