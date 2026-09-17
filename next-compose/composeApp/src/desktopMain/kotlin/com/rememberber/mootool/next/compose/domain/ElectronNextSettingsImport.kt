package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.domain.NavigationToolVisibility
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ToolSettings
import com.rememberber.mootool.next.compose.model.CloseBehavior
import com.rememberber.mootool.next.compose.model.CustomToolGroup
import com.rememberber.mootool.next.compose.model.InterfaceStyle
import com.rememberber.mootool.next.compose.model.NavigationStyle
import com.rememberber.mootool.next.compose.model.ThemePreference
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
        val limit = CodeRunEngine.MAX_CODE_BYTES
        fun pick(patchValue: String, currentValue: String): String =
            patchValue.takeIf { it.isNotEmpty() } ?: currentValue
        return current.copy(
            javaCode = pick(patch.javaCode.take(limit), current.javaCode),
            groovyCode = pick(patch.groovyCode.take(limit), current.groovyCode),
            pythonCode = pick(patch.pythonCode.take(limit), current.pythonCode),
            nodeCode = pick(patch.nodeCode.take(limit), current.nodeCode),
            javaArguments = pick(patch.javaArguments, current.javaArguments),
            groovyArguments = pick(patch.groovyArguments, current.groovyArguments),
            pythonArguments = pick(patch.pythonArguments, current.pythonArguments),
            nodeArguments = pick(patch.nodeArguments, current.nodeArguments),
            javaWorkingDirectory = pick(patch.javaWorkingDirectory, current.javaWorkingDirectory),
            groovyWorkingDirectory = pick(patch.groovyWorkingDirectory, current.groovyWorkingDirectory),
            pythonWorkingDirectory = pick(patch.pythonWorkingDirectory, current.pythonWorkingDirectory),
            nodeWorkingDirectory = pick(patch.nodeWorkingDirectory, current.nodeWorkingDirectory)
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
                language = normalizeLanguage(patch.general.language),
                autoCheckUpdates = patch.general.autoCheckUpdates,
                autoDownloadUpdates = patch.general.autoDownloadUpdates,
                startMaximized = patch.general.startMaximized,
                closeBehavior = normalizeCloseBehavior(patch.general.closeBehavior),
                trayEnabled = patch.general.trayEnabled,
                legacyMigrationHintDismissed = patch.general.legacyMigrationHintDismissed,
            ),
            appearance = current.appearance.copy(
                interfaceStyle = normalizeInterfaceStyle(patch.appearance.interfaceStyle),
                theme = normalizeTheme(patch.appearance.theme),
                accentColor = patch.appearance.accentColor,
                fontFamily = patch.appearance.fontFamily,
                fontSize = patch.appearance.fontSize.coerceIn(12, 18),
                unifiedBackground = patch.appearance.unifiedBackground
            ),
            layout = current.layout.copy(
                showRecent = patch.layout.showRecent,
                compactNavigation = patch.layout.compactNavigation,
                showSeparators = patch.layout.showSeparators,
                hideNavigationTitles = patch.layout.hideNavigationTitles,
                navigationStyle = normalizeNavigationStyle(patch.layout.navigationStyle),
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
                sqlDialect = patch.editor.sqlDialect,
                jsonFontName = patch.editor.jsonFontName,
                jsonFontSize = patch.editor.jsonFontSize.coerceIn(11, 24),
                quickNoteFontName = patch.editor.quickNoteFontName,
                quickNoteFontSize = patch.editor.quickNoteFontSize.coerceIn(11, 24),
                softWrap = patch.editor.softWrap
            ),
            network = current.network.copy(
                proxyEnabled = patch.network.proxyEnabled,
                proxyHost = patch.network.proxyHost,
                proxyPort = patch.network.proxyPort,
                proxyUsername = patch.network.proxyUsername,
                proxyPassword = patch.network.proxyPassword,
                requestTimeoutMs = patch.network.requestTimeoutMs.coerceIn(1_000, 120_000),
                translationTimeoutMs = patch.network.translationTimeoutMs.coerceIn(1_000, 60_000)
            ),
            data = current.data.copy(directory = patch.data.directory),
            vault = current.vault.copy(
                quickNotePath = patch.vault.quickNotePath,
                jsonPath = patch.vault.jsonPath,
                gitRemote = patch.vault.gitRemote,
                gitUsername = patch.vault.gitUsername,
                gitToken = patch.vault.gitToken,
                autoCommit = patch.vault.autoCommit,
                autoCommitIdleSeconds = patch.vault.autoCommitIdleSeconds.coerceAtLeast(5),
                autoCommitInactiveSeconds = patch.vault.autoCommitInactiveSeconds.coerceAtLeast(30),
                autoPullMinutes = patch.vault.autoPullMinutes.coerceAtLeast(0),
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
                qrCodeSize = patch.tools.qrCodeSize.coerceIn(120, 2_000),
                qrErrorCorrection = patch.tools.qrErrorCorrection,
                randomStringLength = patch.tools.randomStringLength.coerceIn(4, 256),
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
        val groups = imported.layout.customGroups.mapNotNull { group ->
            val name = group.name.trim()
            val ids = group.toolIds.filter { ToolId.fromId(it) != null }
            if (name.isEmpty() || ids.isEmpty()) null
            else group.copy(name = name, toolIds = ids)
        }
        val general = if (imported.schemaVersion < ELECTRON_LEGACY_MIGRATION_HINT_SCHEMA) {
            imported.general.copy(legacyMigrationHintDismissed = true)
        } else {
            imported.general
        }
        return imported.copy(
            general = general,
            layout = imported.layout.copy(customGroups = groups),
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
        )
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

    private fun normalizeLanguage(value: String): String =
        AppLanguage.entries.firstOrNull { it.code.equals(value, ignoreCase = true) }?.code ?: AppLanguage.ZhCN.code

    private fun normalizeTheme(value: String): String =
        ThemePreference.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.name?.lowercase()
            ?: ThemePreference.System.name.lowercase()

    private fun normalizeInterfaceStyle(value: String): String {
        val normalized = value.trim().lowercase()
        return when (normalized) {
            "miui-v5", "miuiv5" -> "miui-v5"
            "modern" -> InterfaceStyle.Modern.name.lowercase()
            "quiet" -> InterfaceStyle.Quiet.name.lowercase()
            "hero" -> InterfaceStyle.Hero.name.lowercase()
            "smartisan" -> InterfaceStyle.Smartisan.name.lowercase()
            "claude" -> InterfaceStyle.Claude.name.lowercase()
            else -> InterfaceStyle.Modern.name.lowercase()
        }
    }

    private fun normalizeCloseBehavior(value: String): String =
        CloseBehavior.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.name?.lowercase()
            ?: CloseBehavior.Ask.name.lowercase()

    private fun normalizeNavigationStyle(value: String): String =
        NavigationStyle.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.name?.lowercase()
            ?: NavigationStyle.Classic.name.lowercase()

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
        return CodeRunSessionSnapshot(
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
            nodeWorkingDirectory = nodeOpt.second
        )
    }
}
