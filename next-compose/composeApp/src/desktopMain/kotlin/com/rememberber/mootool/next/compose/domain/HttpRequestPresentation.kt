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

    /** 对齐 `HttpScreen` 发送钮：同 [canSend]。 */
    fun sendActionEnabled(urlTrimmed: String, sending: Boolean): Boolean = canSend(urlTrimmed, sending)

    /** 对齐 `HttpScreen` 发送中「停止」：须在途且已有 requestId（与 [cancelInFlightSend] 守卫一致）。 */
    fun stopSendActionEnabled(sending: Boolean, requestId: String): Boolean =
        sending && requestId.isNotBlank()

    fun runSend(
        draft: HttpRequestDraft,
        requestId: String,
        timeoutMs: Int,
        proxy: HttpProxyConfig = HttpProxyConfig(),
    ): HttpResponseResult = HttpEngine.send(draft, requestId, timeoutMs, proxy)

    fun shouldToastResponseError(errorCode: HttpErrorCode?): Boolean = errorCode != null

    fun shouldToastClientValidation(): Boolean = true

    fun shouldToastCopyFailure(): Boolean = true
}
