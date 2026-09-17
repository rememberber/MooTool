package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.CustomToolGroup
import com.rememberber.mootool.next.compose.model.InterfaceStyle
import com.rememberber.mootool.next.compose.model.NavigationStyle
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.model.ToolId
/** Aligns with Electron `normalizeSettings` layout/appearance slices in `settings.ts`. */
object SettingsLayoutNormalize {
    private val paneKeyPattern = Regex("^[a-z0-9-]{1,64}$", RegexOption.IGNORE_CASE)
    private val customGroupIdPattern = Regex("^[a-z0-9_-]{1,80}$", RegexOption.IGNORE_CASE)
    private val accentColorPresetIds = setOf("yellow", "coral", "blue", "green", "red", "purple")

    fun apply(settings: AppSettings, defaults: AppSettings = AppSettings.Default): AppSettings {
        val base = SettingsGeneralNormalize.apply(settings, defaults)
        val normalized = base.copy(
            appearance = base.appearance.copy(
                interfaceStyle = normalizeInterfaceStyle(base.appearance.interfaceStyle, defaults.appearance.interfaceStyle),
                theme = normalizeTheme(base.appearance.theme, defaults.appearance.theme),
                accentColor = normalizeAccentColor(base.appearance.accentColor, defaults.appearance.accentColor),
                fontFamily = normalizeUiFontFamily(base.appearance.fontFamily, defaults.appearance.fontFamily),
            ),
            layout = base.layout.copy(
                navigationStyle = normalizeNavigationStyle(base.layout.navigationStyle, defaults.layout.navigationStyle),
                customGroups = normalizeCustomGroups(base.layout.customGroups),
                paneSizes = sanitizePaneSizes(base.layout.paneSizes),
            ),
        )
        return SettingsVaultGitNormalize.apply(normalized)
    }

    fun normalizeInterfaceStyle(value: String, fallback: String = InterfaceStyle.Modern.name.lowercase()): String {
        val normalized = value.trim().lowercase()
        return when (normalized) {
            "miui-v5", "miuiv5" -> "miui-v5"
            "modern", "quiet", "hero", "smartisan", "claude" -> normalized
            else -> fallback
        }
    }

    fun normalizeTheme(value: String, fallback: String = ThemePreference.System.name.lowercase()): String =
        ThemePreference.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }?.name?.lowercase()
            ?: fallback

    /** Aligns with Electron `accentColorPresets` and legacy Java `orange`/`teal` aliases in theme swatches. */
    fun normalizeAccentColor(value: String, fallback: String = "blue"): String {
        val normalized = value.trim().lowercase()
        val mapped = when (normalized) {
            "orange" -> "yellow"
            "teal" -> "green"
            else -> normalized
        }
        return if (mapped in accentColorPresetIds) mapped else fallback
    }

    /** Trims UI font id, maps Electron `system-ui` to Compose `system` preset. */
    fun normalizeUiFontFamily(value: String, fallback: String = "system"): String {
        val trimmed = EditorFontSettings.normalizeFontName(value, fallback)
        return when (trimmed.lowercase()) {
            "system-ui" -> "system"
            "system", "sans-serif", "serif", "monospace" -> trimmed.lowercase()
            else -> trimmed
        }
    }

    fun normalizeNavigationStyle(value: String, fallback: String = NavigationStyle.Classic.name.lowercase()): String =
        NavigationStyle.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }?.name?.lowercase()
            ?: fallback

    fun normalizeCustomGroups(groups: List<CustomToolGroup>): List<CustomToolGroup> {
        val usedIds = mutableSetOf<String>()
        return groups.take(32).mapIndexedNotNull { index, candidate ->
            val name = candidate.name.trim().take(64)
            if (name.isEmpty()) return@mapIndexedNotNull null

            val requestedId = candidate.id.trim().take(80).let { raw ->
                if (raw.isNotEmpty() && customGroupIdPattern.matches(raw)) raw else "custom-${index + 1}"
            }
            var id = requestedId
            var suffix = 2
            while (usedIds.contains(id)) {
                id = "$requestedId-$suffix"
                suffix++
            }
            usedIds += id

            val toolIds = candidate.toolIds
                .mapNotNull { ToolId.fromId(it) }
                .filter { it != ToolId.Mootool }
                .distinct()

            CustomToolGroup(id = id, name = name, toolIds = toolIds.map { it.id })
        }
    }

    /** Drops unsafe pane keys and invalid size vectors. Compose persists absolute dp widths per tool slot. */
    fun sanitizePaneSizes(value: Map<String, List<Float>>): Map<String, List<Float>> =
        value.entries
            .take(64)
            .mapNotNull { (key, sizes) ->
                if (!paneKeyPattern.matches(key)) return@mapNotNull null
                if (sizes.size !in 2..4) return@mapNotNull null
                if (sizes.any { !it.isFinite() || it <= 0f }) return@mapNotNull null
                key to sizes
            }
            .toMap()
}
