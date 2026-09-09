package com.rememberber.mootool.next.compose.model

import kotlinx.serialization.Serializable

const val SETTINGS_SCHEMA_VERSION = 1

@Serializable
data class CustomToolGroup(
    val id: String,
    val name: String,
    val toolIds: List<String> = emptyList()
)

@Serializable
data class AppSettings(
    val schemaVersion: Int = SETTINGS_SCHEMA_VERSION,
    val general: GeneralSettings = GeneralSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val editor: EditorSettings = EditorSettings(),
    val network: NetworkSettings = NetworkSettings(),
    val data: DataSettings = DataSettings(),
    val vault: VaultSettings = VaultSettings(),
    val runtime: RuntimeSettings = RuntimeSettings(),
    val tools: ToolSettings = ToolSettings(),
    val shortcuts: ShortcutSettings = ShortcutSettings(),
    val workspace: WorkspaceSettings = WorkspaceSettings()
) {
    companion object {
        val Default: AppSettings = AppSettings()
    }
}

@Serializable
data class GeneralSettings(
    val language: String = AppLanguage.ZhCN.code,
    val autoCheckUpdates: Boolean = true,
    val autoDownloadUpdates: Boolean = false,
    val startMaximized: Boolean = false,
    val closeBehavior: String = CloseBehavior.Ask.name.lowercase(),
    val trayEnabled: Boolean = true
)

@Serializable
data class AppearanceSettings(
    val interfaceStyle: String = InterfaceStyle.Modern.name.lowercase(),
    val theme: String = ThemePreference.System.name.lowercase(),
    val accentColor: String = "blue",
    val fontFamily: String = "system",
    val fontSize: Int = 13,
    val unifiedBackground: Boolean = true
)

@Serializable
data class LayoutSettings(
    val showRecent: Boolean = false,
    val compactNavigation: Boolean = false,
    val showSeparators: Boolean = true,
    val hideNavigationTitles: Boolean = false,
    val navigationStyle: String = NavigationStyle.Classic.name.lowercase(),
    val customGroups: List<CustomToolGroup> = emptyList(),
    val hiddenNavigationToolIds: List<String> = emptyList(),
    val sidebarWidth: Float = 248f,
    val sidebarCollapsed: Boolean = false,
    val paneSizes: Map<String, List<Float>> = emptyMap()
)

@Serializable
data class EditorSettings(
    val sqlDialect: String = "mysql",
    val jsonFontName: String = "ui-monospace",
    val jsonFontSize: Int = 14,
    val quickNoteFontName: String = "ui-monospace",
    val quickNoteFontSize: Int = 14,
    val softWrap: Boolean = true
)

@Serializable
data class NetworkSettings(
    val proxyEnabled: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyUsername: String = "",
    val requestTimeoutMs: Int = 30_000,
    val translationTimeoutMs: Int = 15_000
)

@Serializable
data class DataSettings(
    val directory: String = ""
)

@Serializable
data class VaultSettings(
    val quickNotePath: String = "",
    val jsonPath: String = "",
    val gitRemote: String = "",
    val gitUsername: String = "",
    val autoCommit: Boolean = false,
    val autoCommitIdleSeconds: Int = 30,
    val autoCommitInactiveSeconds: Int = 120,
    val autoPullMinutes: Int = 0,
    val hideGitignoredFiles: Boolean = true,
    val jsonTreeExpandMode: String = "smart"
)

@Serializable
data class RuntimeSettings(
    val javaPath: String = "",
    val groovyPath: String = "",
    val pythonPath: String = "",
    val nodePath: String = ""
)

@Serializable
data class ToolSettings(
    val qrCodeSize: Int = 300,
    val qrErrorCorrection: String = "M",
    val randomStringLength: Int = 16,
    val exportDirectory: String = "",
    val translationProvider: String = "google",
    val translationSourceLang: String = "auto",
    val translationTargetLang: String = "zh-CN"
)

@Serializable
data class ShortcutSettings(
    val search: String = "Meta+K",
    val settings: String = "Meta+Comma"
)

@Serializable
data class WorkspaceSettings(
    val activeToolId: String = ToolId.Mootool.id,
    val recentToolIds: List<String> = emptyList(),
    val windowX: Int? = null,
    val windowY: Int? = null,
    val windowWidth: Int = 1440,
    val windowHeight: Int = 920
)

@Serializable
data class HistoryRecord(
    val id: Long = 0,
    val toolId: String,
    val operation: String,
    val summary: String,
    val input: String,
    val output: String,
    val options: String = "",
    val createdAt: String
)
