package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TextDiffPresentationTest {
    @Test
    fun debounceMatchesScreen() {
        assertEquals(160L, TextDiffPresentation.AUTO_COMPARE_DEBOUNCE_MS)
    }

    @Test
    fun nextNavIndexMatchesDiffScreenNavigate() {
        assertEquals(0, TextDiffPresentation.nextNavIndex(-1, 1, 3))
        assertEquals(1, TextDiffPresentation.nextNavIndex(0, 1, 3))
        assertEquals(-1, TextDiffPresentation.nextNavIndex(0, 1, 0))
    }
}
