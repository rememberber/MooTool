package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageBoardEngineTest {
    @Test
    fun clipsUtf16LengthLikeJavascript() {
        assertEquals("abcdefghij", MessageBoardEngine.clip("abcdefghij"))
        assertEquals("a".repeat(80), MessageBoardEngine.clip("a".repeat(90)))
        assertEquals(80, MessageBoardEngine.clip("你".repeat(90)).length)
        assertEquals(2, MessageBoardEngine.clip("😀").length)
    }

    @Test
    fun normalizesSizeAndKeepsThemeOrder() {
        assertEquals(70, MessageBoardEngine.normalizeSize(68))
        assertEquals(102, MessageBoardEngine.normalizeSize(102))
        assertEquals(100, MessageBoardEngine.snapSize(102))
        assertEquals(130, MessageBoardEngine.normalizeSize(200))
        assertEquals(8, MessageBoardEngine.presets.size)
        assertEquals(
            listOf(BoardTheme.Sunbeam, BoardTheme.Coral, BoardTheme.Paper, BoardTheme.Cobalt, BoardTheme.Forest, BoardTheme.Midnight, BoardTheme.Cobalt, BoardTheme.Forest),
            MessageBoardEngine.presets.map { it.theme }
        )
        assertEquals(BoardThemeColors("#F4CE57", "#183832"), MessageBoardEngine.theme(BoardTheme.Sunbeam))
        assertEquals(BoardTheme.Midnight, MessageBoardEngine.themeId("midnight"))
        assertEquals(BoardAlignment.Left, MessageBoardEngine.alignmentId("left"))
    }

    @Test
    fun fitsFontSizeWithElectronSearchBounds() {
        val fitted = MessageBoardEngine.fitFontSize(400, 200, 100) { fontPx -> fontPx <= 40 }
        assertEquals(40, fitted)
        val min = MessageBoardEngine.fitFontSize(400, 200, 100) { false }
        assertEquals(MessageBoardEngine.MIN_FONT_PX, min)
        assertEquals(MessageBoardEngine.MIN_FONT_PX, MessageBoardEngine.fitFontSize(0, 100, 100) { true })
    }
}

class DisplayWakeLockTest {
    @Test
    fun acquiresOnceAndReleasesLastHolder() {
        var live = false
        val lock = DisplayWakeLock {
            live = true
            object : WakeSession {
                override fun isAlive(): Boolean = live
                override fun stop() { live = false }
            }
        }
        assertTrue(lock.acquire("a"))
        assertTrue(lock.acquire("b"))
        assertEquals(2, lock.holderCount())
        lock.release("a")
        assertTrue(lock.isActive())
        lock.release("b")
        assertFalse(lock.isActive())
        assertEquals(0, lock.holderCount())
        lock.acquire("c")
        lock.releaseAll()
        assertFalse(lock.isActive())
    }
}
