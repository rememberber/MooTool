package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.storage.BackupInfo
import com.rememberber.mootool.next.compose.storage.BackupOpenLocation
import com.rememberber.mootool.next.compose.app.InstallIdentity
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.domain.GitRemoteCommitResult
import com.rememberber.mootool.next.compose.domain.NavigationToolVisibility
import com.rememberber.mootool.next.compose.domain.ProxyPortCommitResult
import com.rememberber.mootool.next.compose.domain.NumericSettingCommitResult
import com.rememberber.mootool.next.compose.domain.SettingsEditorNumericNormalize
import com.rememberber.mootool.next.compose.domain.SettingsNetworkNormalize
import com.rememberber.mootool.next.compose.domain.SettingsToolsNumericNormalize
import com.rememberber.mootool.next.compose.domain.SettingsToolsTranslationNormalize
import com.rememberber.mootool.next.compose.domain.SettingsVaultNumericNormalize
import com.rememberber.mootool.next.compose.domain.SettingsVaultGitNormalize
import com.rememberber.mootool.next.compose.domain.TimeoutCommitResult
import com.rememberber.mootool.next.compose.domain.VaultNumericCommitResult
import com.rememberber.mootool.next.compose.app.UpdateUiState
import com.rememberber.mootool.next.compose.domain.UpdateAboutPresentation
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.CloseBehavior
import com.rememberber.mootool.next.compose.model.InterfaceStyle
import com.rememberber.mootool.next.compose.model.NavigationStyle
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.domain.ShortcutBindings
import com.rememberber.mootool.next.compose.ui.components.AccentSwatches
import com.rememberber.mootool.next.compose.ui.components.FontSelect
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooKbd
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooSettingsNavHeader
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.MooSegmented
import com.rememberber.mootool.next.compose.ui.components.MooSelect
import com.rememberber.mootool.next.compose.ui.components.MooSwitch
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.icons.ToolIcon
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.SettingRow
import com.rememberber.mootool.next.compose.storage.VaultPathConfig
import com.rememberber.mootool.next.compose.ui.components.SettingCommitTextField
import com.rememberber.mootool.next.compose.ui.components.SettingTextField
import com.rememberber.mootool.next.compose.ui.components.SettingsGroup
import com.rememberber.mootool.next.compose.ui.components.SettingsNavItem
import com.rememberber.mootool.next.compose.ui.components.mooSidebarBackground
import com.rememberber.mootool.next.compose.ui.components.mooWorkspaceBackground
import com.rememberber.mootool.next.compose.model.AppSettings

private const val SETTINGS_PANE_KEY = "settings-page"

