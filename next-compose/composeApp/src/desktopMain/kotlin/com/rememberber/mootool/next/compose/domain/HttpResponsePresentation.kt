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

    private fun responseToolbarIoEnabled(visible: HttpResponseResult?, sending: Boolean): Boolean =
        visible != null && !sending

    /** 对齐 `HttpScreen` 响应复制钮：无可见响应或发送中禁用。 */
    fun copyResponseActionEnabled(visible: HttpResponseResult?, sending: Boolean): Boolean =
        responseToolbarIoEnabled(visible, sending)

    /** 对齐 `HttpScreen` 响应另存/二进制另存钮与溢出菜单项。 */
    fun saveResponseActionEnabled(visible: HttpResponseResult?, sending: Boolean): Boolean =
        responseToolbarIoEnabled(visible, sending)

    /** 对齐 `HttpScreen` 响应「查找」钮：无可见响应或发送中禁用；查找条已打开时仍须可点关闭。 */
    fun responseFindOpenActionEnabled(
        findOpen: Boolean,
        visible: HttpResponseResult?,
        sending: Boolean,
    ): Boolean = findOpen || responseToolbarIoEnabled(visible, sending)

    /** 对齐 HTTP 响应查找条「查找」：同 [EditorFindBarPresentation.findQueryActionEnabled]。 */
    fun findQueryActionEnabled(query: String): Boolean =
        EditorFindBarPresentation.findQueryActionEnabled(query)

    /** 对齐 HTTP 响应查找条「上一处/下一处」：同 [EditorFindBarPresentation.findStepActionEnabled]。 */
    fun findStepActionEnabled(query: String): Boolean =
        EditorFindBarPresentation.findStepActionEnabled(query)

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

    fun shouldToastWriteFailure(error: Throwable): Boolean = true
}
