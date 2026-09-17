package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun toolbarGuardsMatchScreenSemantics() {
        assertFalse(QrWiringPresentation.canGenerate(busy = true, content = "x"))
        assertTrue(QrWiringPresentation.canGenerate(busy = false, content = "x"))
        assertFalse(QrWiringPresentation.hasPngOutput(null))
        assertTrue(QrWiringPresentation.hasPngOutput(byteArrayOf(1)))
        assertFalse(QrWiringPresentation.canRecognize(null, busy = false))
        assertTrue(QrWiringPresentation.canRecognize(byteArrayOf(1), busy = false))
        assertTrue(QrWiringPresentation.canCopyRecognition("ok"))
    }
}