@Composable
fun SettingsScreen(container: AppContainer) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val showSettings by container.showSettings.collectAsState()
    var category by remember {
        mutableStateOf(SettingsNavPresentation.fromStorageId(container.settingsNavCategoryId))
    }
    LaunchedEffect(showSettings, container.settingsNavCategoryId) {
        if (showSettings) {
            category = SettingsNavPresentation.fromStorageId(container.settingsNavCategoryId)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().mooWorkspaceBackground()) {
        val minNav = 180f
        val minContent = 420f
        val paneHandle = 10f
        val maxNav = (maxWidth.value - paneHandle - minContent).coerceAtLeast(minNav)
        val defaultNav = (maxWidth.value * (220f / 1000f)).coerceIn(minNav, maxNav)
        val navWidth = settings.layout.pane(SETTINGS_PANE_KEY, 0, defaultNav, minNav, maxNav)
        Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(navWidth.dp).widthIn(min = 180.dp).fillMaxHeight().mooSidebarBackground().padding(vertical = 12.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            val next = SettingsNavPresentation.step(category, 1)
                            category = next
                            container.settingsNavCategoryId = next.storageId()
                            true
                        }
                        Key.DirectionUp -> {
                            val next = SettingsNavPresentation.step(category, -1)
                            category = next
                            container.settingsNavCategoryId = next.storageId()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            SettingsNavCategory.entries.forEach { item ->
                SettingsNavItem(
                    label = container.t(item.categoryLabelKey()),
                    icon = item.navIcon(),
                    selected = item == category,
                    onClick = {
                        category = item
                        container.settingsNavCategoryId = item.storageId()
                    }
                )
            }
        }
        VerticalPaneHandle(
            onDelta = { container.setPaneSize(SETTINGS_PANE_KEY, 0, navWidth + it, 1) },
            onReset = { container.setPaneSize(SETTINGS_PANE_KEY, 0, defaultNav, 1) }
        )
        Column(Modifier.weight(1f).widthIn(min = 420.dp).fillMaxHeight()) {
            Row(
                modifier = Modifier.mooSettingsNavHeader(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    SettingsNavPresentation.contentHeaderIcon(category),
                    color = colors.textSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                MooPageTitle(container.t(SettingsNavPresentation.contentHeaderLabelKey(category)), settings = true)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (category) {
                SettingsNavCategory.General -> SettingsGroup(container.t("settings.group.application")) {
                    SettingRow(container.t("settings.language")) {
                        MooSelect(
                            options = AppLanguage.entries.map { it.code to container.t("settings.language.${it.code}") },
                            value = settings.general.language,
                            onChange = { value ->
                                container.updateSettings { it.copy(general = it.general.copy(language = value)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.closeBehavior")) {
                        MooSegmented(
                            options = CloseBehavior.entries.map { it.name.lowercase() to container.t("settings.close.${it.name.lowercase()}") },
                            value = settings.general.closeBehavior,
                            onChange = { value ->
                                container.updateSettings { it.copy(general = it.general.copy(closeBehavior = value)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.tray")) {
                        MooSwitch(settings.general.trayEnabled) { enabled ->
                            container.updateSettings { current -> current.copy(general = current.general.copy(trayEnabled = enabled)) }
                        }
                    }
                    val trayReason = com.rememberber.mootool.next.compose.app.AppTray.unavailableReason()
                    if (trayReason != null) {
                        Text(container.t("settings.tray.unavailable"), color = colors.warning, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    } else if (settings.general.trayEnabled) {
                        Text(container.t("settings.tray.active"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    Toggle(container, container.t("settings.startMaximized"), settings.general.startMaximized) {
                        container.updateSettings { it.copy(general = it.general.copy(startMaximized = !it.general.startMaximized)) }
                    }
                    Toggle(container, container.t("settings.autoCheckUpdates"), settings.general.autoCheckUpdates) {
                        container.updateSettings { it.copy(general = it.general.copy(autoCheckUpdates = !it.general.autoCheckUpdates)) }
                    }
                    Toggle(container, container.t("settings.autoDownloadUpdates"), settings.general.autoDownloadUpdates) {
                        container.updateSettings { it.copy(general = it.general.copy(autoDownloadUpdates = !it.general.autoDownloadUpdates)) }
                    }
                    Text(container.t("settings.update.autoDownloadHint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                SettingsNavCategory.Appearance -> SettingsGroup(container.t("settings.group.theme")) {
                    SettingRow(container.t("settings.style")) {
                        MooSelect(
                            options = InterfaceStyle.entries.map { style ->
                                val id = style.name.lowercase().replace("miuiv5", "miui-v5")
                                id to container.t("settings.style.$id")
                            },
                            value = settings.appearance.interfaceStyle,
                            onChange = { value ->
                                container.updateSettings { it.copy(appearance = it.appearance.copy(interfaceStyle = value)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.theme")) {
                        MooSegmented(
                            options = listOf("system", "light", "dark").map { it to container.t("settings.theme.$it") },
                            value = settings.appearance.theme,
                            onChange = { value ->
                                container.updateSettings { it.copy(appearance = it.appearance.copy(theme = value)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.accent")) {
                        AccentSwatches(
                            value = settings.appearance.accentColor,
                            onChange = { value ->
                                container.updateSettings { it.copy(appearance = it.appearance.copy(accentColor = value)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.fontSize")) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Slider(
                                value = settings.appearance.fontSize.toFloat(),
                                onValueChange = { value ->
                                    val size = value.toInt().coerceIn(12, 18)
                                    container.updateSettings {
                                        it.copy(appearance = it.appearance.copy(fontSize = size))
                                    }
                                },
                                valueRange = 12f..18f,
                                steps = 5,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                settings.appearance.fontSize.toString(),
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.widthIn(min = 20.dp),
                            )
                        }
                    }
                    Toggle(container, container.t("settings.unifiedBackground"), settings.appearance.unifiedBackground) {
                        container.updateSettings { it.copy(appearance = it.appearance.copy(unifiedBackground = !it.appearance.unifiedBackground)) }
                    }
                    SettingRow(container.t("settings.fontFamily")) {
                        MooSelect(
                            options = listOf("system", "sans-serif", "serif", "monospace").map { it to it },
                            value = settings.appearance.fontFamily,
                            onChange = { value ->
                                container.updateSettings { it.copy(appearance = it.appearance.copy(fontFamily = value)) }
                            }
                        )
                    }
                    Text(container.t("settings.style.live"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                SettingsNavCategory.Layout -> {
                    SettingsGroup(container.t("settings.group.navigation")) {
                    Toggle(container, container.t("settings.nav.recent"), settings.layout.showRecent) {
                        container.updateSettings { current -> current.copy(layout = current.layout.copy(showRecent = !current.layout.showRecent)) }
                    }
                    Toggle(container, container.t("settings.nav.separators"), settings.layout.showSeparators) {
                        container.updateSettings { current -> current.copy(layout = current.layout.copy(showSeparators = !current.layout.showSeparators)) }
                    }
                    Toggle(container, container.t("settings.nav.collapsed"), settings.layout.hideNavigationTitles) {
                        container.updateSettings { current -> current.copy(layout = current.layout.copy(hideNavigationTitles = !current.layout.hideNavigationTitles)) }
                    }
                    Toggle(container, container.t("settings.nav.compact"), settings.layout.compactNavigation) {
                        container.updateSettings { current -> current.copy(layout = current.layout.copy(compactNavigation = !current.layout.compactNavigation)) }
                    }
                    SettingRow(container.t("settings.nav.style")) {
                        MooSegmented(
                            options = NavigationStyle.entries.map { it.name.lowercase() to container.t("settings.nav.style.${it.name.lowercase()}") },
                            value = settings.layout.navigationStyle,
                            onChange = { value ->
                                container.updateSettings { it.copy(layout = it.layout.copy(navigationStyle = value)) }
                            }
                        )
                    }
                    }
                    SettingsGroup(container.t("settings.nav.toolsTitle")) {
                    Text(container.t("settings.nav.toolsDescription"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    val navigationToolIds = NavigationToolVisibility.navigationToolIds
                    val visibleCount = NavigationToolVisibility.visibleNavigationToolCount(
                        settings.layout.hiddenNavigationToolIds
                    )
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MooButton(
                            container.t("settings.nav.showAll"),
                            enabled = visibleCount < navigationToolIds.size,
                            p5Toolbar = true,
                            onClick = {
                                container.updateSettings {
                                    it.copy(layout = it.layout.copy(hiddenNavigationToolIds = NavigationToolVisibility.showAll()))
                                }
                            }
                        )
                        MooButton(
                            container.t("settings.nav.hideAll"),
                            enabled = visibleCount > 0,
                            p5Toolbar = true,
                            onClick = {
                                container.updateSettings {
                                    it.copy(layout = it.layout.copy(hiddenNavigationToolIds = NavigationToolVisibility.hideAll()))
                                }
                            }
                        )
                    }
                    Text(container.t("settings.nav.hidden"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    NavigationToolVisibilityList(container, settings)
                    }
                    SettingsGroup(container.t("app.group.manage.title")) {
                    Text(container.t("app.group.manage.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("app.nav.manageGroups"), onClick = { container.setGroupManagerOpen(true) })
                    }
                    }
                }
                SettingsNavCategory.Editor -> SettingsGroup(container.t("settings.group.editor")) {
                    Toggle(container, container.t("settings.softWrap"), settings.editor.softWrap) {
                        container.updateSettings { current -> current.copy(editor = current.editor.copy(softWrap = !current.editor.softWrap)) }
                    }
                    SettingRow(container.t("settings.sqlDialect")) {
                        val dialects = com.rememberber.mootool.next.compose.domain.SqlFormatEngine.dialects
                        val current = dialects.firstOrNull { it.equals(settings.editor.sqlDialect, true) }
                            ?: if (settings.editor.sqlDialect.equals("mysql", true)) "MySQL" else settings.editor.sqlDialect
                        MooSelect(
                            options = dialects.map { it to it },
                            value = current,
                            onChange = { dialect ->
                                container.updateSettings { it.copy(editor = it.editor.copy(sqlDialect = dialect)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.jsonFontName")) {
                        FontSelect(
                            value = settings.editor.jsonFontName,
                            ariaLabel = container.t("settings.jsonFontName"),
                            labels = mapOf("ui-monospace" to container.t("quickNote.font.mono")),
                            searchPlaceholder = container.t("settings.jsonFontName"),
                            onChange = { name ->
                                container.updateSettings { it.copy(editor = it.editor.copy(jsonFontName = name)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.jsonFontSize")) {
                        SettingCommitTextField(
                            settings.editor.jsonFontSize.toString(),
                            onCommit = { draft ->
                                when (val outcome = SettingsEditorNumericNormalize.commitJsonFontSize(draft)) {
                                    is NumericSettingCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(editor = current.editor.copy(jsonFontSize = outcome.value))
                                        }
                                        true
                                    }
                                    NumericSettingCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.numericInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    SettingRow(container.t("settings.quickNoteFontName")) {
                        FontSelect(
                            value = settings.editor.quickNoteFontName,
                            ariaLabel = container.t("settings.quickNoteFontName"),
                            labels = mapOf("ui-monospace" to container.t("quickNote.font.mono")),
                            searchPlaceholder = container.t("settings.quickNoteFontName"),
                            onChange = { name ->
                                container.updateSettings { it.copy(editor = it.editor.copy(quickNoteFontName = name)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.quickNoteFontSize")) {
                        SettingCommitTextField(
                            settings.editor.quickNoteFontSize.toString(),
                            onCommit = { draft ->
                                when (val outcome = SettingsEditorNumericNormalize.commitQuickNoteFontSize(draft)) {
                                    is NumericSettingCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(editor = current.editor.copy(quickNoteFontSize = outcome.value))
                                        }
                                        true
                                    }
                                    NumericSettingCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.numericInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                }
                SettingsNavCategory.Data -> {
                    SettingsGroup(container.t("settings.group.storage")) {
                        Text(
                            container.t("settings.dataPath.defaultHint", mapOf("path" to container.directories.dataRoot.toString())),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                        DirectorySettingRow(
                            container = container,
                            label = container.t("settings.dataPath"),
                            value = settings.data.directory,
                            placeholder = container.t("settings.vault.default"),
                            onCommit = { path ->
                                container.updateSettings { current -> current.copy(data = current.data.copy(directory = path)) }
                            },
                        )
                        Text(
                            container.t(
                                "settings.dataPath.effectiveHint",
                                mapOf("path" to container.dataDirectories().dataRoot.toString()),
                            ),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                        Text(
                            container.t(
                                "settings.configPath.effectiveHint",
                                mapOf("path" to container.directories.configRoot.toString()),
                            ),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MooButton(
                                container.t("settings.openData"),
                                onClick = { container.openDirectory(container.dataDirectories().dataRoot) },
                            )
                            MooButton(
                                container.t("settings.openConfig"),
                                p5Toolbar = true,
                                onClick = { container.openDirectory(container.directories.configRoot) },
                            )
                        }
                    }
                    SettingsGroup(container.t("settings.group.backup")) {
                    Text(container.t("settings.backup.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    var backupPaths by remember { mutableStateOf<BackupInfo?>(null) }
                    LaunchedEffect(settings.data.directory, settings.vault.jsonPath, settings.vault.quickNotePath, sessionGeneration) {
                        backupPaths = container.backupInfo()
                    }
                    backupPaths?.let { info ->
                        Text(
                            container.t("settings.backup.pathsHint"),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                        BackupPathRow(container, container.t("settings.backup.database"), info.databasePath, BackupOpenLocation.DatabaseFile)
                        BackupPathRow(container, container.t("settings.backup.settings"), info.settingsPath, BackupOpenLocation.SettingsConfig)
                        BackupPathRow(container, container.t("settings.backup.images"), info.imagesPath, BackupOpenLocation.Images)
                        BackupPathRow(container, container.t("settings.vault.quickNote"), info.quickNotePath, BackupOpenLocation.QuickNote)
                        BackupPathRow(container, container.t("settings.vault.json"), info.jsonVaultPath, BackupOpenLocation.JsonVault)
                    }
                    var backupNotice by remember { mutableStateOf("") }
                    var backupError by remember { mutableStateOf("") }
                    if (backupError.isNotBlank()) Text(backupError, color = colors.danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
                    if (backupNotice.isNotBlank()) Text(backupNotice, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("settings.backup.export"), onClick = {
                        val dialog = java.awt.FileDialog(null as java.awt.Frame?, container.t("settings.backup.export"), java.awt.FileDialog.SAVE)
                        dialog.file = com.rememberber.mootool.next.compose.storage.BackupEngine.defaultZipName()
                        dialog.isVisible = true
                        val directory = dialog.directory
                        val file = dialog.file
                        if (!directory.isNullOrBlank() && !file.isNullOrBlank()) {
                            runCatching {
                                val name = if (file.endsWith(".zip")) file else "$file.zip"
                                container.exportBackup(java.nio.file.Path.of(directory, name))
                            }.onSuccess {
                                backupError = ""
                                backupNotice = container.t("settings.backup.exported", mapOf("count" to it.manifest.files.size.toString()))
                                container.toastSuccess(backupNotice)
                            }.onFailure {
                                backupNotice = ""
                                backupError = it.message ?: container.t("settings.backup.failed")
                                container.toastError(backupError)
                            }
                        }
                    })
                    MooButton(container.t("settings.backup.preview"), onClick = {
                        chooseBackupZip()?.let { zip ->
                            runCatching { container.previewBackup(zip) }
                                .onSuccess {
                                    backupError = ""
                                    backupNotice = container.t(
                                        "settings.backup.previewResult",
                                        mapOf("product" to it.productId, "version" to it.appVersion, "count" to it.files.size.toString())
                                    )
                                }
                                .onFailure {
                                    backupNotice = ""
                                    backupError = it.message ?: container.t("settings.backup.failed")
                                }
                        }
                    })
                    MooButton(container.t("settings.backup.restore"), onClick = {
                        chooseBackupZip()?.let { zip ->
                            runCatching { container.restoreBackup(zip) }
                                .onSuccess {
                                    backupError = ""
                                    backupNotice = container.t("settings.backup.restored")
                                    container.toastSuccess(backupNotice)
                                }
                                .onFailure {
                                    backupNotice = ""
                                    backupError = it.message ?: container.t("settings.backup.failed")
                                    container.toastError(backupError)
                                }
                        }
                    })
                    }
                    Text(container.t("settings.backup.credentials"), color = colors.warning, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    MigrationSettingsPanel(container)
                }
                SettingsNavCategory.About -> SettingsGroup(container.t("settings.about")) {
                    val update by container.updates.state.collectAsState()
                    val result = update.result
                    MooPageTitle(ProductIdentity.DISPLAY_NAME)
                    Text("v${ProductIdentity.VERSION}", color = colors.textSecondary)
                    Text(ProductIdentity.APPLICATION_ID, color = colors.textSecondary)
                    Text(container.t("settings.about.bundle"), color = colors.textSecondary, fontSize = 12.sp)
                    Text("${container.t("settings.about.upgradeCode")}: ${InstallIdentity.WINDOWS_UPGRADE_UUID}", color = colors.textSecondary, fontSize = 12.sp)
                    Text("${container.t("settings.about.linuxPackage")}: ${InstallIdentity.LINUX_PACKAGE}", color = colors.textSecondary, fontSize = 12.sp)
                    Text(container.t("settings.about.uninstallHint"), color = colors.textSecondary, fontSize = 12.sp)
                    InstallIdentity.uninstallMustNotTouch().forEach { path ->
                        Text("· $path", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Text(container.t("settings.update.current", mapOf("version" to ProductIdentity.VERSION)), color = colors.textSecondary)
                    Text(updateStatusLabel(container, update), color = if (update.error.isNotBlank()) colors.danger else colors.textPrimary)
                    if (result?.latestVersion?.isNotBlank() == true && result.status.name.lowercase() != "unpublished") {
                        Text(container.t("settings.update.latest", mapOf("version" to result.latestVersion)), color = colors.textSecondary)
                    }
                    if (UpdateAboutPresentation.showReleaseNotes(result?.releaseNotes, result?.latestVersion, result?.status?.name)) {
                        Label(container.t("settings.update.notes"))
                        Text(result!!.releaseNotes, color = colors.textSecondary, fontSize = 12.sp)
                    }
                    val progress = update.progress
                    if (progress != null && (update.status == "downloading" || update.status == "ready")) {
                        Text(
                            container.t(
                                "settings.update.progress",
                                mapOf(
                                    "percent" to progress.percent.toString(),
                                    "transferred" to progress.transferred.toString(),
                                    "total" to progress.total.toString()
                                )
                            ),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton(
                            container.t("settings.update.check"),
                            enabled = !update.busy,
                            onClick = { container.updates.check(autoDownload = settings.general.autoDownloadUpdates) }
                        )
                        if (UpdateAboutPresentation.showDownloadAction(
                                hasDownloadPack = result?.download != null,
                                installerReady = update.downloaded != null,
                            )
                        ) {
                            MooButton(
                                container.t("settings.update.download"),
                                prominent = true,
                                enabled = !update.busy,
                                onClick = { container.updates.download() }
                            )
                        }
                        if (UpdateAboutPresentation.showCancelDownloadAction(update.status)) {
                            MooButton(container.t("settings.update.cancel"), onClick = { container.updates.cancel() })
                        }
                        if (UpdateAboutPresentation.showOpenInstallerAction(update.downloaded != null)) {
                            MooButton(
                                container.t("settings.update.openInstaller"),
                                prominent = true,
                                enabled = !update.busy,
                                onClick = { container.updates.openInstaller() }
                            )
                        }
                        MooButton(container.t("settings.update.openRelease"), onClick = { container.updates.openReleasePage() })
                    }
                    Text(container.t("settings.update.manualInstall"), color = colors.warning, fontSize = 12.sp)
                }
                SettingsNavCategory.Ai -> AiIntegrationSettingsPanel(container)
                SettingsNavCategory.Runtime -> RuntimeSettingsPanel(container)
                SettingsNavCategory.Vault -> {
                    SettingsGroup(container.t("settings.group.vaultPaths")) {
                    Text(container.t("settings.vault.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    DirectorySettingRow(
                        container = container,
                        label = container.t("settings.vault.quickNote"),
                        value = settings.vault.quickNotePath,
                        placeholder = container.t("settings.vault.default"),
                        onCommit = { path ->
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(quickNotePath = path)) }
                        }
                    )
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        MooButton(container.t("quickNote.openVault"), onClick = { container.openDirectory(container.noteVault().root()) })
                    }
                    DirectorySettingRow(
                        container = container,
                        label = container.t("settings.vault.json"),
                        value = settings.vault.jsonPath,
                        placeholder = container.t("settings.vault.default"),
                        onCommit = { path ->
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(jsonPath = path)) }
                        }
                    )
                    SettingRow(container.t("settings.vault.jsonTreeExpand")) {
                        MooSegmented(
                            options = listOf("smart", "expandAll", "collapseAll").map { it to container.t("settings.vault.expand.$it") },
                            value = settings.vault.jsonTreeExpandMode,
                            onChange = { mode ->
                                container.updateSettings { it.copy(vault = it.vault.copy(jsonTreeExpandMode = mode)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.vault.quickNoteTreeExpand")) {
                        MooSegmented(
                            options = listOf("smart", "expandAll", "collapseAll").map { it to container.t("settings.vault.expand.$it") },
                            value = settings.vault.quickNoteTreeExpandMode,
                            onChange = { mode ->
                                container.updateSettings { it.copy(vault = it.vault.copy(quickNoteTreeExpandMode = mode)) }
                            }
                        )
                    }
                    }
                    SettingsGroup(container.t("settings.group.git")) {
                    SettingRow(container.t("settings.vault.gitUsername")) {
                        SettingCommitTextField(
                            settings.vault.gitUsername,
                            onCommit = { draft ->
                                container.updateSettings { current ->
                                    current.copy(
                                        vault = current.vault.copy(
                                            gitUsername = draft.trim().take(128),
                                        ),
                                    )
                                }
                                true
                            },
                            placeholder = "MooTool Next Compose",
                        )
                    }
                    SettingRow(container.t("settings.vault.gitRemote")) {
                        SettingCommitTextField(
                            settings.vault.gitRemote,
                            onCommit = { draft ->
                                when (val outcome = SettingsVaultGitNormalize.commitGitRemote(draft)) {
                                    GitRemoteCommitResult.Cleared -> {
                                        container.updateSettings { current ->
                                            current.copy(vault = current.vault.copy(gitRemote = ""))
                                        }
                                        true
                                    }
                                    is GitRemoteCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(vault = current.vault.copy(gitRemote = outcome.remote))
                                        }
                                        true
                                    }
                                    GitRemoteCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.gitRemoteInvalid"))
                                        false
                                    }
                                }
                            },
                            placeholder = container.t("git.remotePlaceholder"),
                        )
                    }
                    SettingRow(container.t("settings.vault.gitToken")) {
                        SettingCommitTextField(
                            settings.vault.gitToken,
                            onCommit = { draft ->
                                container.updateSettings { current ->
                                    current.copy(
                                        vault = current.vault.copy(
                                            gitToken = draft.trim().take(2048),
                                        ),
                                    )
                                }
                                true
                            },
                            placeholder = container.t("settings.vault.gitTokenHint"),
                        )
                    }
                    Text(container.t("settings.vault.gitTokenHint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    Toggle(container, container.t("settings.autoCommit"), settings.vault.autoCommit) {
                        container.updateSettings { it.copy(vault = it.vault.copy(autoCommit = !it.vault.autoCommit)) }
                    }
                    SettingRow(container.t("settings.autoCommitIdleSeconds")) {
                        SettingCommitTextField(
                            settings.vault.autoCommitIdleSeconds.toString(),
                            onCommit = { draft ->
                                when (val outcome = SettingsVaultNumericNormalize.commitAutoCommitIdleSeconds(draft)) {
                                    is VaultNumericCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(vault = current.vault.copy(autoCommitIdleSeconds = outcome.value))
                                        }
                                        true
                                    }
                                    VaultNumericCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.numericInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    SettingRow(container.t("settings.autoCommitInactiveSeconds")) {
                        SettingCommitTextField(
                            settings.vault.autoCommitInactiveSeconds.toString(),
                            onCommit = { draft ->
                                when (val outcome = SettingsVaultNumericNormalize.commitAutoCommitInactiveSeconds(draft)) {
                                    is VaultNumericCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(vault = current.vault.copy(autoCommitInactiveSeconds = outcome.value))
                                        }
                                        true
                                    }
                                    VaultNumericCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.numericInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    SettingRow(container.t("settings.autoPullMinutes")) {
                        SettingCommitTextField(
                            settings.vault.autoPullMinutes.toString(),
                            onCommit = { draft ->
                                when (val outcome = SettingsVaultNumericNormalize.commitAutoPullMinutes(draft)) {
                                    is VaultNumericCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(vault = current.vault.copy(autoPullMinutes = outcome.value))
                                        }
                                        true
                                    }
                                    VaultNumericCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.numericInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    Toggle(container, container.t("settings.vault.hideIgnored"), settings.vault.hideGitignoredFiles) {
                        container.updateSettings { it.copy(vault = it.vault.copy(hideGitignoredFiles = !it.vault.hideGitignoredFiles)) }
                    }
                    }
                }
                SettingsNavCategory.Network -> {
                    SettingsGroup(container.t("settings.group.proxy")) {
                    val proxyEnabled = settings.network.proxyEnabled
                    Toggle(container, container.t("settings.proxy.enabled"), proxyEnabled) {
                        container.updateSettings { it.copy(network = it.network.copy(proxyEnabled = !it.network.proxyEnabled)) }
                    }
                    SettingRow(container.t("settings.proxy.host")) {
                        SettingCommitTextField(
                            settings.network.proxyHost,
                            onCommit = { draft ->
                                container.updateSettings { current ->
                                    current.copy(
                                        network = current.network.copy(
                                            proxyHost = draft.trim().take(256),
                                        ),
                                    )
                                }
                                true
                            },
                            enabled = proxyEnabled,
                        )
                    }
                    SettingRow(container.t("settings.proxy.port")) {
                        SettingCommitTextField(
                            settings.network.proxyPort,
                            onCommit = { draft ->
                                when (val outcome = SettingsNetworkNormalize.commitProxyPort(draft)) {
                                    ProxyPortCommitResult.Cleared -> {
                                        container.updateSettings { current ->
                                            current.copy(network = current.network.copy(proxyPort = ""))
                                        }
                                        true
                                    }
                                    is ProxyPortCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(network = current.network.copy(proxyPort = outcome.port))
                                        }
                                        true
                                    }
                                    ProxyPortCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.network.proxyPortInvalid"))
                                        false
                                    }
                                }
                            },
                            enabled = proxyEnabled,
                        )
                    }
                    SettingRow(container.t("settings.proxy.username")) {
                        SettingCommitTextField(
                            settings.network.proxyUsername,
                            onCommit = { draft ->
                                container.updateSettings { current ->
                                    current.copy(
                                        network = current.network.copy(
                                            proxyUsername = draft.trim().take(128),
                                        ),
                                    )
                                }
                                true
                            },
                            enabled = proxyEnabled,
                        )
                    }
                    SettingRow(container.t("settings.proxy.password")) {
                        SettingCommitTextField(
                            settings.network.proxyPassword,
                            onCommit = { draft ->
                                container.updateSettings { current ->
                                    current.copy(
                                        network = current.network.copy(
                                            proxyPassword = draft.trim().take(512),
                                        ),
                                    )
                                }
                                true
                            },
                            enabled = proxyEnabled,
                        )
                    }
                    }
                    SettingsGroup(container.t("settings.group.timeouts")) {
                    SettingRow(container.t("settings.httpTimeout")) {
                        SettingCommitTextField(
                            settings.network.requestTimeoutMs.toString(),
                            onCommit = { draft ->
                                when (
                                    val outcome = SettingsNetworkNormalize.commitTimeoutMs(
                                        draft,
                                        minimum = 1_000,
                                        maximum = 120_000,
                                    )
                                ) {
                                    is TimeoutCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(
                                                network = current.network.copy(requestTimeoutMs = outcome.milliseconds),
                                            )
                                        }
                                        true
                                    }
                                    TimeoutCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.network.timeoutInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    SettingRow(container.t("settings.translationTimeout")) {
                        SettingCommitTextField(
                            settings.network.translationTimeoutMs.toString(),
                            onCommit = { draft ->
                                when (
                                    val outcome = SettingsNetworkNormalize.commitTimeoutMs(
                                        draft,
                                        minimum = 1_000,
                                        maximum = 120_000,
                                    )
                                ) {
                                    is TimeoutCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(
                                                network = current.network.copy(translationTimeoutMs = outcome.milliseconds),
                                            )
                                        }
                                        true
                                    }
                                    TimeoutCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.network.timeoutInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    }
                }
                SettingsNavCategory.Tools -> SettingsGroup(container.t("settings.group.toolDefaults")) {
                    SettingRow(container.t("settings.tools.qrSize")) {
                        SettingCommitTextField(
                            settings.tools.qrCodeSize.toString(),
                            onCommit = { draft ->
                                when (val outcome = SettingsToolsNumericNormalize.commitQrCodeSize(draft)) {
                                    is NumericSettingCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(tools = current.tools.copy(qrCodeSize = outcome.value))
                                        }
                                        true
                                    }
                                    NumericSettingCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.numericInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    SettingRow(container.t("settings.tools.qrError")) {
                        MooSegmented(
                            options = listOf("L", "M", "Q", "H").map { it to it },
                            value = settings.tools.qrErrorCorrection,
                            onChange = { level ->
                                container.updateSettings { it.copy(tools = it.tools.copy(qrErrorCorrection = level)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.tools.randomLength")) {
                        SettingCommitTextField(
                            settings.tools.randomStringLength.toString(),
                            onCommit = { draft ->
                                when (val outcome = SettingsToolsNumericNormalize.commitRandomStringLength(draft)) {
                                    is NumericSettingCommitResult.Accepted -> {
                                        container.updateSettings { current ->
                                            current.copy(tools = current.tools.copy(randomStringLength = outcome.value))
                                        }
                                        true
                                    }
                                    NumericSettingCommitResult.Rejected -> {
                                        container.toastError(container.t("settings.vault.numericInvalid"))
                                        false
                                    }
                                }
                            },
                        )
                    }
                    Text(container.t("settings.tools.exportDirHint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    DirectorySettingRow(
                        container = container,
                        label = container.t("settings.tools.exportDir"),
                        value = settings.tools.exportDirectory,
                        placeholder = container.t("settings.tools.exportDirDefault"),
                        onCommit = { path ->
                            container.updateSettings { current -> current.copy(tools = current.tools.copy(exportDirectory = path)) }
                        }
                    )
                    SettingRow(container.t("settings.tools.translator")) {
                        MooSegmented(
                            options = listOf("google", "bing").map { it to it },
                            value = settings.tools.translationProvider,
                            onChange = { provider ->
                                container.updateSettings { it.copy(tools = it.tools.copy(translationProvider = provider)) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.tools.sourceLang")) {
                        SettingCommitTextField(
                            settings.tools.translationSourceLang,
                            onCommit = { draft ->
                                val (source, target) = SettingsToolsTranslationNormalize.commitLanguagePair(
                                    draft,
                                    settings.tools.translationTargetLang,
                                )
                                container.updateSettings { current ->
                                    current.copy(
                                        tools = current.tools.copy(
                                            translationSourceLang = source,
                                            translationTargetLang = target,
                                        ),
                                    )
                                }
                                true
                            },
                        )
                    }
                    SettingRow(container.t("settings.tools.targetLang")) {
                        SettingCommitTextField(
                            settings.tools.translationTargetLang,
                            onCommit = { draft ->
                                val (source, target) = SettingsToolsTranslationNormalize.commitLanguagePair(
                                    settings.tools.translationSourceLang,
                                    draft,
                                )
                                container.updateSettings { current ->
                                    current.copy(
                                        tools = current.tools.copy(
                                            translationSourceLang = source,
                                            translationTargetLang = target,
                                        ),
                                    )
                                }
                                true
                            },
                        )
                    }
                }
                SettingsNavCategory.Shortcuts -> SettingsGroup(container.t("settings.group.shortcuts")) {
                    var searchDraft by remember(settings.shortcuts.search) { mutableStateOf(settings.shortcuts.search) }
                    var settingsDraft by remember(settings.shortcuts.settings) { mutableStateOf(settings.shortcuts.settings) }
                    var shortcutError by remember { mutableStateOf("") }
                    Text(container.t("settings.shortcuts.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    SettingRow(container.t("settings.shortcuts.search")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingTextField(searchDraft, { value ->
                        searchDraft = value
                        val next = value.trim().ifBlank { "Meta+K" }
                        if (ShortcutBindings.conflict(next, settings.shortcuts.settings)) {
                            shortcutError = container.t("settings.shortcuts.conflict")
                        } else {
                            shortcutError = ""
                            container.updateSettings { it.copy(shortcuts = it.shortcuts.copy(search = next)) }
                        }
                    })
                    MooKbd(ShortcutBindings.formatDisplay(searchDraft))
                    }
                    }
                    SettingRow(container.t("settings.shortcuts.settings")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingTextField(settingsDraft, { value ->
                        settingsDraft = value
                        val next = value.trim().ifBlank { "Meta+Comma" }
                        if (ShortcutBindings.conflict(next, settings.shortcuts.search)) {
                            shortcutError = container.t("settings.shortcuts.conflict")
                        } else {
                            shortcutError = ""
                            container.updateSettings { it.copy(shortcuts = it.shortcuts.copy(settings = next)) }
                        }
                    })
                    MooKbd(ShortcutBindings.formatDisplay(settingsDraft))
                    }
                    }
                    if (shortcutError.isNotBlank()) {
                        Text(shortcutError, color = colors.danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                    Text(container.t("settings.shortcuts.help"), color = colors.textSecondary, fontSize = 12.sp)
                }
            }
            }
        }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
}

@Composable
private fun NavigationToolVisibilityList(container: AppContainer, settings: AppSettings) {
    val colors = MooTheme.colors
    val hidden = settings.layout.hiddenNavigationToolIds.toSet()
    Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ToolRegistry.groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    container.t(group.titleKey),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                group.toolIds.forEach { toolId ->
                    val tool = ToolRegistry.byId.getValue(toolId)
                    val isHidden = tool.id.id in hidden
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .mooFocusClickable {
                                container.updateSettings { current ->
                                    val nextHidden = if (isHidden) {
                                        current.layout.hiddenNavigationToolIds - tool.id.id
                                    } else {
                                        current.layout.hiddenNavigationToolIds + tool.id.id
                                    }
                                    current.copy(
                                        layout = current.layout.copy(
                                            hiddenNavigationToolIds = NavigationToolVisibility.normalizeHiddenNavigationToolIds(nextHidden)
                                        )
                                    )
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolIcon(tool.id, colors.textSecondary, Modifier.size(15.dp))
                        Text(
                            container.t(tool.titleKey),
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        MooSwitch(checked = !isHidden) {
                            container.updateSettings { current ->
                                val nextHidden = if (isHidden) {
                                    current.layout.hiddenNavigationToolIds - tool.id.id
                                } else {
                                    current.layout.hiddenNavigationToolIds + tool.id.id
                                }
                                current.copy(
                                    layout = current.layout.copy(
                                        hiddenNavigationToolIds = NavigationToolVisibility.normalizeHiddenNavigationToolIds(nextHidden)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Toggle(container: AppContainer, label: String, value: Boolean, onClick: () -> Unit) {
    SettingRow(label) {
        MooSwitch(value) { onClick() }
    }
}

@Composable
private fun DirectorySettingRow(
    container: AppContainer,
    label: String,
    value: String,
    placeholder: String,
    onCommit: (String) -> Unit,
) {
    SettingRow(label) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MooTextField(
                value,
                { commitVaultPath(container, it, onCommit) },
                modifier = Modifier.weight(1f),
                placeholder = placeholder
            )
            MooButton(
                container.t("settings.chooseDirectory"),
                p5Toolbar = true,
                onClick = {
                    container.chooseDirectory(label, value)?.let { picked ->
                        commitVaultPath(container, picked, onCommit)
                    }
                }
            )
        }
    }
}

@Composable
private fun BackupPathRow(
    container: AppContainer,
    label: String,
    path: String,
    openLocation: BackupOpenLocation,
) {
    val colors = MooTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, color = colors.textPrimary, fontSize = 12.sp)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                path,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            MooButton(
                container.t("settings.backup.open"),
                p5Toolbar = true,
                onClick = { container.openBackupLocation(openLocation) },
            )
        }
    }
}

private fun commitVaultPath(container: AppContainer, raw: String, apply: (String) -> Unit) {
    when (val normalized = VaultPathConfig.normalizedCustomRoot(raw)) {
        null -> {
            if (raw.trim().isNotEmpty()) {
                container.toastError(container.t("settings.vault.absoluteRequired"))
            }
        }
        else -> apply(normalized)
    }
}

private fun updateStatusLabel(container: AppContainer, state: UpdateUiState): String {
    if (state.error.isNotBlank()) return state.error
    val result = state.result
    val key = UpdateAboutPresentation.statusMessageKey(
        status = state.status,
        hasDownloadPack = result?.download != null,
    )
    return if (key == "settings.update.available" && result != null) {
        container.t(key, mapOf("version" to result.latestVersion))
    } else {
        container.t(key)
    }
}

private fun chooseBackupZip(): java.nio.file.Path? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Backup", java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return java.nio.file.Path.of(directory, file)
}
