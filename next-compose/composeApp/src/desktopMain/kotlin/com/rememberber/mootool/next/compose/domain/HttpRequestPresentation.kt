package com.rememberber.mootool.next.compose.domain

/** F09 发送/在途响应写入守卫（对齐 Electron requestId 丢弃过期响应）。 */
object HttpRequestPresentation {
    fun shouldApplyResponse(activeRequestId: String, resultRequestId: String): Boolean =
        activeRequestId.isNotEmpty() && activeRequestId == resultRequestId

    fun previousResponseBeforeSend(
        current: HttpResponseResult?,
        previous: HttpResponseResult?,
    ): HttpResponseResult? = HttpResponsePresentation.usableResponse(current, previous)

    fun canSend(urlTrimmed: String, sending: Boolean): Boolean = urlTrimmed.isNotBlank() && !sending

    fun runSend(
        draft: HttpRequestDraft,
        requestId: String,
        timeoutMs: Int,
        proxy: HttpProxyConfig = HttpProxyConfig(),
    ): HttpResponseResult = HttpEngine.send(draft, requestId, timeoutMs, proxy)
}
