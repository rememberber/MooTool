package com.rememberber.mootool.next.compose.domain

/** F13 编码解码：转换方向启用守卫与引擎路径（可单测）。 */
object EncodeWiringPresentation {
    fun canConvert(sourceText: String): Boolean = sourceText.isNotBlank()

    /** 对齐 `EncodeScreen` 正向转换钮：源侧无文本时禁用。 */
    fun forwardConvertActionEnabled(sourceText: String): Boolean = canConvert(sourceText)

    /** 对齐 `EncodeScreen` 反向转换钮：结果侧无文本时禁用。 */
    fun reverseConvertActionEnabled(sourceText: String): Boolean = canConvert(sourceText)

    sealed interface ConvertOutcome {
        data class Success(val output: String) : ConvertOutcome
        data class Failure(val error: Throwable) : ConvertOutcome
    }

    fun runConvert(
        tab: EncodeTab,
        forward: Boolean,
        input: String,
        charset: UrlCharset,
        asciiFormat: AsciiFormat,
    ): ConvertOutcome =
        runCatching { EncodeEngine.convert(tab, forward, input, charset, asciiFormat) }.fold(
            onSuccess = { ConvertOutcome.Success(it) },
            onFailure = { ConvertOutcome.Failure(it) },
        )

    fun shouldToastConvertFailure(error: Throwable): Boolean = true
}
