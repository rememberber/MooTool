package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UaEngineTest {
    @Test
    fun detectsChromeWindowsDesktop() {
        val chrome = UaEngine.parse(UaEngine.presets[0].second)
        assertEquals("Chrome", chrome.browser)
        assertTrue(chrome.browserVersion.startsWith("131"))
        assertEquals("Windows", chrome.os)
        assertEquals("Blink", chrome.engine)
        assertFalse(chrome.mobile)
        assertFalse(chrome.bot)
        assertEquals("desktop", chrome.deviceType)
    }

    @Test
    fun detectsSafariIphoneAndAndroidChrome() {
        val iphone = UaEngine.parse(UaEngine.presets[3].second)
        assertTrue(iphone.mobile)
        assertEquals("Safari", iphone.browser)
        assertEquals("iOS", iphone.os)
        assertEquals("Apple", iphone.deviceBrand)
        assertEquals("WebKit", iphone.engine)
        val android = UaEngine.parse(UaEngine.presets[4].second)
        assertTrue(android.mobile)
        assertEquals("Chrome", android.browser)
        assertEquals("Android", android.os)
    }

    @Test
    fun detectsFirefoxAndBotsAndUnknown() {
        val firefox = UaEngine.parse(UaEngine.presets[2].second)
        assertEquals("Firefox", firefox.browser)
        assertEquals("Gecko", firefox.engine)
        val bot = UaEngine.parse("Mozilla/5.0 Googlebot/2.1")
        assertTrue(bot.bot)
        assertEquals("bot", bot.deviceType)
        val unknown = UaEngine.parse("totally-unknown-agent/0")
        assertEquals(UaEngine.UNKNOWN, unknown.browser)
        assertEquals(UaEngine.UNKNOWN, unknown.os)
        assertFailsWith<UaException> { UaEngine.parse("   ") }
    }
}
