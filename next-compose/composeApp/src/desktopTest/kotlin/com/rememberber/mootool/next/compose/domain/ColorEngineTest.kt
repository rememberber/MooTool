package com.rememberber.mootool.next.compose.domain

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorEngineTest {
    @Test
    fun parsesShortLongHexAndRgb() {
        assertEquals(RgbColor(0, 255, 136), ColorEngine.parseColor("#0f8"))
        assertEquals(RgbColor(12, 34, 56), ColorEngine.parseColor("12, 34, 56"))
        assertEquals("#0ABBCC", ColorEngine.formatColor(RgbColor(10, 187, 204), ColorFormat.HEX_UPPER))
        assertEquals("#0abbcc", ColorEngine.formatColor(RgbColor(10, 187, 204), ColorFormat.HEX_LOWER))
        assertEquals("10, 187, 204", ColorEngine.formatColor(RgbColor(10, 187, 204), ColorFormat.RGB))
        assertFailsWith<ColorException> { ColorEngine.parseColor("not-a-color") }
        assertFailsWith<ColorException> { ColorEngine.parseColor("12, 340, 1") }
    }

    @Test
    fun performsJavaColorOperations() {
        val a = RgbColor(100, 150, 200)
        val b = RgbColor(50, 200, 100)
        assertEquals(RgbColor(155, 105, 55), ColorEngine.apply(ColorOperation.Invert, a, b))
        assertEquals(RgbColor(19, 117, 78), ColorEngine.apply(ColorOperation.Intersect, a, b))
        assertEquals(RgbColor(150, 255, 255), ColorEngine.apply(ColorOperation.Add, a, b))
        assertEquals(RgbColor(50, 50, 100), ColorEngine.apply(ColorOperation.Difference, a, b))
        assertEquals(RgbColor(75, 175, 150), ColorEngine.apply(ColorOperation.Average, a, b))
        assertEquals("#000000", ColorEngine.bestTextColor(ColorEngine.parseColor("#ffffff")))
        assertEquals("#FFFFFF", ColorEngine.bestTextColor(ColorEngine.parseColor("#111111")))
    }

    @Test
    fun keepsJavaThemeOrderAndHashes() {
        assertEquals(
            listOf(ColorThemeId.Default, ColorThemeId.Theme1, ColorThemeId.Theme2, ColorThemeId.Theme3, ColorThemeId.Theme4, ColorThemeId.Theme5, ColorThemeId.China),
            ColorEngine.THEMES.map { it.id }
        )
        assertTrue(ColorEngine.THEMES.all { it.main.size == 10 && it.shades.size == 10 && it.shades.all { column -> column.size == 5 } })
        assertEquals("72e2218c6dcffce0def99f1317075f2041127035730a0379875f5e875e893575", sha256(ColorEngine.canonicalThemesJson()))
        assertEquals("1994345359abd00b901c22614eeef6b400775120cebc04083e05bab9875b535c", sha256(ColorEngine.canonicalStandardJson()))
    }

    @Test
    fun samplesScreenPixelsAndClamps() {
        val pixels = byteArrayOf(
            10, 20, 30, -1,
            222.toByte(), 143.toByte(), 125, -1
        )
        assertEquals(RgbColor(222, 143, 125), ColorEngine.sampleRgba(pixels, 2, 1, 1.0, 0.0))
        val single = byteArrayOf(1, 2, 3, -1)
        assertEquals(RgbColor(1, 2, 3), ColorEngine.sampleRgba(single, 1, 1, 99.0, -4.0))
        assertEquals("#010203", ColorEngine.formatColor(ColorEngine.sampleRgba(single, 1, 1, 99.0, -4.0)!!, ColorFormat.HEX_UPPER))
        assertEquals("#0010FF", ColorEngine.rgbToHex(-10, 16, 300))
        assertNull(ColorEngine.sampleRgba(byteArrayOf(1, 2, 3), 1, 1, 0.0, 0.0))
        assertEquals("#AABBCC", ColorEngine.extractHex("was #aabbcc now #DE8F7D extra"))
        assertEquals("#DE8F7D", ColorEngine.extractHex("result #de8f7d"))
        assertEquals(ColorThemeId.China, ColorEngine.themeId("china"))
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}
