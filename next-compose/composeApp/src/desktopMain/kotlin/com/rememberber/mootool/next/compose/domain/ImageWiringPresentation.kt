package com.rememberber.mootool.next.compose.domain

import kotlin.coroutines.cancellation.CancellationException

/** F23 图片：busy/选中态工具栏守卫（SVG 数值 clamp 见 [ImageSvgWiringPresentation]）。 */
object ImageWiringPresentation {
    fun canImport(busy: Boolean): Boolean = !busy

    fun importActionEnabled(busy: Boolean): Boolean = canImport(busy)

    fun canProcessSelection(selectedCount: Int, busy: Boolean): Boolean = selectedCount > 0 && !busy

    fun processSelectionActionEnabled(selectedCount: Int, busy: Boolean): Boolean =
        canProcessSelection(selectedCount, busy)

    fun canActOnCurrent(hasCurrent: Boolean, busy: Boolean = false): Boolean = hasCurrent && !busy

    fun actOnCurrentActionEnabled(hasCurrent: Boolean, busy: Boolean): Boolean =
        canActOnCurrent(hasCurrent, busy)

    fun canCopyCurrent(hasCurrent: Boolean): Boolean = hasCurrent

    fun copyCurrentActionEnabled(hasCurrent: Boolean): Boolean = canCopyCurrent(hasCurrent)

    fun canExportBase64(text: String): Boolean = text.isNotBlank()

    fun exportBase64ActionEnabled(text: String): Boolean = canExportBase64(text)

    fun canStartWatermark(text: String): Boolean = text.isNotBlank()

    fun startWatermarkActionEnabled(text: String): Boolean = canStartWatermark(text)

    fun renamePromptActionEnabled(name: String): Boolean = name.isNotBlank()

    sealed interface DecodeOutcome {
        data class Success(val image: java.awt.image.BufferedImage) : DecodeOutcome
        data class Failure(val error: Throwable) : DecodeOutcome
    }

    fun runDecodeDataUrl(text: String): DecodeOutcome =
        runCatching { ImageEngine.decodeDataUrl(text) }.fold(
            onSuccess = { DecodeOutcome.Success(it) },
            onFailure = { DecodeOutcome.Failure(it) },
        )

    sealed interface ExportAssetsOutcome {
        data object Success : ExportAssetsOutcome
        data class Failure(val error: Throwable) : ExportAssetsOutcome
    }

    fun runExportAssets(block: () -> Unit): ExportAssetsOutcome =
        runCatching(block).fold(
            onSuccess = { ExportAssetsOutcome.Success },
            onFailure = { ExportAssetsOutcome.Failure(it) },
        )

    fun shouldToastProcessFailure(error: Throwable): Boolean {
        if (error is CancellationException) return false
        val image = error as? ImageException
        if (image?.code == "cancelled") return false
        return true
    }
}
