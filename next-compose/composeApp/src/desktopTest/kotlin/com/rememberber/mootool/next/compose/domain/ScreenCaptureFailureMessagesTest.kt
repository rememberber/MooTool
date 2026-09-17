package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenCaptureFailureMessagesTest {
    private val t: (String) -> String = { key -> key }

    @Test
    fun trayCaptureUsesPermissionCopyWhenSettingsOpened() {
        val message = ScreenCaptureFailureMessages.trayCaptureMessage(
            t,
            ColorException("permission", "blank", openedSettings = true),
        )
        assertTrue(message.contains("color.error.permission"))
        assertTrue(message.contains("color.error.permissionSettings"))
    }

    @Test
    fun trayCaptureFallsBackToCaptureKeyForUnknownErrors() {
        assertEquals("image.error.capture", ScreenCaptureFailureMessages.trayCaptureMessage(t, RuntimeException("")))
        assertEquals("network down", ScreenCaptureFailureMessages.trayCaptureMessage(t, RuntimeException("network down")))
    }

    @Test
    fun imageOperationMapsPermissionAndImageCodes() {
        assertEquals("image.error.missing", ScreenCaptureFailureMessages.imageOperationMessage(t, ImageException("missing", "")))
        assertEquals(
            "color.error.permission",
            ScreenCaptureFailureMessages.imageOperationMessage(t, ColorException("permission", "denied")),
        )
        assertEquals(
            "color.error.permission\ncolor.error.permissionSettings",
            ScreenCaptureFailureMessages.imageOperationMessage(
                t,
                ColorException("permission", "denied", openedSettings = true),
            ),
        )
    }

    @Test
    fun colorPickerMapsInvalidAndPermission() {
        assertEquals("color.error.invalid", ScreenCaptureFailureMessages.colorPickerMessage(t, ColorException("invalid-hex", "")))
        assertEquals("color.error.permission", ScreenCaptureFailureMessages.colorPickerMessage(t, ColorException("picker", "none")))
    }
}
