package com.rememberber.mootool.next.compose.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutPaneSizesTest {
    @Test
    fun storesPerToolPaneWidthsAndClamps() {
        val updated = LayoutSettings().withPane("json", 0, 260f, 2).withPane("json", 1, 300f, 2)
        assertEquals(260f, updated.pane("json", 0, 240f, 200f, 320f))
        assertEquals(300f, updated.pane("json", 1, 280f, 240f, 340f))
        assertEquals(240f, updated.pane("quickNote", 0, 240f, 200f, 320f))
        val clamped = updated.withPane("json", 0, 12f, 2)
        assertEquals(200f, clamped.pane("json", 0, 240f, 200f, 320f))
        val wide = updated.withPane("host", 0, 900f, 1)
        assertEquals(320f, wide.pane("host", 0, 240f, 200f, 320f))
        val ua = LayoutSettings().withPane("uaParse", 0, 420f, 1)
        assertEquals(420f, ua.pane("uaParse", 0, 400f, 300f, 600f))
    }
}
