package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritePresentationTest {
    @Test
    fun matchesQuery_filtersByNameSnippetAndGroup() {
        assertTrue(FavoritePresentation.matchesQuery("daily", "", "Daily", "0 0 * * *", "work"))
        assertTrue(FavoritePresentation.matchesQuery("", "work", "Daily", "0 0 * * *", "work"))
        assertFalse(FavoritePresentation.matchesQuery("daily", "other", "Daily", "0 0 * * *", "work"))
        assertFalse(FavoritePresentation.matchesQuery("missing", "", "Daily", "0 0 * * *", "work"))
    }

    @Test
    fun defaultName_fallsBackToSnippet() {
        assertEquals(
            "expression-longer-than-thirty-tw",
            FavoritePresentation.defaultName("  ", "expression-longer-than-thirty-two-characters"),
        )
        assertEquals("Named", FavoritePresentation.defaultName(" Named ", "ignored"))
    }
}
