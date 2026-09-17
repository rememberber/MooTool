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
                if (dark) assertTrue(scrim.alpha >= 0.34f, "$style dark scrim ${scrim.alpha}")
            }
        }
    }

    @Test
    fun modernChromeTokensMatchElectronCss() {
        val light = resolveMooColors(false, "modern", "blue")
        assertEquals(Color(0xFFECECEF), light.borderSoft)
        assertEquals(Color(0xFFDCDCE0), light.borderControl)
        assertEquals(Color(0xFFF4F4F5), light.surfaceCard)
        assertEquals(Color(0xFFEDEDEE), light.surfaceCardHover)
        assertEquals(Color(0xFFCCCDD1), light.borderControlHover)
        assertEquals(Color(0xFF1D1E21), light.textStrong)
        assertEquals(Color(0xFF3D3E43), light.textBody)
        assertEquals(Color(0xFF76787E), light.textMuted)
        val dark = resolveMooColors(true, "modern", "blue")
        assertEquals(Color(0xFF303034), dark.borderSoft)
        assertEquals(Color(0xFF414146), dark.borderControl)
        assertEquals(Color(0xFF29292C), dark.surfaceCard)
        assertEquals(Color(0xFF303033), dark.surfaceCardHover)
        assertEquals(Color(0xFFF7F7F8), dark.textStrong)
        assertEquals(Color(0xFFD9D9DC), dark.textBody)
        assertEquals(Color(0xFFA8A8AE), dark.textMuted)
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
    fun heroClaudeRestoreWorkspaceEditorShell() {
        val hero = resolveMooColors(false, "hero", "blue")
        val claude = resolveMooColors(false, "claude", "blue")
        val modern = resolveMooColors(false, "modern", "blue")
        assertTrue(hero.restoresWorkspaceChrome())
        assertTrue(claude.restoresWorkspaceChrome())
        assertTrue(!modern.restoresWorkspaceChrome())
        assertTrue(!hero.flattenWorkspaceToolPanels())
        assertTrue(modern.flattenWorkspaceToolPanels())
    }

    @Test
    fun heroAndMiuiTokensFollowElectronCssVariables() {
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFFAFAFA), hero.sidebar)
        assertEquals(Color(0xFFF4F4F5), hero.workspace)
        assertEquals(Color(0xFF18181B), hero.textPrimary)
        assertEquals(Color(0xFFF0F0F2), hero.borderSoft)
        assertEquals(Color(0xFFD4D4D8), hero.borderControl)
        assertEquals(Color(0xFFF4F4F5), hero.surfaceCard)
        val heroDark = resolveMooColors(true, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF111113), heroDark.sidebar)
        assertEquals(Color(0xFF09090B), heroDark.workspace)
        val miui = resolveMooColors(false, "miui-v5", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFF2F2F2), miui.sidebar)
        assertEquals(Color(0xFFE9E9E9), miui.workspace)
        assertEquals(Color(0xFFF36C21), miui.navActiveBar)
        assertEquals(Color(0xFFE3E3E3), miui.borderSoft)
        assertEquals(Color(0xFFC5C5C5), miui.borderControl)
        assertEquals(Color(0xFFEEEEEE), miui.surfaceCard)
        val claude = resolveMooColors(false, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFEEECE5), claude.sidebar)
        assertEquals(Color(0xFFF7F5EF), claude.workspace)
        assertEquals(Color(0xFFD97757), claude.navActiveBar)
        assertEquals(Color(0xFFEBE7DF), claude.borderSoft)
        assertEquals(Color(0xFFD8D2C8), claude.borderControl)
        assertEquals(Color(0xFFF0EDE6), claude.surfaceCard)
        val smartisan = resolveMooColors(false, "smartisan", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFD6D1C9), smartisan.borderSoft)
        assertEquals(Color(0xFFBCB6AC), smartisan.borderControl)
        assertEquals(Color(0xFFE6E3DD), smartisan.surfaceCard)
        assertEquals(Color(0xFFD9D5CE), smartisan.surfaceCardHover)
        assertEquals(Color(0xFF9F988E), smartisan.borderControlHover)
        val quiet = resolveMooColors(false, "quiet", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFF2F2F3), quiet.surfaceCard)
        assertEquals(Color(0xFFE1E1E3), quiet.borderControl)
        assertEquals(Color(0xFFECECEE), quiet.surfaceCardHover)
        assertEquals(Color(0xFF27272A), quiet.textStrong)
        assertEquals(Color(0xFF45464A), quiet.textBody)
        assertEquals(Color(0xFF858589), quiet.textMuted)
        assertEquals(Color(0xFF09090B), hero.textStrong)
        assertEquals(Color(0xFF3F3F46), hero.textBody)
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

    @Test
    fun hoverAndPressedFillsCompositeTranslucentControlTokens() {
        val quiet = resolveMooColors(false, "quiet", "blue")
        val hovered = quiet.hoveredControlFill()
        val pressed = quiet.pressedControlFill()
        assertTrue(hovered != quiet.control, "quiet hover should tint control")
        assertTrue(pressed != quiet.control, "quiet press should tint control")
        assertTrue(hovered.alpha >= 0.99f)
        assertTrue(pressed.alpha >= 0.99f)
        val modern = resolveMooColors(false, "modern", "blue")
        assertEquals(Color(0xFFE7E7E9), modern.pressedControlFill())
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFE4E4E7), hero.pressedControlFill())
        assertEquals(Color(0xFFA1A1AA), hero.borderControlHover)
    }

    @Test
    fun desktopControlDimensMatchElectron() {
        val modern = resolveMooDimens("modern")
        assertEquals(34, modern.controlHeight.value.toInt())
        assertEquals(9, modern.radius.value.toInt())
        assertEquals(48, modern.toolbar.value.toInt())
        val hero = resolveMooDimens("hero")
        assertEquals(36, hero.controlHeight.value.toInt())
        assertEquals(12, hero.radius.value.toInt())
        assertEquals(52, hero.toolbar.value.toInt())
        val claude = resolveMooDimens("claude")
        assertEquals(10, claude.radius.value.toInt())
        assertEquals(34, claude.controlHeight.value.toInt())
        val smartisan = resolveMooDimens("smartisan")
        assertEquals(7, smartisan.radius.value.toInt())
        assertEquals(34, smartisan.controlHeight.value.toInt())
        val miui = resolveMooDimens("miui-v5")
        assertEquals(4, miui.radius.value.toInt())
        assertEquals(34, miui.controlHeight.value.toInt())
        val quiet = resolveMooDimens("quiet")
        assertEquals(30, quiet.controlHeight.value.toInt())
        assertEquals(6, quiet.radius.value.toInt())
    }

    @Test
    fun prominentButtonTokensMatchElectronDesktopSystem() {
        val light = resolveMooColors(false, "modern", "blue")
        assertEquals(Color(0xFF303135), light.prominentFill(false, hovered = false, pressed = false))
        assertEquals(Color.White, light.prominentContent(false))
        val dark = resolveMooColors(true, "modern", "blue")
        assertEquals(Color(0xFFEEEEEF), dark.prominentFill(true, hovered = false, pressed = false))
        assertEquals(Color(0xFF242426), dark.prominentContent(true))
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertEquals(hero.accentAction, hero.prominentFill(false, hovered = false, pressed = false))
        val miui = resolveMooColors(false, "miui-v5", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFF36C21), miui.prominentFill(false, hovered = false, pressed = false))
        val claude = resolveMooColors(false, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFD97757), claude.prominentFill(false, hovered = false, pressed = false))
        assertEquals(Color.White, claude.prominentContent(false))
    }

    @Test
    fun navSelectedTokensFollowElectronStyleBlocks() {
        val claude = resolveMooColors(false, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFF6E7DF), claude.navSelectedFill())
        assertEquals(Color(0xFFBD6348), claude.navSelectedContent())
        val claudeDark = resolveMooColors(true, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF4A3129), claudeDark.navSelectedFill())
        assertEquals(Color(0xFFF2A087), claudeDark.navSelectedContent())
        val miui = resolveMooColors(false, "miui-v5", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFD95312), miui.navSelectedContent())
        val modern = resolveMooColors(false, "modern", "blue")
        assertEquals(modern.textStrong, modern.navSelectedContent())
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertTrue(hero.navSelectedFill() != hero.selected)
        assertEquals(hero.accentAction, hero.navSelectedContent())
        val heroLightScrim = hero.overlayScrim()
        assertTrue(heroLightScrim.alpha >= 0.4f, "hero light scrim ${heroLightScrim.alpha}")
    }

    @Test
    fun chromeRadiiMatchElectronStyleBlocks() {
        val modern = resolveMooDimens("modern")
        assertEquals(7, modern.navRadius.value.toInt())
        assertEquals(13, modern.dialogRadius.value.toInt())
        assertEquals(8, modern.commandRadius.value.toInt())
        assertEquals(54, modern.settingsRowMin.value.toInt())
        val hero = resolveMooDimens("hero")
        assertEquals(10, hero.navRadius.value.toInt())
        assertEquals(16, hero.dialogRadius.value.toInt())
        assertEquals(16, hero.commandRadius.value.toInt())
        assertEquals(58, hero.settingsRowMin.value.toInt())
        val claude = resolveMooDimens("claude")
        assertEquals(9, claude.navRadius.value.toInt())
        assertEquals(16, claude.dialogRadius.value.toInt())
        val miui = resolveMooDimens("miui-v5")
        assertEquals(4, miui.navRadius.value.toInt())
        assertEquals(8, miui.dialogRadius.value.toInt())
        assertEquals(8, modern.shellRadius.value.toInt())
        assertEquals(5, modern.shellElevation.value.toInt())
        assertEquals(8, modern.cardRadius.value.toInt())
        assertEquals(14, hero.shellRadius.value.toInt())
        assertEquals(8, hero.shellElevation.value.toInt())
        assertEquals(12, hero.cardRadius.value.toInt())
        assertEquals(12, claude.shellRadius.value.toInt())
        assertEquals(6, claude.shellElevation.value.toInt())
        assertEquals(12, claude.cardRadius.value.toInt())
        assertEquals(5, miui.shellRadius.value.toInt())
        assertEquals(4, miui.cardRadius.value.toInt())
        val smartisan = resolveMooDimens("smartisan")
        assertEquals(10, smartisan.shellRadius.value.toInt())
        assertEquals(7, smartisan.shellElevation.value.toInt())
        val quiet = resolveMooDimens("quiet")
        assertEquals(8, quiet.shellRadius.value.toInt())
        assertEquals(4, quiet.shellElevation.value.toInt())
        assertEquals(6, quiet.cardRadius.value.toInt())
    }

    @Test
    fun tooltipAndScrimTokensMatchElectronStyleBlocks() {
        val modern = resolveMooColors(false, "modern", "blue")
        assertEquals(Color(0xFF2E2E31), modern.tooltipFill())
        assertEquals(Color.White, modern.tooltipContent())
        assertEquals(6, modern.tooltipRadius().value.toInt())
        assertEquals(0.24f, modern.overlayScrim().alpha, 0.02f)
        val modernDark = resolveMooColors(true, "modern", "blue")
        assertEquals(Color(0xFFEDEDF0), modernDark.tooltipFill())
        assertEquals(Color(0xFF242426), modernDark.tooltipContent())
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF18181B), hero.tooltipFill())
        assertEquals(Color(0xFFFAFAFA), hero.tooltipContent())
        assertEquals(9, hero.tooltipRadius().value.toInt())
        assertEquals(0.46f, hero.overlayScrim().alpha, 0.02f)
        val smartisan = resolveMooColors(false, "smartisan", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF393632), smartisan.tooltipFill())
        assertEquals(Color(0xFFF5F3EF), smartisan.tooltipContent())
        assertEquals(0.42f, smartisan.overlayScrim().alpha, 0.02f)
        val miui = resolveMooColors(false, "miui-v5", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF333333), miui.tooltipFill())
        assertEquals(4, miui.tooltipRadius().value.toInt())
        assertEquals(0.38f, miui.overlayScrim().alpha, 0.02f)
        val claude = resolveMooColors(false, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF302B27), claude.tooltipFill())
        assertEquals(Color(0xFFFFFAF3), claude.tooltipContent())
        assertEquals(8, claude.tooltipRadius().value.toInt())
        assertEquals(0.34f, claude.overlayScrim().alpha, 0.02f)
    }

    @Test
    fun tactileStylesExposeRaisedChromeForButtons() {
        val smartisan = resolveMooColors(false, "smartisan", "blue", unifiedBackground = false)
        assertTrue(smartisan.raisedChrome)
        assertTrue(smartisan.raisedTop != smartisan.raisedBottom)
        val miui = resolveMooColors(false, "miui-v5", "blue", unifiedBackground = false)
        assertTrue(miui.raisedChrome)
        assertTrue(miui.raisedTop != miui.raisedBottom)
        val modern = resolveMooColors(false, "modern", "blue")
        assertTrue(!modern.raisedChrome)
    }

    @Test
    fun navItemChromeFollowsElectronStyleBlocks() {
        val modern = resolveMooColors(false, "modern", "blue")
        assertEquals(Color.Transparent, modern.navItemBorder(selected = true, hovered = false))
        assertEquals(modern.navSelectedContent(), modern.navSelectedIcon())
        val smartisan = resolveMooColors(false, "smartisan", "blue", unifiedBackground = false)
        assertEquals(smartisan.borderControl, smartisan.navItemBorder(selected = true, hovered = false))
        assertEquals(smartisan.navActiveBar, smartisan.navSelectedIcon())
        val miui = resolveMooColors(false, "miui-v5", "blue", unifiedBackground = false)
        assertEquals(miui.border, miui.navItemBorder(selected = true, hovered = false))
        assertEquals(miui.borderSoft, miui.navItemBorder(selected = false, hovered = true))
        assertEquals(miui.navActiveBar, miui.navSelectedIcon())
        val claude = resolveMooColors(false, "claude", "blue", unifiedBackground = false)
        assertEquals(0.16f, claude.navItemBorder(selected = true, hovered = false).alpha, 0.02f)
        assertEquals(claude.accent, claude.navSelectedIcon())
    }
}
