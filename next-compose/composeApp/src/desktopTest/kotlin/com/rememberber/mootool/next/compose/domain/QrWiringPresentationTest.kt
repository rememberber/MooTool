package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class QrWiringPresentationTest {
    @Test
    fun fromSettingsClampsSizeAndNormalizesCorrection() {
        val defaults = QrWiringPresentation.fromSettings(qrCodeSize = 20, qrErrorCorrection = "h")
        assertEquals(120, defaults.size)
        assertEquals(QrErrorCorrection.H, defaults.correction)
    }

    @Test
    fun parseSizeFieldNormalizesEditorInput() {
        assertEquals(360, QrWiringPresentation.parseSizeField("360.4", fallback = 240))
        assertEquals(240, QrWiringPresentation.parseSizeField("not-a-number", fallback = 240))
    }

    @Test
    fun generateSizeUsesEngineNormalize() {
        assertEquals(2000, QrWiringPresentation.generateSize(9999))
    }
}
