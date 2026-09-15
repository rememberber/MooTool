package com.rememberber.mootool.next.compose.ui.theme

import androidx.compose.ui.graphics.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeContrastTest {
    @Test
    fun bodyTextMeetsContrastFloorAcrossStyles() {
        val styles = listOf("modern", "quiet", "hero", "smartisan", "miui-v5", "claude")
        for (dark in listOf(false, true)) {
            for (style in styles) {
                val colors = resolveMooColors(dark, style, "blue")
                val body = contrastRatio(colors.textPrimary, colors.workspace)
                val sidebar = contrastRatio(colors.textPrimary, colors.sidebar)
                val focus = contrastRatio(colors.focusRing, colors.workspace)
                val scrim = colors.overlayScrim()
                assertTrue(body >= 4.5f, "$style dark=$dark body contrast $body")
                assertTrue(sidebar >= 4.5f, "$style dark=$dark sidebar contrast $sidebar")
                assertTrue(focus >= 3f, "$style dark=$dark focus contrast $focus")
                assertTrue(scrim.alpha >= 0.2f, "$style dark=$dark scrim ${scrim.alpha}")
                if (dark) assertTrue(scrim.alpha >= 0.4f, "$style dark scrim ${scrim.alpha}")
            }
        }
    }

    @Test
    fun unifiedBackgroundChangesModernWorkspace() {
        for (dark in listOf(false, true)) {
            val unified = resolveMooColors(dark, "modern", "blue", unifiedBackground = true)
            val split = resolveMooColors(dark, "modern", "blue", unifiedBackground = false)
            assertTrue(unified.workspace != split.workspace, "dark=$dark workspace should change")
            val body = contrastRatio(split.textPrimary, split.workspace)
            assertTrue(body >= 4.5f, "dark=$dark split workspace contrast $body")
        }
    }

    @Test
    fun heroAndMiuiTokensFollowElectronCssVariables() {
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFFAFAFA), hero.sidebar)
        assertEquals(Color(0xFFF4F4F5), hero.workspace)
        assertEquals(Color(0xFF18181B), hero.textPrimary)
        val heroDark = resolveMooColors(true, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF111113), heroDark.sidebar)
        assertEquals(Color(0xFF09090B), heroDark.workspace)
        val miui = resolveMooColors(false, "miui-v5", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFF2F2F2), miui.sidebar)
        assertEquals(Color(0xFFE9E9E9), miui.workspace)
        assertEquals(Color(0xFFF36C21), miui.navActiveBar)
        val claude = resolveMooColors(false, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFEEECE5), claude.sidebar)
        assertEquals(Color(0xFFF7F5EF), claude.workspace)
        assertEquals(Color(0xFFD97757), claude.navActiveBar)
    }

    @Test
    fun electronAccentIdsMatchPresets() {
        assertEquals(listOf("yellow", "coral", "blue", "green", "red", "purple"), AccentPresets.ids)
        assertEquals("yellow", AccentPresets.normalize("orange"))
        assertEquals("green", AccentPresets.normalize("teal"))
        val yellow = resolveMooColors(false, "modern", "yellow")
        assertEquals(Color(0xFFE0B22B), yellow.accent)
        assertEquals(Color(0xFFB78300), yellow.accentAction)
        val coral = resolveMooColors(false, "modern", "coral")
        assertEquals(Color(0xFFDE8F7D), coral.accent)
        for (id in AccentPresets.ids) {
            val colors = resolveMooColors(false, "modern", id)
            assertTrue(contrastRatio(colors.textPrimary, colors.workspace) >= 4.5f, "$id body")
            assertTrue(contrastRatio(colors.focusRing, colors.workspace) >= 3f, "$id focus")
        }
    }

    @Test
    fun writeStyleBoardsForAuxiliaryEvidence() {
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-14-shell-theme/boards")
        dir.mkdirs()
        val styles = listOf("modern", "quiet", "hero", "smartisan", "miui-v5", "claude")
        for (dark in listOf(false, true)) {
            for (style in styles) {
                val colors = resolveMooColors(dark, style, "blue", unifiedBackground = false)
                val image = BufferedImage(320, 80, BufferedImage.TYPE_INT_RGB)
                val g = image.createGraphics()
                try {
                    g.color = colors.sidebar.toAwtColor()
                    g.fillRect(0, 0, 80, 80)
                    g.color = colors.workspace.toAwtColor()
                    g.fillRect(80, 0, 160, 80)
                    g.color = colors.accent.toAwtColor()
                    g.fillRect(240, 0, 80, 80)
                    g.color = colors.textPrimary.toAwtColor()
                    g.drawString(style + if (dark) " dark" else " light", 88, 46)
                } finally {
                    g.dispose()
                }
                val file = File(dir, "${style}-${if (dark) "dark" else "light"}.png")
                assertTrue(ImageIO.write(image, "png", file), "write ${file.name}")
                assertTrue(file.length() > 80)
            }
        }
    }
}
