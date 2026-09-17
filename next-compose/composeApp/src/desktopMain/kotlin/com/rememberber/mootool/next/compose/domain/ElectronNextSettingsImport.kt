package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.domain.NavigationToolVisibility
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ToolSettings
import com.rememberber.mootool.next.compose.model.CustomToolGroup
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CodeRunSessionSnapshot
import com.rememberber.mootool.next.compose.storage.VaultPathConfig
import com.rememberber.mootool.next.compose.ui.components.normalizeVaultTreeExpandMode
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ElectronNextSettingsImport {
    const val WARNING_ENCRYPTED_SECRETS_SKIPPED = "secretsSkipped"

    /** Electron `mergeSettings`: upgrades before schema 11 should not see the first-run Java migration tip. */
    private const val ELECTRON_LEGACY_MIGRATION_HINT_SCHEMA = 11

    private val json = Json { ignoreUnknownKeys = true }

    /** Electron `safeStorage` ciphertext lives in the store root `secrets` object; compose cannot decrypt it. */
    fun hasEncryptedSecretsInStore(store: Path): Boolean {
        val root = runCatching { json.parseToJsonElement(store.readText()).jsonObject }.getOrNull() ?: return false
        val secrets = root["secrets"]?.jsonObject ?: return false
        return secrets.values.any { element ->
            element.jsonPrimitive.contentOrNull?.trim()?.isNotEmpty() == true
        }
    }

    fun loadFromStore(store: Path): AppSettings? {
        val root = runCatching { json.parseToJsonElement(store.readText()).jsonObject }.getOrNull() ?: return null
        val settings = root["settings"] ?: return null
        val retainSecrets = !hasEncryptedSecretsInStore(store)
        return runCatching { json.decodeFromJsonElement<AppSettings>(settings) }.getOrNull()?.let { sanitize(it, retainSecrets) }
    }

    fun shouldRetainPlaintextSecrets(store: Path): Boolean = !hasEncryptedSecretsInStore(store)

    fun loadCodeRunPatchFromStore(store: Path): CodeRunSessionSnapshot? {
        val runtime = runCatching {
            json.parseToJsonElement(store.readText()).jsonObject["settings"]?.jsonObject?.get("runtime")?.jsonObject
        }.getOrNull() ?: return null
        return parseCodeRunPatch(runtime)
    }

    fun mergeCodeRunSnapshots(current: CodeRunSessionSnapshot, patch: CodeRunSessionSnapshot): CodeRunSessionSnapshot {
        val normalizedPatch = CodeRunRuntimeOptionsNormalize.normalizeSnapshot(patch)
        fun pick(patchValue: String, currentValue: String): String =
            patchValue.takeIf { it.isNotEmpty() } ?: currentValue
        return CodeRunRuntimeOptionsNormalize.normalizeSnapshot(
            current.copy(
                javaCode = pick(normalizedPatch.javaCode, current.javaCode),
                groovyCode = pick(normalizedPatch.groovyCode, current.groovyCode),
                pythonCode = pick(normalizedPatch.pythonCode, current.pythonCode),
                nodeCode = pick(normalizedPatch.nodeCode, current.nodeCode),
                javaArguments = pick(normalizedPatch.javaArguments, current.javaArguments),
                groovyArguments = pick(normalizedPatch.groovyArguments, current.groovyArguments),
                pythonArguments = pick(normalizedPatch.pythonArguments, current.pythonArguments),
                nodeArguments = pick(normalizedPatch.nodeArguments, current.nodeArguments),
                javaWorkingDirectory = pick(normalizedPatch.javaWorkingDirectory, current.javaWorkingDirectory),
                groovyWorkingDirectory = pick(normalizedPatch.groovyWorkingDirectory, current.groovyWorkingDirectory),
                pythonWorkingDirectory = pick(normalizedPatch.pythonWorkingDirectory, current.pythonWorkingDirectory),
                nodeWorkingDirectory = pick(normalizedPatch.nodeWorkingDirectory, current.nodeWorkingDirectory),
            ),
        )
    }

    fun hasCodeRunPatch(patch: CodeRunSessionSnapshot): Boolean =
        patch.javaCode.isNotEmpty() ||
            patch.groovyCode.isNotEmpty() ||
            patch.pythonCode.isNotEmpty() ||
            patch.nodeCode.isNotEmpty() ||
            patch.javaArguments.isNotEmpty() ||
            patch.groovyArguments.isNotEmpty() ||
            patch.pythonArguments.isNotEmpty() ||
            patch.nodeArguments.isNotEmpty() ||
            patch.javaWorkingDirectory.isNotEmpty() ||
            patch.groovyWorkingDirectory.isNotEmpty() ||
            patch.pythonWorkingDirectory.isNotEmpty() ||
            patch.nodeWorkingDirectory.isNotEmpty()

    fun mergeInto(current: AppSettings, imported: AppSettings, retainSecrets: Boolean = false): AppSettings {
        val patch = sanitize(imported, retainSecrets)
        return current.copy(
            general = current.general.copy(
                language = SettingsGeneralNormalize.normalizeLanguage(patch.general.language),
                autoCheckUpdates = patch.general.autoCheckUpdates,
                autoDownloadUpdates = patch.general.autoDownloadUpdates,
                startMaximized = patch.general.startMaximized,
                closeBehavior = SettingsGeneralNormalize.normalizeCloseBehavior(patch.general.closeBehavior),
                trayEnabled = patch.general.trayEnabled,
                legacyMigrationHintDismissed = patch.general.legacyMigrationHintDismissed,
            ),
            appearance = current.appearance.copy(
                interfaceStyle = SettingsLayoutNormalize.normalizeInterfaceStyle(patch.appearance.interfaceStyle),
                theme = SettingsLayoutNormalize.normalizeTheme(patch.appearance.theme),
                accentColor = SettingsLayoutNormalize.normalizeAccentColor(patch.appearance.accentColor),
                fontFamily = SettingsLayoutNormalize.normalizeUiFontFamily(patch.appearance.fontFamily),
                fontSize = SettingsNumericBounds.clampNumber(patch.appearance.fontSize, 12, 18),
                unifiedBackground = patch.appearance.unifiedBackground
            ),
            layout = current.layout.copy(
                showRecent = patch.layout.showRecent,
                compactNavigation = patch.layout.compactNavigation,
                showSeparators = patch.layout.showSeparators,
                hideNavigationTitles = patch.layout.hideNavigationTitles,
                navigationStyle = SettingsLayoutNormalize.normalizeNavigationStyle(patch.layout.navigationStyle),
                customGroups = mergeCustomGroups(current.layout.customGroups, patch.layout.customGroups),
                hiddenNavigationToolIds = NavigationToolVisibility.normalizeHiddenNavigationToolIds(
                    patch.layout.hiddenNavigationToolIds
                ),
                paneSizes = ElectronPaneSizeImport.mergeElectronIntoCompose(
                    patch.layout.paneSizes,
                    current.layout.paneSizes
                )
            ),
            editor = current.editor.copy(
                sqlDialect = EditorFontSettings.normalizeSqlDialect(
                    patch.editor.sqlDialect,
                    current.editor.sqlDialect,
                ),
                jsonFontName = EditorFontSettings.normalizeFontName(
                    patch.editor.jsonFontName,
                    AppSettings.Default.editor.jsonFontName,
                ),
                jsonFontSize = SettingsNumericBounds.clampNumber(patch.editor.jsonFontSize, 11, 24),
                quickNoteFontName = EditorFontSettings.normalizeFontName(
                    patch.editor.quickNoteFontName,
                    AppSettings.Default.editor.quickNoteFontName,
                ),
                quickNoteFontSize = SettingsNumericBounds.clampNumber(patch.editor.quickNoteFontSize, 11, 24),
                softWrap = patch.editor.softWrap
            ),
            network = current.network.copy(
                proxyEnabled = patch.network.proxyEnabled,
                proxyHost = patch.network.proxyHost,
                proxyPort = patch.network.proxyPort,
                proxyUsername = patch.network.proxyUsername,
                proxyPassword = patch.network.proxyPassword,
                requestTimeoutMs = SettingsNumericBounds.clampNumber(patch.network.requestTimeoutMs, 1_000, 120_000),
                translationTimeoutMs = SettingsNumericBounds.clampNumber(patch.network.translationTimeoutMs, 1_000, 120_000),
            ),
            data = current.data.copy(directory = patch.data.directory),
            vault = current.vault.copy(
                quickNotePath = patch.vault.quickNotePath,
                jsonPath = patch.vault.jsonPath,
                gitRemote = patch.vault.gitRemote,
                gitUsername = patch.vault.gitUsername,
                gitToken = patch.vault.gitToken,
                autoCommit = patch.vault.autoCommit,
                autoCommitIdleSeconds = SettingsNumericBounds.clampNumber(patch.vault.autoCommitIdleSeconds, 5, 3_600),
                autoCommitInactiveSeconds = SettingsNumericBounds.clampNumber(patch.vault.autoCommitInactiveSeconds, 5, 3_600),
                autoPullMinutes = SettingsNumericBounds.clampNumber(patch.vault.autoPullMinutes, 0, 1_440),
                hideGitignoredFiles = patch.vault.hideGitignoredFiles,
                jsonTreeExpandMode = patch.vault.jsonTreeExpandMode,
                quickNoteTreeExpandMode = patch.vault.quickNoteTreeExpandMode
            ),
            runtime = current.runtime.copy(
                javaPath = patch.runtime.javaPath,
                groovyPath = patch.runtime.groovyPath,
                pythonPath = patch.runtime.pythonPath,
                nodePath = patch.runtime.nodePath
            ),
            tools = current.tools.copy(
                qrCodeSize = SettingsNumericBounds.clampNumber(patch.tools.qrCodeSize, 120, 2_000),
                qrErrorCorrection = SettingsNumericBounds.normalizeQrErrorCorrection(
                    patch.tools.qrErrorCorrection,
                    AppSettings.Default.tools.qrErrorCorrection,
                ),
                randomStringLength = SettingsNumericBounds.clampNumber(patch.tools.randomStringLength, 1, 4_096),
                exportDirectory = patch.tools.exportDirectory,
                translationProvider = patch.tools.translationProvider,
                translationSourceLang = patch.tools.translationSourceLang,
                translationTargetLang = patch.tools.translationTargetLang,
            ),
            shortcuts = current.shortcuts.copy(
                search = patch.shortcuts.search,
                settings = patch.shortcuts.settings
            )
        )
    }

    private fun sanitize(imported: AppSettings, retainSecrets: Boolean = false): AppSettings {
        val general = if (imported.schemaVersion < ELECTRON_LEGACY_MIGRATION_HINT_SCHEMA) {
            imported.general.copy(legacyMigrationHintDismissed = true)
        } else {
            imported.general
        }
        val base = SettingsLayoutNormalize.apply(
            imported.copy(
            general = general,
            editor = EditorFontSettings.normalizeEditorSettings(imported.editor, AppSettings.Default.editor),
            network = imported.network.copy(
                proxyPassword = if (retainSecrets) imported.network.proxyPassword else ""
            ),
            vault = imported.vault.copy(
                gitToken = if (retainSecrets) imported.vault.gitToken else "",
                quickNotePath = VaultPathConfig.effectiveCustomRoot(imported.vault.quickNotePath),
                jsonPath = VaultPathConfig.effectiveCustomRoot(imported.vault.jsonPath),
                jsonTreeExpandMode = normalizeVaultTreeExpandMode(imported.vault.jsonTreeExpandMode),
                quickNoteTreeExpandMode = normalizeVaultTreeExpandMode(imported.vault.quickNoteTreeExpandMode),
            ),
            tools = normalizeTranslationTools(imported.tools),
            ),
        )
        return SettingsNumericBounds.normalize(base, AppSettings.Default)
    }

    private fun normalizeTranslationTools(tools: ToolSettings): ToolSettings {
        val languages = TranslationEngine.normalizeLanguagePair(
            tools.translationSourceLang,
            tools.translationTargetLang,
        )
        return tools.copy(
            exportDirectory = VaultPathConfig.effectiveCustomRoot(tools.exportDirectory),
            translationSourceLang = languages.first,
            translationTargetLang = languages.second,
        )
    }

    private fun mergeCustomGroups(current: List<CustomToolGroup>, imported: List<CustomToolGroup>): List<CustomToolGroup> {
        if (imported.isEmpty()) return current
        val merged = current.toMutableList()
        imported.forEach { group ->
            if (merged.none { it.id == group.id }) merged += group
        }
        return merged
    }

    private fun parseCodeRunPatch(runtime: JsonObject): CodeRunSessionSnapshot {
        val drafts = runtime["drafts"]?.jsonObject
        val options = runtime["options"]?.jsonObject
        fun draft(key: String): String = drafts?.get(key)?.jsonPrimitive?.content?.trim() ?: ""
        fun option(key: String): Pair<String, String> {
            val node = options?.get(key)?.jsonObject
            val arguments = node?.get("arguments")?.jsonPrimitive?.content?.trim() ?: ""
            val workingDirectory = node?.get("workingDirectory")?.jsonPrimitive?.content?.trim() ?: ""
            return arguments to workingDirectory
        }
        val javaOpt = option("java")
        val groovyOpt = option("groovy")
        val pythonOpt = option("python")
        val nodeOpt = option("node")
        return CodeRunRuntimeOptionsNormalize.normalizeSnapshot(
            CodeRunSessionSnapshot(
                javaCode = draft("java"),
                groovyCode = draft("groovy"),
                pythonCode = draft("python"),
                nodeCode = draft("node"),
                javaArguments = javaOpt.first,
                groovyArguments = groovyOpt.first,
                pythonArguments = pythonOpt.first,
                nodeArguments = nodeOpt.first,
                javaWorkingDirectory = javaOpt.second,
                groovyWorkingDirectory = groovyOpt.second,
                pythonWorkingDirectory = pythonOpt.second,
                nodeWorkingDirectory = nodeOpt.second,
            ),
        )
    }
}
