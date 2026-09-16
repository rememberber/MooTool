package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.NavigationStyle
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.sessions.SessionManager
import com.rememberber.mootool.next.compose.storage.HostProfileStore
import com.rememberber.mootool.next.compose.storage.VaultPathConfig
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

typealias LegacyJavaConfig = Map<String, Map<String, String>>

object LegacyJavaSettings {
    const val WARNING_DIFFERENT_VAULT_REMOTES = "differentVaultRemotes"
    const val WARNING_SECRETS_SKIPPED = "secretsSkipped"

    fun findConfigFile(sourceRoot: Path): Path? {
        val preferred = sourceRoot.resolve("config/config.setting")
        if (preferred.isRegularFile()) return preferred
        return null
    }

    fun parse(raw: String): LegacyJavaConfig {
        val result = LinkedHashMap<String, LinkedHashMap<String, String>>()
        var group = ""
        raw.replace("\uFEFF", "").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith('#') || line.startsWith(';')) return@forEach
            val groupMatch = Regex("^\\[([^]]+)]$").matchEntire(line)
            if (groupMatch != null) {
                group = groupMatch.groupValues[1].trim()
                result.getOrPut(group) { LinkedHashMap() }
                return@forEach
            }
            val separator = line.indexOf('=')
            if (separator < 1) return@forEach
            val key = line.substring(0, separator).trim()
            val value = unquote(line.substring(separator + 1).trim())
            if (group.isEmpty()) return@forEach
            result.getOrPut(group) { LinkedHashMap() }[key] = value
        }
        return result
    }

    fun warningCodes(config: LegacyJavaConfig): List<String> {
        if (config.isEmpty()) return emptyList()
        val warnings = ArrayList<String>()
        val quickRemote = value(config, "func.quickNote", "quickNoteGitRemoteUrl")
        val jsonRemote = value(config, "func.jsonBeauty", "jsonBeautyGitRemoteUrl")
        if (quickRemote.isNotEmpty() && jsonRemote.isNotEmpty() && quickRemote != jsonRemote) {
            warnings += WARNING_DIFFERENT_VAULT_REMOTES
        }
        if (value(config, "setting.http", "httpProxyPassword").isNotEmpty() ||
            value(config, "func.vaultGit", "vaultGitToken").isNotEmpty()
        ) {
            warnings += WARNING_SECRETS_SKIPPED
        }
        return warnings
    }

    fun hasMigratableSettings(config: LegacyJavaConfig): Boolean {
        if (config.isEmpty()) return false
        return has(config, "setting.common", "locale") ||
            has(config, "setting.common", "autoCheckUpdate") ||
            has(config, "setting.common", "autoDownloadUpdate") ||
            has(config, "setting.normal", "defaultMaxWindow") ||
            has(config, "setting.normal", "themeColorFollowSystem") ||
            has(config, "setting.appearance", "theme") ||
            has(config, "setting.appearance", "font") ||
            has(config, "setting.appearance", "fontSize") ||
            has(config, "setting.normal", "unifiedBackground") ||
            has(config, "setting.custom", "tabCompact") ||
            has(config, "setting.custom", "tabHideTitle") ||
            has(config, "setting.custom", "tabSeparator") ||
            has(config, "setting.custom", "funcRecentVisible") ||
            has(config, "setting.custom", "tabCard") ||
            has(config, "setting.custom", "funcTabGrouped") ||
            has(config, "setting.quickNote", "sqlDialect") ||
            has(config, "setting.quickNote", "accentColor") ||
            has(config, "func.quickNote", "quickNoteFontSize") ||
            has(config, "func.jsonBeauty", "jsonBeautyFontSize") ||
            has(config, "func.quickNote", "quickNoteFontName") ||
            has(config, "func.jsonBeauty", "jsonBeautyFontName") ||
            has(config, "setting.http", "httpUseProxy") ||
            has(config, "setting.http", "httpProxyHost") ||
            has(config, "setting.http", "httpProxyPort") ||
            has(config, "setting.http", "httpProxyUserName") ||
            has(config, "setting.http", "httpTimeoutMs") ||
            has(config, "func.host", "hostExportPath") ||
            has(config, "func.quickNote", "quickNoteVaultPath") ||
            has(config, "func.jsonBeauty", "jsonBeautyVaultPath") ||
            has(config, "func.quickNote", "quickNoteGitRemoteUrl") ||
            has(config, "func.jsonBeauty", "jsonBeautyGitRemoteUrl") ||
            has(config, "func.vaultGit", "vaultGitUsername") ||
            has(config, "func.quickNote", "quickNoteAutoGitCommit") ||
            has(config, "func.jsonBeauty", "jsonBeautyAutoGitCommit") ||
            has(config, "func.quickNote", "quickNoteAutoGitIdleSeconds") ||
            has(config, "func.jsonBeauty", "jsonBeautyAutoGitIdleSeconds") ||
            has(config, "func.quickNote", "quickNoteAutoGitInactiveSeconds") ||
            has(config, "func.jsonBeauty", "jsonBeautyAutoGitInactiveSeconds") ||
            has(config, "func.quickNote", "quickNoteAutoPullIntervalMinutes") ||
            has(config, "func.jsonBeauty", "jsonBeautyAutoPullIntervalMinutes") ||
            has(config, "func.quickNote", "quickNoteHideGitignoredFiles") ||
            has(config, "func.jsonBeauty", "jsonBeautyHideGitignoredFiles") ||
            has(config, "func.qrCode", "qrCodeSize") ||
            has(config, "func.qrCode", "qrCodeErrorCorrectionLevel") ||
            has(config, "func.crypto", "randomStringDigit") ||
            has(config, "func.crypto", "randomNumDigit") ||
            has(config, "func.crypto", "randomPasswordDigit") ||
            has(config, "func.crypto", "digestFilePath") ||
            has(config, "func.quickNote", "quickNoteExportPath") ||
            has(config, "func.jsonBeauty", "jsonBeautyExportPath") ||
            has(config, "func.image", "imageExportPath") ||
            has(config, "func.translation", "translatorType") ||
            has(config, "func.translation", "sourceLanguage") ||
            has(config, "func.translation", "targetLanguage") ||
            has(config, "func.regex", "regexText") ||
            has(config, "func.calculator", "calculatorInputExpress") ||
            has(config, "func.host", "currentHostName") ||
            has(config, "func.qrCode", "qrCodeLogoPath") ||
            has(config, "func.qrCode", "qrCodeRecognitionImagePath") ||
            has(config, "func.qrCode", "qrCodeSaveAsPath") ||
            has(config, "func.quickNote", "quickNoteListSortMode") ||
            has(config, "func.quickNote", "quickNoteTreeExpandMode") ||
            has(config, "func.jsonBeauty", "jsonBeautyListSortMode") ||
            has(config, "func.jsonBeauty", "jsonBeautyTreeExpandMode") ||
            has(config, "func.colorBoard", "lastSelectedColor") ||
            has(config, "func.colorBoard", "colorTheme") ||
            has(config, "func.colorBoard", "colorCodeType")
    }

    /** Patches tool sessions from `config.setting` keys that are not stored in [AppSettings]. */
    fun applySessionPatches(
        sessionManager: SessionManager,
        config: LegacyJavaConfig,
        hostProfiles: HostProfileStore? = null,
    ): Int {
        if (config.isEmpty()) return 0
        var applied = 0
        if (has(config, "func.regex", "regexText")) {
            val pattern = value(config, "func.regex", "regexText")
            if (pattern.isNotEmpty()) {
                sessionManager.regexSession().pattern = pattern
                sessionManager.persistRegex()
                applied++
            }
        }
        if (has(config, "func.calculator", "calculatorInputExpress")) {
            val expression = value(config, "func.calculator", "calculatorInputExpress")
            if (expression.isNotEmpty()) {
                sessionManager.calculatorSession().expression = expression
                sessionManager.persistCalculator()
                applied++
            }
        }
        if (has(config, "func.quickNote", "quickNoteListSortMode")) {
            sessionManager.quickNoteSession().vaultSort = VaultSort.normalize(
                legacyVaultListSort(value(config, "func.quickNote", "quickNoteListSortMode")),
                allowCreated = true,
            )
            sessionManager.persistQuickNote()
            applied++
        }
        if (has(config, "func.jsonBeauty", "jsonBeautyListSortMode")) {
            sessionManager.jsonSession().vaultSort = VaultSort.normalize(
                legacyVaultListSort(value(config, "func.jsonBeauty", "jsonBeautyListSortMode")),
                allowCreated = false,
            )
            sessionManager.persistJson()
            applied++
        }
        if (has(config, "func.host", "currentHostName") && hostProfiles != null) {
            val profileName = value(config, "func.host", "currentHostName")
            if (profileName.isNotEmpty()) {
                val summary = hostProfiles.list(includeContent = false)
                    .firstOrNull { it.name == profileName }
                val profile = summary?.let { hostProfiles.get(it.id) }
                if (profile != null) {
                    val session = sessionManager.hostSession()
                    session.selectedId = profile.id
                    session.name = profile.name
                    session.content = profile.content
                    session.savedName = profile.name
                    session.savedContent = profile.content
                    sessionManager.persistHost()
                    applied++
                }
            }
        }
        val qrSession = sessionManager.qrSession()
        var qrChanged = false
        if (has(config, "func.qrCode", "qrCodeLogoPath")) {
            val pathText = value(config, "func.qrCode", "qrCodeLogoPath")
            val path = Path.of(pathText)
            if (pathText.isNotEmpty() && path.isRegularFile()) {
                qrSession.logoPath = pathText
                qrSession.logoName = path.fileName.toString()
                qrSession.logoImage = runCatching { QrEngine.readImageFile(path) }.getOrNull()
                qrChanged = true
                applied++
            }
        }
        if (has(config, "func.qrCode", "qrCodeRecognitionImagePath")) {
            val pathText = value(config, "func.qrCode", "qrCodeRecognitionImagePath")
            val path = Path.of(pathText)
            if (pathText.isNotEmpty() && path.isRegularFile()) {
                qrSession.recognitionName = path.fileName.toString()
                qrSession.recognitionBytes = runCatching { path.readBytes() }.getOrNull()
                qrChanged = true
                applied++
            }
        }
        if (qrChanged) {
            sessionManager.persistQr()
        }
        val randomLengths = listOfNotNull(
            legacyCryptoRandomLength(config, "randomStringDigit"),
            legacyCryptoRandomLength(config, "randomNumDigit"),
            legacyCryptoRandomLength(config, "randomPasswordDigit"),
        )
        if (randomLengths.isNotEmpty()) {
            val crypto = sessionManager.cryptoSession()
            crypto.randomLength = randomLengths.max().coerceIn(
                CryptoEngine.MIN_RANDOM_LENGTH,
                CryptoEngine.MAX_RANDOM_LENGTH,
            )
            sessionManager.persistCrypto()
            applied++
        }
        if (has(config, "func.crypto", "digestFilePath")) {
            val pathText = value(config, "func.crypto", "digestFilePath")
            val path = Path.of(pathText)
            if (pathText.isNotEmpty() && path.isRegularFile()) {
                val crypto = sessionManager.cryptoSession()
                crypto.digestFileName = path.fileName.toString()
                runCatching {
                    crypto.digestOutput = CryptoEngine.digestFile(crypto.digestAlgorithm, path)
                }
                sessionManager.persistCrypto()
                applied++
            }
        }
        if (has(config, "setting.http", "httpTimeoutMs")) {
            val timeout = numberValue(value(config, "setting.http", "httpTimeoutMs"), 0)
            if (timeout > 0) {
                val session = sessionManager.httpSession()
                session.timeoutMs = HttpEngine.clampTimeout(timeout)
                sessionManager.persistHttp()
                applied++
            }
        }
        if (has(config, "func.colorBoard", "lastSelectedColor") ||
            has(config, "func.colorBoard", "colorTheme") ||
            has(config, "func.colorBoard", "colorCodeType")
        ) {
            val color = sessionManager.colorSession()
            var colorChanged = false
            if (has(config, "func.colorBoard", "colorCodeType")) {
                color.format = legacyColorFormat(value(config, "func.colorBoard", "colorCodeType"))
                colorChanged = true
            }
            if (has(config, "func.colorBoard", "colorTheme")) {
                color.theme = legacyColorThemeId(value(config, "func.colorBoard", "colorTheme"))
                colorChanged = true
            }
            if (has(config, "func.colorBoard", "lastSelectedColor")) {
                val raw = value(config, "func.colorBoard", "lastSelectedColor")
                val parsed = runCatching {
                    val input = if (raw.startsWith("#")) raw else "#$raw"
                    ColorEngine.parseColor(input)
                }.getOrNull()
                if (parsed != null) {
                    color.primary = parsed
                    colorChanged = true
                }
            }
            if (colorChanged) {
                color.code = ColorEngine.formatColor(color.primary, color.format)
                sessionManager.persistColor()
                applied++
            }
        }
        return applied
    }

    /**
     * When Java used relative vault paths, imported files land in this product's vault roots.
     * Point settings at those absolute paths so Git/监视与设置页与真实目录一致。
     */
    fun applyVaultRootsAfterRelativeJavaConfig(
        current: AppSettings,
        config: LegacyJavaConfig,
        quickNoteRoot: Path,
        jsonRoot: Path,
        importedNotes: Int,
        importedJson: Int,
    ): AppSettings {
        if (config.isEmpty()) return current
        var vault = current.vault
        if (importedNotes > 0 && has(config, "func.quickNote", "quickNoteVaultPath")) {
            val raw = value(config, "func.quickNote", "quickNoteVaultPath")
            if (legacyAbsoluteVaultPath(raw) == null) {
                vault = vault.copy(quickNotePath = quickNoteRoot.toAbsolutePath().normalize().toString())
            }
        }
        if (importedJson > 0 && has(config, "func.jsonBeauty", "jsonBeautyVaultPath")) {
            val raw = value(config, "func.jsonBeauty", "jsonBeautyVaultPath")
            if (legacyAbsoluteVaultPath(raw) == null) {
                vault = vault.copy(jsonPath = jsonRoot.toAbsolutePath().normalize().toString())
            }
        }
        return current.copy(vault = vault)
    }

    fun applyPatch(current: AppSettings, config: LegacyJavaConfig): AppSettings {
        if (config.isEmpty()) return current
        var next = current
        val followSystem = booleanValue(value(config, "setting.normal", "themeColorFollowSystem"), true)
        val themeName = value(config, "setting.appearance", "theme").lowercase()
        val quickRemote = value(config, "func.quickNote", "quickNoteGitRemoteUrl")
        val jsonRemote = value(config, "func.jsonBeauty", "jsonBeautyGitRemoteUrl")
        val remotesConflict = quickRemote.isNotEmpty() && jsonRemote.isNotEmpty() && quickRemote != jsonRemote

        if (has(config, "setting.common", "locale")) {
            next = next.copy(general = next.general.copy(language = legacyLanguage(value(config, "setting.common", "locale"))))
        }
        if (has(config, "setting.common", "autoCheckUpdate")) {
            next = next.copy(
                general = next.general.copy(
                    autoCheckUpdates = booleanValue(value(config, "setting.common", "autoCheckUpdate"), true)
                )
            )
        }
        if (has(config, "setting.common", "autoDownloadUpdate")) {
            next = next.copy(
                general = next.general.copy(
                    autoDownloadUpdates = booleanValue(value(config, "setting.common", "autoDownloadUpdate"), false),
                ),
            )
        }
        if (has(config, "setting.normal", "defaultMaxWindow")) {
            next = next.copy(
                general = next.general.copy(
                    startMaximized = booleanValue(value(config, "setting.normal", "defaultMaxWindow"), false)
                )
            )
        }

        if (has(config, "setting.normal", "themeColorFollowSystem") || has(config, "setting.appearance", "theme")) {
            val theme = if (followSystem) {
                ThemePreference.System.name.lowercase()
            } else if (themeName.contains("dark") || themeName.contains("darcula")) {
                ThemePreference.Dark.name.lowercase()
            } else {
                ThemePreference.Light.name.lowercase()
            }
            next = next.copy(appearance = next.appearance.copy(theme = theme))
        }
        if (has(config, "setting.appearance", "font")) {
            next = next.copy(appearance = next.appearance.copy(fontFamily = value(config, "setting.appearance", "font")))
        }
        if (has(config, "setting.appearance", "fontSize")) {
            next = next.copy(
                appearance = next.appearance.copy(fontSize = positiveNumber(value(config, "setting.appearance", "fontSize"), 13))
            )
        }
        if (has(config, "setting.normal", "unifiedBackground")) {
            next = next.copy(
                appearance = next.appearance.copy(
                    unifiedBackground = booleanValue(value(config, "setting.normal", "unifiedBackground"), true)
                )
            )
        }
        if (has(config, "setting.quickNote", "accentColor")) {
            next = next.copy(
                appearance = next.appearance.copy(
                    accentColor = legacyAccentColor(value(config, "setting.quickNote", "accentColor")),
                ),
            )
        }

        if (has(config, "setting.custom", "tabCompact")) {
            next = next.copy(
                layout = next.layout.copy(compactNavigation = booleanValue(value(config, "setting.custom", "tabCompact"), false))
            )
        }
        if (has(config, "setting.custom", "tabHideTitle")) {
            next = next.copy(
                layout = next.layout.copy(hideNavigationTitles = booleanValue(value(config, "setting.custom", "tabHideTitle"), false))
            )
        }
        if (has(config, "setting.custom", "tabSeparator")) {
            next = next.copy(
                layout = next.layout.copy(showSeparators = booleanValue(value(config, "setting.custom", "tabSeparator"), false))
            )
        }
        if (has(config, "setting.custom", "funcRecentVisible")) {
            next = next.copy(
                layout = next.layout.copy(showRecent = booleanValue(value(config, "setting.custom", "funcRecentVisible"), false))
            )
        }
        legacyNavigationStyle(config)?.let { style ->
            next = next.copy(layout = next.layout.copy(navigationStyle = style))
        }

        var editor = next.editor
        if (has(config, "setting.quickNote", "sqlDialect")) {
            editor = editor.copy(sqlDialect = value(config, "setting.quickNote", "sqlDialect"))
        }
        val quickNoteFontSize = numberValue(value(config, "func.quickNote", "quickNoteFontSize"), 0)
        val jsonFontSize = numberValue(value(config, "func.jsonBeauty", "jsonBeautyFontSize"), 0)
        if (quickNoteFontSize > 0) editor = editor.copy(quickNoteFontSize = quickNoteFontSize)
        if (jsonFontSize > 0) editor = editor.copy(jsonFontSize = jsonFontSize)
        val quickNoteFontName = value(config, "func.quickNote", "quickNoteFontName")
        val jsonFontName = value(config, "func.jsonBeauty", "jsonBeautyFontName")
        if (quickNoteFontName.isNotEmpty()) editor = editor.copy(quickNoteFontName = quickNoteFontName)
        if (jsonFontName.isNotEmpty()) editor = editor.copy(jsonFontName = jsonFontName)
        next = next.copy(editor = editor)

        var network = next.network
        if (has(config, "setting.http", "httpUseProxy")) {
            network = network.copy(proxyEnabled = booleanValue(value(config, "setting.http", "httpUseProxy"), false))
        }
        if (has(config, "setting.http", "httpProxyHost")) {
            network = network.copy(proxyHost = value(config, "setting.http", "httpProxyHost"))
        }
        if (has(config, "setting.http", "httpProxyPort")) {
            network = network.copy(proxyPort = value(config, "setting.http", "httpProxyPort"))
        }
        if (has(config, "setting.http", "httpProxyUserName")) {
            network = network.copy(proxyUsername = value(config, "setting.http", "httpProxyUserName"))
        }
        if (has(config, "setting.http", "httpTimeoutMs")) {
            val timeout = numberValue(value(config, "setting.http", "httpTimeoutMs"), 0)
            if (timeout > 0) {
                network = network.copy(requestTimeoutMs = timeout.coerceIn(1_000, 120_000))
            }
        }
        next = next.copy(network = network)

        var vault = next.vault
        if (!remotesConflict && (quickRemote.isNotEmpty() || jsonRemote.isNotEmpty())) {
            vault = vault.copy(gitRemote = quickRemote.ifEmpty { jsonRemote })
        }
        if (has(config, "func.vaultGit", "vaultGitUsername")) {
            vault = vault.copy(gitUsername = value(config, "func.vaultGit", "vaultGitUsername"))
        }
        if (has(config, "func.quickNote", "quickNoteAutoGitCommit") || has(config, "func.jsonBeauty", "jsonBeautyAutoGitCommit")) {
            vault = vault.copy(
                autoCommit = booleanValue(value(config, "func.quickNote", "quickNoteAutoGitCommit"), false) ||
                    booleanValue(value(config, "func.jsonBeauty", "jsonBeautyAutoGitCommit"), false)
            )
        }
        val idleSeconds = listOf(
            numberValue(value(config, "func.quickNote", "quickNoteAutoGitIdleSeconds"), 0),
            numberValue(value(config, "func.jsonBeauty", "jsonBeautyAutoGitIdleSeconds"), 0)
        ).filter { it > 0 }
        if (idleSeconds.isNotEmpty()) vault = vault.copy(autoCommitIdleSeconds = idleSeconds.min())
        val inactiveSeconds = listOf(
            numberValue(value(config, "func.quickNote", "quickNoteAutoGitInactiveSeconds"), 0),
            numberValue(value(config, "func.jsonBeauty", "jsonBeautyAutoGitInactiveSeconds"), 0)
        ).filter { it > 0 }
        if (inactiveSeconds.isNotEmpty()) vault = vault.copy(autoCommitInactiveSeconds = inactiveSeconds.min())
        if (has(config, "func.quickNote", "quickNoteAutoPullIntervalMinutes") ||
            has(config, "func.jsonBeauty", "jsonBeautyAutoPullIntervalMinutes")
        ) {
            val pullIntervals = listOf(
                numberValue(value(config, "func.quickNote", "quickNoteAutoPullIntervalMinutes"), 0),
                numberValue(value(config, "func.jsonBeauty", "jsonBeautyAutoPullIntervalMinutes"), 0)
            ).filter { it > 0 }
            vault = vault.copy(autoPullMinutes = pullIntervals.minOrNull() ?: 0)
        }
        if (has(config, "func.quickNote", "quickNoteHideGitignoredFiles") ||
            has(config, "func.jsonBeauty", "jsonBeautyHideGitignoredFiles")
        ) {
            vault = vault.copy(
                hideGitignoredFiles = booleanValue(value(config, "func.quickNote", "quickNoteHideGitignoredFiles"), true) &&
                    booleanValue(value(config, "func.jsonBeauty", "jsonBeautyHideGitignoredFiles"), true)
            )
        }
        if (has(config, "func.quickNote", "quickNoteTreeExpandMode")) {
            vault = vault.copy(
                quickNoteTreeExpandMode = legacyVaultTreeExpandMode(
                    value(config, "func.quickNote", "quickNoteTreeExpandMode"),
                ),
            )
        }
        if (has(config, "func.jsonBeauty", "jsonBeautyTreeExpandMode")) {
            vault = vault.copy(
                jsonTreeExpandMode = legacyVaultTreeExpandMode(
                    value(config, "func.jsonBeauty", "jsonBeautyTreeExpandMode"),
                ),
            )
        }
        if (has(config, "func.quickNote", "quickNoteVaultPath")) {
            legacyAbsoluteVaultPath(value(config, "func.quickNote", "quickNoteVaultPath"))?.let {
                vault = vault.copy(quickNotePath = it)
            }
        }
        if (has(config, "func.jsonBeauty", "jsonBeautyVaultPath")) {
            legacyAbsoluteVaultPath(value(config, "func.jsonBeauty", "jsonBeautyVaultPath"))?.let {
                vault = vault.copy(jsonPath = it)
            }
        }
        next = next.copy(vault = vault)

        var tools = next.tools
        if (has(config, "func.qrCode", "qrCodeSize")) {
            tools = tools.copy(qrCodeSize = positiveNumber(value(config, "func.qrCode", "qrCodeSize"), 300))
        }
        if (has(config, "func.qrCode", "qrCodeErrorCorrectionLevel")) {
            tools = tools.copy(qrErrorCorrection = legacyQrCorrection(value(config, "func.qrCode", "qrCodeErrorCorrectionLevel")))
        }
        if (has(config, "func.crypto", "randomStringDigit")) {
            tools = tools.copy(randomStringLength = positiveNumber(value(config, "func.crypto", "randomStringDigit"), 16))
        }
        val exportDirectory = value(config, "func.quickNote", "quickNoteExportPath")
            .ifEmpty { value(config, "func.jsonBeauty", "jsonBeautyExportPath") }
            .ifEmpty { value(config, "func.image", "imageExportPath") }
            .ifEmpty { value(config, "func.host", "hostExportPath") }
            .ifEmpty { value(config, "func.qrCode", "qrCodeSaveAsPath") }
        if (exportDirectory.isNotEmpty() && Path.of(exportDirectory).isAbsolute()) {
            tools = tools.copy(exportDirectory = exportDirectory)
        }
        if (has(config, "func.translation", "translatorType")) {
            val provider = value(config, "func.translation", "translatorType").uppercase()
            tools = tools.copy(translationProvider = if (provider == "BING") "bing" else "google")
        }
        if (has(config, "func.translation", "sourceLanguage")) {
            tools = tools.copy(translationSourceLang = value(config, "func.translation", "sourceLanguage"))
        }
        if (has(config, "func.translation", "targetLanguage")) {
            tools = tools.copy(translationTargetLang = value(config, "func.translation", "targetLanguage"))
        }
        return next.copy(tools = tools)
    }

    private fun value(config: LegacyJavaConfig, group: String, key: String): String =
        config[group]?.get(key)?.trim().orEmpty()

    private fun has(config: LegacyJavaConfig, group: String, key: String): Boolean =
        config[group]?.containsKey(key) == true

    private fun legacyAbsoluteVaultPath(raw: String): String? {
        if (raw.isBlank()) return null
        val path = expandHomePath(raw).normalize()
        if (!path.isAbsolute) return null
        return VaultPathConfig.normalizedCustomRoot(path.toString())
    }

    private fun expandHomePath(value: String): Path {
        val trimmed = value.trim()
        if (trimmed == "~") return Path.of(System.getProperty("user.home"))
        if (trimmed.startsWith("~/") || trimmed.startsWith("~\\")) {
            return Path.of(System.getProperty("user.home"), trimmed.substring(2))
        }
        return Path.of(trimmed)
    }

    private fun legacyNavigationStyle(config: LegacyJavaConfig): String? {
        val groupedPresent = has(config, "setting.custom", "funcTabGrouped")
        val cardPresent = has(config, "setting.custom", "tabCard")
        if (!groupedPresent && !cardPresent) return null
        val grouped = groupedPresent && booleanValue(value(config, "setting.custom", "funcTabGrouped"), true)
        val card = cardPresent && booleanValue(value(config, "setting.custom", "tabCard"), false)
        return when {
            grouped -> NavigationStyle.Grouped.name.lowercase()
            card -> NavigationStyle.Card.name.lowercase()
            else -> NavigationStyle.Classic.name.lowercase()
        }
    }

    private fun legacyAccentColor(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "blue"
        val compact = trimmed.lowercase().replace(".", "")
        if (trimmed.startsWith("Moo.accent.")) {
            return when {
                "yellow" in compact || "orange" in compact -> "yellow"
                "red" in compact -> "red"
                "purple" in compact -> "purple"
                "green" in compact -> "green"
                "blue" in compact || compact.endsWith("default") -> "blue"
                else -> "blue"
            }
        }
        return when (trimmed.lowercase()) {
            "yellow", "coral", "blue", "green", "red", "purple" -> trimmed.lowercase()
            "orange" -> "yellow"
            "teal" -> "green"
            else -> "blue"
        }
    }

    private fun legacyVaultListSort(value: String): String = when (value.trim().uppercase()) {
        "MODIFIED_TIME" -> VaultSort.MODIFIED
        "CREATE_TIME" -> VaultSort.CREATED
        "NAME" -> VaultSort.NAME
        else -> VaultSort.MODIFIED
    }

    private fun legacyVaultTreeExpandMode(value: String): String = when (value.trim().uppercase()) {
        "COLLAPSE_ALL" -> "collapseAll"
        "EXPAND_ALL" -> "expandAll"
        else -> "expandAll"
    }

    private fun legacyColorThemeId(value: String): ColorThemeId {
        val trimmed = value.trim()
        return when (trimmed) {
            "默认", "Default" -> ColorThemeId.Default
            "主题1", "Theme 1", "Theme1", "theme1" -> ColorThemeId.Theme1
            "主题2", "Theme 2", "Theme2", "theme2" -> ColorThemeId.Theme2
            "主题3", "Theme 3", "Theme3", "theme3" -> ColorThemeId.Theme3
            "主题4", "Theme 4", "Theme4", "theme4" -> ColorThemeId.Theme4
            "主题5", "Theme 5", "Theme5", "theme5" -> ColorThemeId.Theme5
            "中国色", "Chinese Colors", "china", "China" -> ColorThemeId.China
            else -> trimmed.toIntOrNull()?.let { index ->
                ColorThemeId.entries.getOrNull(index)
            } ?: ColorEngine.themeId(trimmed)
        }
    }

    private fun legacyColorFormat(value: String): ColorFormat = when (value.trim()) {
        "HTML" -> ColorFormat.HEX_UPPER
        "html" -> ColorFormat.HEX_LOWER
        "RGB" -> ColorFormat.RGB
        else -> ColorFormat.HEX_UPPER
    }

    private fun legacyCryptoRandomLength(config: LegacyJavaConfig, key: String): Int? {
        if (!has(config, "func.crypto", key)) return null
        return positiveNumber(value(config, "func.crypto", key), 16)
            .coerceIn(CryptoEngine.MIN_RANDOM_LENGTH, CryptoEngine.MAX_RANDOM_LENGTH)
    }

    private fun legacyLanguage(value: String): String {
        val normalized = value.replace('_', '-').lowercase()
        return when {
            normalized.startsWith("ja") -> AppLanguage.JaJP.code
            normalized.startsWith("zh") -> AppLanguage.ZhCN.code
            else -> AppLanguage.EnUS.code
        }
    }

    private fun legacyQrCorrection(value: String): String {
        val normalized = value.trim().uppercase()
        return when {
            normalized == "Q" || value.contains("中高") -> "Q"
            normalized == "H" || value.contains("高") -> "H"
            normalized == "L" || value == "低" -> "L"
            else -> "M"
        }
    }

    private fun booleanValue(value: String, fallback: Boolean): Boolean {
        val normalized = value.trim().lowercase()
        return when (normalized) {
            "true", "1", "yes", "on" -> true
            "false", "0", "no", "off" -> false
            else -> fallback
        }
    }

    private fun numberValue(value: String, fallback: Int): Int =
        value.trim().toDoubleOrNull()?.toInt() ?: fallback

    private fun positiveNumber(value: String, fallback: Int): Int {
        val number = numberValue(value, fallback)
        return if (number > 0) number else fallback
    }

    private fun unquote(value: String): String {
        if (value.length >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length - 1)
        }
        return value
    }
}
