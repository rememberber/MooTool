package com.rememberber.mootool.next.compose.domain

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QrEngineTest {
    @Test
    fun roundTripsContentAtEveryCorrectionLevel() {
        val content = "https://github.com/rememberber/MooTool"
        for (level in QrErrorCorrection.entries) {
            val png = QrEngine.generatePng(content, 240, level)
            assertTrue(png.size > 32)
            assertEquals(content, QrEngine.decodePng(png))
        }
    }

    @Test
    fun encodesChineseAndRoundTripsThroughLogo() {
        val content = "https://mootool.app/搜索"
        val plain = QrEngine.generatePng(content, 320, QrErrorCorrection.H)
        assertEquals(content, QrEngine.decodePng(plain))
        val logo = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
        val graphics = logo.createGraphics()
        graphics.color = Color.RED
        graphics.fillRect(0, 0, 16, 16)
        graphics.dispose()
        val withLogo = QrEngine.generatePng(content, 360, QrErrorCorrection.H, logo)
        assertEquals(content, QrEngine.decodePng(withLogo))
    }

    @Test
    fun normalizesSizeAndRejectsEmptyOrBrokenImages() {
        assertEquals(120, QrEngine.normalizeSize(20))
        assertEquals(360, QrEngine.normalizeSize(360))
        assertEquals(2000, QrEngine.normalizeSize(9999))
        val empty = assertFailsWith<QrException> { QrEngine.generatePng("  ", 300, QrErrorCorrection.M) }
        assertEquals("empty", empty.code)
        val broken = assertFailsWith<QrException> { QrEngine.decodePng(byteArrayOf(1, 2, 3, 4)) }
        assertEquals("invalid-image", broken.code)
        val blank = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
        val graphics = blank.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, 64, 64)
        graphics.dispose()
        val missing = assertFailsWith<QrException> { QrEngine.decode(blank) }
        assertEquals("not-found", missing.code)
    }
}
