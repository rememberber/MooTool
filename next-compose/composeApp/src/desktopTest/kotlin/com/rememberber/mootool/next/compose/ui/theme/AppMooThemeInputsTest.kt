package com.rememberber.mootool.next.compose.ui.theme

import com.rememberber.mootool.next.compose.model.AppearanceSettings
import com.rememberber.mootool.next.compose.model.LayoutSettings
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ThemePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppMooThemeInputsTest {
    @Test
    fun mainAndDetachedWindowsShareCompactNavigationAndAppearance() {
        val settings = AppSettings(
            appearance = AppearanceSettings(
                interfaceStyle = "smartisan",
                accentColor = "orange",
                unifiedBackground = false,
                fontFamily = "mono",
            ),
            layout = LayoutSettings(compactNavigation = true),
        )
        val main = AppMooThemeInputs.from(ThemePreference.Dark, systemDark = false, settings)
        val detached = AppMooThemeInputs.from(ThemePreference.Dark, systemDark = false, settings)
        assertEquals(main, detached)
        assertTrue(main.compactNavigation)
        assertEquals("smartisan", main.interfaceStyle)
        assertEquals("orange", main.accentColor)
        assertEquals("mono", main.fontFamily)
    }
}
