package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ColorSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorHistoryRestoreTest {
    @Test
    fun restoresPrimaryAndFormatFromLegacyOptions() {
        val session = ColorSession()
        session.primary = ColorEngine.parseColor("#000000")
        val item = HistoryRecord(
            toolId = "color",
            operation = "pick",
            summary = "pick",
            input = "#111111",
            output = "#FF00AA",
            options = ColorFormat.RGB.name,
            createdAt = "",
        )
        assertEquals(ColorHistoryRestore.Result.Ok, ColorHistoryRestore.apply(session, item))
        assertEquals(ColorFormat.RGB, session.format)
        assertEquals("#FF00AA", session.primaryHex)
    }

    @Test
    fun restoresSwapPairFromOutput() {
        val session = ColorSession()
        val item = HistoryRecord(
            toolId = "color",
            operation = "swap",
            summary = "swap",
            input = "#111111 / #222222",
            output = "#333333 / #444444",
            options = ColorHistoryMetadata.encode(ColorFormat.HEX_UPPER, "swap"),
            createdAt = "",
        )
        assertEquals(ColorHistoryRestore.Result.Ok, ColorHistoryRestore.apply(session, item))
        assertEquals("#333333", session.primaryHex)
        assertEquals("#444444", session.secondaryHex)
    }
}
