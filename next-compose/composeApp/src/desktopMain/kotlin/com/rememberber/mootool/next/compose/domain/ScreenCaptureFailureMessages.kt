package com.rememberber.mootool.next.compose.domain

/**
 * User-visible errors for screen capture used by the tray, color board, and image tools.
 * Tray pick-color / region-screenshot and in-tool overlays share the permission retry path in [ScreenColorSampler].
 */
object ScreenCaptureFailureMessages {
    fun colorPickerMessage(t: (String) -> String, error: Throwable): String {
        val code = (error as? ColorException)?.code
        return when (code) {
            "invalid-hex", "invalid-rgb" -> t("color.error.invalid")
            "permission", "picker" -> ScreenCaptureAccess.userMessage(t, error)
            else -> error.message?.takeIf { it.isNotBlank() } ?: t("color.error.generic")
        }
    }

    fun imageOperationMessage(t: (String) -> String, error: Throwable): String {
        val image = error as? ImageException
        return when (image?.code) {
            "invalid-base64" -> t("image.error.base64")
            "invalid-image", "unsupported" -> t("image.error.image")
            "watermark-text" -> t("image.error.watermark")
            "too-large" -> t("image.error.tooLarge")
            "too-many" -> t("image.error.tooMany")
            "cancelled" -> t("image.cancelled")
            "missing" -> t("image.error.missing")
            "exists" -> t("image.error.exists")
            else -> {
                val color = error as? ColorException
                if (color?.code == "permission" || color?.code == "picker") {
                    ScreenCaptureAccess.userMessage(t, error)
                } else {
                    error.message?.takeIf { it.isNotBlank() } ?: t("image.error.generic")
                }
            }
        }
    }

    /** Tray color pick and region screenshot failures (no in-tool ImageException codes). */
    fun trayCaptureMessage(t: (String) -> String, error: Throwable): String =
        when (error) {
            is ColorException -> ScreenCaptureAccess.userMessage(t, error)
            is ImageException -> imageOperationMessage(t, error)
            else -> error.message?.takeIf { it.isNotBlank() } ?: t("image.error.capture")
        }
}
