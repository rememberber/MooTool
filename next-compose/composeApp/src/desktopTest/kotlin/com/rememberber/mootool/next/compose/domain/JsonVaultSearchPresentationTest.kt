package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonVaultSearchPresentationTest {
    @Test
    fun normalizeQueryTrimsAndLowercases() {
        assertEquals("readme", JsonVaultSearchPresentation.normalizeQuery("  ReadMe "))
        assertFalse(JsonVaultSearchPresentation.isFiltering("   "))
        assertTrue(JsonVaultSearchPresentation.isFiltering("x"))
    }

    @Test
    fun pathAndContentMatching() {
        assertTrue(JsonVaultSearchPresentation.pathSegmentMatches("vault/readme.json", "readme.json", "readme"))
        assertFalse(JsonVaultSearchPresentation.pathSegmentMatches("vault/a.json", "a.json", "missing"))
        assertTrue(JsonVaultSearchPresentation.titleMatches("My Title", "title"))
        assertFalse(JsonVaultSearchPresentation.contentMatches("secret", "secret", includeContent = false))
        assertTrue(JsonVaultSearchPresentation.contentMatches("has secret inside", "secret", includeContent = true))
    }
}
