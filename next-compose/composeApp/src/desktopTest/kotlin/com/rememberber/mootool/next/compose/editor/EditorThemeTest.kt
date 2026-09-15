package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.ui.theme.editorPalette
import com.rememberber.mootool.next.compose.ui.theme.resolveMooColors
import com.rememberber.mootool.next.compose.ui.theme.toAwtColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.fife.ui.rsyntaxtextarea.TokenTypes

class EditorThemeTest {
    @Test
    fun applyThemeUsesWorkspaceAndSyntaxTokens() {
        val colors = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        val palette = editorPalette(false, colors)
        val buffer = EditorBuffer("{ \"a\": 1 }")
        buffer.applyTheme(false, "Monospaced", 14, false, palette)
        assertEquals(palette.background.toAwtColor().rgb, buffer.area.background.rgb)
        assertEquals(palette.foreground.toAwtColor().rgb, buffer.area.foreground.rgb)
        assertEquals(palette.string.toAwtColor().rgb, buffer.area.syntaxScheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).foreground.rgb)
        assertEquals(palette.number.toAwtColor().rgb, buffer.area.syntaxScheme.getStyle(TokenTypes.LITERAL_NUMBER_DECIMAL_INT).foreground.rgb)
    }

    @Test
    fun lineSpacingMultiplierIncreasesRstaLineHeight() {
        val buffer = EditorBuffer("a\nb")
        buffer.applyTheme(false, "Monospaced", 14, true, lineSpacing = 1.0)
        val compact = buffer.area.lineHeight
        buffer.applyTheme(false, "Monospaced", 14, true, lineSpacing = 2.0)
        assertTrue(buffer.area.lineHeight >= compact * 2 - 1)
        assertTrue(compact > 0)
    }
}
