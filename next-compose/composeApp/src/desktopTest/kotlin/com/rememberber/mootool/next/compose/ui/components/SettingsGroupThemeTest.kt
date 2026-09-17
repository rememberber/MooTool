package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.ui.graphics.Color
import com.rememberber.mootool.next.compose.ui.theme.resolveMooColors
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsGroupThemeTest {
    @Test
    fun settingsGroupRowsFillUsesElectronSurfaceNotSurfaceCard() {
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFFFFFFF), hero.settingsGroupRowsFill())
        assertEquals(hero.raisedTop, hero.settingsGroupRowsFill())
        assertEquals(Color(0xFFF4F4F5), hero.workspace)
        assertEquals(Color(0xFFF4F4F5), hero.surfaceCard)

        val claude = resolveMooColors(false, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFFFFEFA), claude.settingsGroupRowsFill())
        assertEquals(Color(0xFFF0EDE6), claude.surfaceCard)

        val smartisan = resolveMooColors(false, "smartisan", "blue", unifiedBackground = false)
        assertEquals(Color(0xFFF5F3EF), smartisan.settingsGroupRowsFill())
        assertEquals(Color(0xFFE6E3DD), smartisan.surfaceCard)

        val heroDark = resolveMooColors(true, "hero", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF18181B), heroDark.settingsGroupRowsFill())
        assertEquals(heroDark.toolbar, heroDark.settingsGroupRowsFill())

        val claudeDark = resolveMooColors(true, "claude", "blue", unifiedBackground = false)
        assertEquals(Color(0xFF2B2824), claudeDark.settingsGroupRowsFill())
        assertEquals(claudeDark.raisedBottom, claudeDark.settingsGroupRowsFill())
    }

    @Test
    fun settingsGroupRowsBorderMatchesElectronStyleBlocks() {
        val hero = resolveMooColors(false, "hero", "blue", unifiedBackground = false)
        assertEquals(hero.borderSoft, hero.settingsGroupRowsBorder())
        val smartisan = resolveMooColors(false, "smartisan", "blue", unifiedBackground = false)
        assertEquals(smartisan.borderControl, smartisan.settingsGroupRowsBorder())
        val modern = resolveMooColors(false, "modern", "blue")
        assertEquals(modern.border, modern.settingsGroupRowsBorder())
    }
}
