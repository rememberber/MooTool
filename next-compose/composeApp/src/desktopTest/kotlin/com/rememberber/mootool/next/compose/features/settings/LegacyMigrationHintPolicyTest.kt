package com.rememberber.mootool.next.compose.features.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegacyMigrationHintPolicyTest {
    @Test
    fun shows_when_not_dismissed_and_no_workspace_overlay() {
        assertTrue(shouldShowLegacyMigrationHint(dismissed = false, settingsOpen = false, searchOpen = false, groupManagerOpen = false))
    }

    @Test
    fun hidden_when_dismissed_or_overlay_open() {
        assertFalse(shouldShowLegacyMigrationHint(dismissed = true, settingsOpen = false, searchOpen = false, groupManagerOpen = false))
        assertFalse(shouldShowLegacyMigrationHint(dismissed = false, settingsOpen = true, searchOpen = false, groupManagerOpen = false))
        assertFalse(shouldShowLegacyMigrationHint(dismissed = false, settingsOpen = false, searchOpen = true, groupManagerOpen = false))
        assertFalse(shouldShowLegacyMigrationHint(dismissed = false, settingsOpen = false, searchOpen = false, groupManagerOpen = true))
    }
}
