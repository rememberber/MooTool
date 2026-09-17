package com.rememberber.mootool.next.compose.domain

import java.io.File
import java.nio.charset.StandardCharsets

/** F09 响应区「上次响应」显隐与可见 payload（对齐 Electron 发送中/失败保留上一响应）。 */
object HttpResponsePresentation {
    fun isTransportFailure(result: HttpResponseResult?): Boolean {
        val code = result?.errorCode ?: return false
        return code == HttpErrorCode.ABORTED ||
            code == HttpErrorCode.TIMEOUT ||
            code == HttpErrorCode.NETWORK ||
            code == HttpErrorCode.INVALID_REQUEST
    }

    fun usableResponse(current: HttpResponseResult?, previous: HttpResponseResult?): HttpResponseResult? =
        current?.takeUnless { isTransportFailure(it) } ?: previous

    fun showPreviousLabel(
        sending: Boolean,
        current: HttpResponseResult?,
        previous: HttpResponseResult?,
    ): Boolean = previous != null && (sending || isTransportFailure(current))

    fun visibleResponse(
        sending: Boolean,
        current: HttpResponseResult?,
        previous: HttpResponseResult?,
    ): HttpResponseResult? = if (showPreviousLabel(sending, current, previous)) previous else current

    /** 响应元信息行是否使用成功色（对齐 Electron `.http-status--ok`）。 */
    fun statusMetaSuccess(visible: HttpResponseResult?): Boolean =
        visible != null && visible.ok && visible.status in 200..399

    sealed interface WriteExportOutcome {
        data object Success : WriteExportOutcome
        data class Failure(val error: Throwable) : WriteExportOutcome
    }

    fun runWriteResponseText(file: File, text: String): WriteExportOutcome =
        runCatching { file.writeText(text, StandardCharsets.UTF_8) }.fold(
            onSuccess = { WriteExportOutcome.Success },
            onFailure = { WriteExportOutcome.Failure(it) },
        )

    fun runWriteResponseBytes(file: File, bytes: ByteArray): WriteExportOutcome =
        runCatching { file.writeBytes(bytes) }.fold(
            onSuccess = { WriteExportOutcome.Success },
            onFailure = { WriteExportOutcome.Failure(it) },
        )
}
