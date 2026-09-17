package com.rememberber.mootool.next.compose.domain

/** F20 在途翻译结果是否写入会话（对齐 Electron 序号/请求 id 丢弃过期响应，非 mock）。 */
object TranslationResponsePresentation {
    fun shouldApplyResult(
        expectedSeq: Int,
        currentSeq: Int,
        activeRequestId: String,
        resultRequestId: String,
    ): Boolean =
        currentSeq == expectedSeq &&
            activeRequestId.isNotEmpty() &&
            activeRequestId == resultRequestId

    /** 自动 debounce 协程启动时的 seq 是否仍为当前（源/语言/provider 变更则放弃）。 */
    fun shouldProceedAutoDebounce(startSeq: Int, currentSeq: Int): Boolean = startSeq == currentSeq

    fun shouldShowError(errorCode: TranslationErrorCode?): Boolean =
        errorCode != null && errorCode != TranslationErrorCode.ABORTED

    /** 译文区底栏：有 provider 或 fallback 标记时才展示（对齐 Electron 结果条）。 */
    fun showResultFooter(providerUsed: String, fallbackUsed: Boolean): Boolean =
        providerUsed.isNotBlank() || fallbackUsed

    fun resultFooterText(providerUsed: String, fallbackUsed: Boolean, fallbackLabel: String): String {
        if (providerUsed.isBlank()) return if (fallbackUsed) fallbackLabel else ""
        val providerLabel = when (providerUsed.lowercase()) {
            "google" -> "Google"
            "bing" -> "Bing"
            else -> providerUsed
        }
        return if (fallbackUsed) "$providerLabel · $fallbackLabel" else providerLabel
    }
}
