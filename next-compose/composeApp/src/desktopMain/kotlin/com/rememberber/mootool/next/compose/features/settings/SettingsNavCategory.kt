package com.rememberber.mootool.next.compose.features.settings

internal enum class SettingsNavCategory {
    General,
    Appearance,
    Layout,
    Editor,
    Network,
    Data,
    Vault,
    Runtime,
    Ai,
    Tools,
    Shortcuts,
    About,
    ;

    fun storageId(): String = name.lowercase()
}

internal fun settingsNavCategoryFromStorageId(id: String): SettingsNavCategory =
    SettingsNavCategory.entries.find { it.name.equals(id, ignoreCase = true) } ?: SettingsNavCategory.General

/** Arrow Up/Down on settings left nav (align Electron category list keyboard). */
internal fun settingsNavCategoryStep(current: SettingsNavCategory, delta: Int): SettingsNavCategory {
    val entries = SettingsNavCategory.entries
    val next = (entries.indexOf(current) + delta).coerceIn(0, entries.lastIndex)
    return entries[next]
}
