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

    /** Electron `settings.category.*` label keys (SettingsWindow nav + content header). */
    fun categoryLabelKey(): String = "settings.category.${name.lowercase()}"

    /** Monochrome nav glyph aligned with Electron Lucide category icons (decorative). */
    fun navIcon(): String = when (this) {
        General -> "⚙"
        Appearance -> "☼"
        Layout -> "▦"
        Editor -> "{ }"
        Network -> "⬡"
        Data -> "▤"
        Vault -> "⌁"
        Runtime -> ">_"
        Ai -> "⚡"
        Tools -> "☰"
        Shortcuts -> "⌘"
        About -> "ℹ"
    }
}

internal fun settingsNavCategoryFromStorageId(id: String): SettingsNavCategory =
    SettingsNavCategory.entries.find { it.name.equals(id, ignoreCase = true) } ?: SettingsNavCategory.General

/** Arrow Up/Down on settings left nav (align Electron category list keyboard). */
internal fun settingsNavCategoryStep(current: SettingsNavCategory, delta: Int): SettingsNavCategory {
    val entries = SettingsNavCategory.entries
    val next = (entries.indexOf(current) + delta).coerceIn(0, entries.lastIndex)
    return entries[next]
}
