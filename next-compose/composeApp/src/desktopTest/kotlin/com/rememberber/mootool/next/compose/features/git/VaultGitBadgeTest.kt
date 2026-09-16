package com.rememberber.mootool.next.compose.features.git

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VaultGitBadgeTest {
    @Test
    fun formatBadgeCount() {
        assertNull(formatVaultGitBadgeCount(0))
        assertNull(formatVaultGitBadgeCount(-1))
        assertEquals("1", formatVaultGitBadgeCount(1))
        assertEquals("99", formatVaultGitBadgeCount(99))
        assertEquals("99+", formatVaultGitBadgeCount(100))
        assertEquals("99+", formatVaultGitBadgeCount(500))
    }

    @Test
    fun gitActionMenuLabelShowsCount() {
        assertEquals("Git", gitActionMenuLabel("Git", 0))
        assertEquals("Git (3)", gitActionMenuLabel("Git", 3))
        assertEquals("Git sync (99+)", gitActionMenuLabel("Git sync", 120))
    }
}
