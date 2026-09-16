package com.rememberber.mootool.next.compose.features.settings

internal fun shouldShowLegacyMigrationHint(
    dismissed: Boolean,
    settingsOpen: Boolean,
    searchOpen: Boolean,
    groupManagerOpen: Boolean,
): Boolean = !dismissed && !settingsOpen && !searchOpen && !groupManagerOpen
