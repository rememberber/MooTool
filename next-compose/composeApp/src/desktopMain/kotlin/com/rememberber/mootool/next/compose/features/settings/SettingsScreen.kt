package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.InstallIdentity
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.app.UpdateUiState
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.CloseBehavior
import com.rememberber.mootool.next.compose.model.InterfaceStyle
import com.rememberber.mootool.next.compose.model.NavigationStyle
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.domain.CrossProductImporter
import com.rememberber.mootool.next.compose.domain.ShortcutBindings
import com.rememberber.mootool.next.compose.ui.components.AccentSwatches
import com.rememberber.mootool.next.compose.ui.components.FontSelect
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooSegmented
import com.rememberber.mootool.next.compose.ui.components.MooSelect
import com.rememberber.mootool.next.compose.ui.components.MooSwitch
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.SettingRow
import com.rememberber.mootool.next.compose.ui.components.SettingTextField
import com.rememberber.mootool.next.compose.ui.components.SettingsGroup
import com.rememberber.mootool.next.compose.ui.components.SettingsNavItem
import com.rememberber.mootool.next.compose.ui.components.mooSidebarBackground
import com.rememberber.mootool.next.compose.ui.components.mooWorkspaceBackground
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

private enum class SettingsCategory { General, Appearance, Layout, Editor, Network, Data, Vault, Runtime, Tools, Shortcuts, About }

@Composable
fun SettingsScreen(container: AppContainer) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    var category by remember { mutableStateOf(SettingsCategory.General) }
    Row(Modifier.fillMaxSize().mooWorkspaceBackground()) {
        Column(Modifier.width(220.dp).fillMaxHeight().mooSidebarBackground().padding(vertical = 12.dp)) {
            SettingsCategory.entries.forEach { item ->
                SettingsNavItem(
                    label = container.t("settings.${item.name.lowercase()}"),
                    selected = item == category,
                    onClick = { category = item }
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).background(colors.toolbarBrush()).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(container.t("settings.${category.name.lowercase()}"), fontSize = 18.sp, color = colors.textPrimary)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (category) {
                SettingsCategory.General -> SettingsGroup(container.t("settings.group.application")) {
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
                SettingsCategory.Appearance -> SettingsGroup(container.t("settings.group.theme")) {
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
                    SettingRow(container.t("settings.fontSize") + ": ${settings.appearance.fontSize}") {
                        MooSegmented(
                            options = listOf(12, 13, 14, 16, 18).map { it.toString() to it.toString() },
                            value = settings.appearance.fontSize.toString(),
                            onChange = { value ->
                                container.updateSettings { it.copy(appearance = it.appearance.copy(fontSize = value.toInt())) }
                            }
                        )
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
                SettingsCategory.Layout -> {
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
                    Text(container.t("settings.nav.hidden"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    ToolRegistry.tools.filter { it.id != ToolId.Mootool }.forEach { tool ->
                        val hidden = tool.id.id in settings.layout.hiddenNavigationToolIds
                        Toggle(container, container.t(tool.titleKey), !hidden) {
                            container.updateSettings { current ->
                                val next = if (hidden) current.layout.hiddenNavigationToolIds - tool.id.id
                                else current.layout.hiddenNavigationToolIds + tool.id.id
                                current.copy(layout = current.layout.copy(hiddenNavigationToolIds = next))
                            }
                        }
                    }
                    }
                    SettingsGroup(container.t("app.group.manage.title")) {
                    Text(container.t("app.group.manage.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("app.nav.manageGroups"), onClick = { container.setGroupManagerOpen(true) })
                    }
                    }
                }
                SettingsCategory.Editor -> SettingsGroup(container.t("settings.group.editor")) {
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
                    SettingRow(container.t("settings.jsonFontSize") + ": ${settings.editor.jsonFontSize}") {
                        MooSegmented(
                            options = listOf(12, 13, 14, 16, 18).map { it.toString() to it.toString() },
                            value = settings.editor.jsonFontSize.toString(),
                            onChange = { value ->
                                container.updateSettings { it.copy(editor = it.editor.copy(jsonFontSize = value.toInt())) }
                            }
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
                    SettingRow(container.t("settings.quickNoteFontSize") + ": ${settings.editor.quickNoteFontSize}") {
                        MooSegmented(
                            options = listOf(12, 13, 14, 16, 18).map { it.toString() to it.toString() },
                            value = settings.editor.quickNoteFontSize.toString(),
                            onChange = { value ->
                                container.updateSettings { it.copy(editor = it.editor.copy(quickNoteFontSize = value.toInt())) }
                            }
                        )
                    }
                }
                SettingsCategory.Data -> {
                    SettingsGroup(container.t("settings.group.storage")) {
                        SettingRow(container.t("settings.dataPath")) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(container.directories.dataRoot.toString(), color = colors.textSecondary, fontSize = 12.sp)
                                MooButton(container.t("settings.openData"), onClick = { container.openDirectory(container.directories.dataRoot) })
                            }
                        }
                    }
                    SettingsGroup(container.t("settings.group.backup")) {
                    Text(container.t("settings.backup.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
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
                            }.onFailure {
                                backupNotice = ""
                                backupError = it.message ?: container.t("settings.backup.failed")
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
                                }
                                .onFailure {
                                    backupNotice = ""
                                    backupError = it.message ?: container.t("settings.backup.failed")
                                }
                        }
                    })
                    }
                    Text(container.t("settings.backup.credentials"), color = colors.warning, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    SettingsGroup(container.t("settings.group.migration")) {
                    Text(container.t("settings.import.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    var importNotice by remember { mutableStateOf("") }
                    var importError by remember { mutableStateOf("") }
                    if (importError.isNotBlank()) Text(importError, color = colors.danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
                    if (importNotice.isNotBlank()) Text(importNotice, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
                    Row(Modifier.padding(14.dp)) {
                    MooButton(container.t("settings.import.choose"), onClick = {
                        val chooser = javax.swing.JFileChooser().apply {
                            fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                        }
                        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                            runCatching {
                                val preview = CrossProductImporter.inspect(chooser.selectedFile.toPath())
                                if (preview.notes + preview.jsonItems + preview.customGroups == 0) {
                                    error(preview.warnings.joinToString("; ").ifBlank { container.t("settings.import.empty") })
                                }
                                val backup = container.exportBackup(
                                    java.nio.file.Files.createTempDirectory("mootool-compose-pre-import").resolve("backup.zip")
                                )
                                val applied = CrossProductImporter.apply(
                                    preview,
                                    container.noteVault(),
                                    container.jsonVault,
                                    container.importedFingerprints()
                                )
                                if (preview.groups.isNotEmpty()) {
                                    container.updateSettings { current ->
                                        current.copy(layout = current.layout.copy(customGroups = current.layout.customGroups + preview.groups.filter { group ->
                                            current.layout.customGroups.none { it.id == group.id }
                                        }))
                                    }
                                }
                                container.rememberImport(preview.fingerprint)
                                Triple(preview, applied, backup)
                            }.onSuccess { (preview, applied, backup) ->
                                importError = ""
                                importNotice = container.t(
                                    "settings.import.done",
                                    mapOf(
                                        "kind" to preview.sourceKind,
                                        "notes" to applied.importedNotes.toString(),
                                        "json" to applied.importedJson.toString(),
                                        "groups" to applied.importedGroups.toString(),
                                        "skipped" to applied.skipped.toString(),
                                        "backup" to backup.zipPath.fileName.toString()
                                    )
                                )
                            }.onFailure {
                                importNotice = ""
                                importError = it.message ?: container.t("settings.import.failed")
                            }
                        }
                    })
                    }
                    }
                }
                SettingsCategory.About -> SettingsGroup(container.t("settings.about")) {
                    val update by container.updates.state.collectAsState()
                    val result = update.result
                    Text(ProductIdentity.DISPLAY_NAME, fontSize = 16.sp, color = colors.textPrimary)
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
                    if (result?.releaseNotes?.isNotBlank() == true) {
                        Label(container.t("settings.update.notes"))
                        Text(result.releaseNotes, color = colors.textSecondary, fontSize = 12.sp)
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
                        if (result?.download != null && update.downloaded == null) {
                            MooButton(
                                container.t("settings.update.download"),
                                primary = true,
                                enabled = !update.busy,
                                onClick = { container.updates.download() }
                            )
                        }
                        if (update.status == "downloading") {
                            MooButton(container.t("settings.update.cancel"), onClick = { container.updates.cancel() })
                        }
                        if (update.downloaded != null) {
                            MooButton(
                                container.t("settings.update.openInstaller"),
                                primary = true,
                                enabled = !update.busy,
                                onClick = { container.updates.openInstaller() }
                            )
                        }
                        MooButton(container.t("settings.update.openRelease"), onClick = { container.updates.openReleasePage() })
                    }
                    Text(container.t("settings.update.manualInstall"), color = colors.warning, fontSize = 12.sp)
                }
                SettingsCategory.Runtime -> SettingsGroup(container.t("settings.group.runtimes")) {
                    Text(container.t("settings.runtime.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    SettingRow("Java") {
                        SettingTextField(settings.runtime.javaPath, {
                            container.updateSettings { current -> current.copy(runtime = current.runtime.copy(javaPath = it)) }
                        }, placeholder = container.t("settings.runtime.auto"))
                    }
                    SettingRow("Groovy") {
                        SettingTextField(settings.runtime.groovyPath, {
                            container.updateSettings { current -> current.copy(runtime = current.runtime.copy(groovyPath = it)) }
                        }, placeholder = container.t("settings.runtime.auto"))
                    }
                    SettingRow("Python") {
                        SettingTextField(settings.runtime.pythonPath, {
                            container.updateSettings { current -> current.copy(runtime = current.runtime.copy(pythonPath = it)) }
                        }, placeholder = container.t("settings.runtime.auto"))
                    }
                    SettingRow("Node.js") {
                        SettingTextField(settings.runtime.nodePath, {
                            container.updateSettings { current -> current.copy(runtime = current.runtime.copy(nodePath = it)) }
                        }, placeholder = container.t("settings.runtime.auto"))
                    }
                    Text(container.t("settings.runtime.jvmNote"), color = colors.warning, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                SettingsCategory.Vault -> {
                    SettingsGroup(container.t("settings.group.vaultPaths")) {
                    Text(container.t("settings.vault.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    SettingRow(container.t("settings.vault.quickNote")) {
                        SettingTextField(settings.vault.quickNotePath, {
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(quickNotePath = it)) }
                        }, placeholder = container.t("settings.vault.default"))
                    }
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        MooButton(container.t("quickNote.openVault"), onClick = { container.openDirectory(container.noteVault().root()) })
                    }
                    SettingRow(container.t("settings.vault.json")) {
                        SettingTextField(settings.vault.jsonPath, {
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(jsonPath = it)) }
                        }, placeholder = container.t("settings.vault.default"))
                    }
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
                        SettingTextField(settings.vault.gitUsername, {
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(gitUsername = it)) }
                        }, placeholder = "MooTool Next Compose")
                    }
                    SettingRow(container.t("settings.vault.gitRemote")) {
                        SettingTextField(settings.vault.gitRemote, {
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(gitRemote = it)) }
                        }, placeholder = container.t("git.remotePlaceholder"))
                    }
                    SettingRow(container.t("settings.vault.gitToken")) {
                        SettingTextField(settings.vault.gitToken, {
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(gitToken = it)) }
                        }, placeholder = container.t("settings.vault.gitTokenHint"))
                    }
                    Text(container.t("settings.vault.gitTokenHint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    Toggle(container, container.t("settings.autoCommit"), settings.vault.autoCommit) {
                        container.updateSettings { it.copy(vault = it.vault.copy(autoCommit = !it.vault.autoCommit)) }
                    }
                    SettingRow(container.t("settings.autoCommitIdleSeconds")) {
                        SettingTextField(settings.vault.autoCommitIdleSeconds.toString(), {
                            it.toIntOrNull()?.let { value ->
                                container.updateSettings { current -> current.copy(vault = current.vault.copy(autoCommitIdleSeconds = value.coerceIn(5, 3600))) }
                            }
                        })
                    }
                    SettingRow(container.t("settings.autoCommitInactiveSeconds")) {
                        SettingTextField(settings.vault.autoCommitInactiveSeconds.toString(), {
                            it.toIntOrNull()?.let { value ->
                                container.updateSettings { current -> current.copy(vault = current.vault.copy(autoCommitInactiveSeconds = value.coerceIn(5, 3600))) }
                            }
                        })
                    }
                    SettingRow(container.t("settings.autoPullMinutes")) {
                        SettingTextField(settings.vault.autoPullMinutes.toString(), {
                            it.toIntOrNull()?.let { value ->
                                container.updateSettings { current -> current.copy(vault = current.vault.copy(autoPullMinutes = value.coerceIn(0, 1440))) }
                            }
                        })
                    }
                    Toggle(container, container.t("settings.vault.hideIgnored"), settings.vault.hideGitignoredFiles) {
                        container.updateSettings { it.copy(vault = it.vault.copy(hideGitignoredFiles = !it.vault.hideGitignoredFiles)) }
                    }
                    }
                }
                SettingsCategory.Network -> {
                    SettingsGroup(container.t("settings.group.proxy")) {
                    Toggle(container, container.t("settings.proxy.enabled"), settings.network.proxyEnabled) {
                        container.updateSettings { it.copy(network = it.network.copy(proxyEnabled = !it.network.proxyEnabled)) }
                    }
                    SettingRow(container.t("settings.proxy.host")) {
                        SettingTextField(settings.network.proxyHost, {
                            container.updateSettings { current -> current.copy(network = current.network.copy(proxyHost = it)) }
                        })
                    }
                    SettingRow(container.t("settings.proxy.port")) {
                        SettingTextField(settings.network.proxyPort, {
                            container.updateSettings { current -> current.copy(network = current.network.copy(proxyPort = it)) }
                        })
                    }
                    SettingRow(container.t("settings.proxy.username")) {
                        SettingTextField(settings.network.proxyUsername, {
                            container.updateSettings { current -> current.copy(network = current.network.copy(proxyUsername = it)) }
                        })
                    }
                    SettingRow(container.t("settings.proxy.password")) {
                        SettingTextField(settings.network.proxyPassword, {
                            container.updateSettings { current -> current.copy(network = current.network.copy(proxyPassword = it)) }
                        })
                    }
                    }
                    SettingsGroup(container.t("settings.group.timeouts")) {
                    SettingRow(container.t("settings.httpTimeout")) {
                        SettingTextField(settings.network.requestTimeoutMs.toString(), {
                            it.toIntOrNull()?.let { value ->
                                container.updateSettings { current -> current.copy(network = current.network.copy(requestTimeoutMs = value.coerceIn(1_000, 120_000))) }
                            }
                        })
                    }
                    SettingRow(container.t("settings.translationTimeout")) {
                        SettingTextField(settings.network.translationTimeoutMs.toString(), {
                            it.toIntOrNull()?.let { value ->
                                container.updateSettings { current -> current.copy(network = current.network.copy(translationTimeoutMs = value.coerceIn(1_000, 120_000))) }
                            }
                        })
                    }
                    }
                }
                SettingsCategory.Tools -> SettingsGroup(container.t("settings.group.toolDefaults")) {
                    SettingRow(container.t("settings.tools.qrSize") + ": ${settings.tools.qrCodeSize}") {
                        MooSegmented(
                            options = listOf(200, 300, 400, 600).map { it.toString() to it.toString() },
                            value = settings.tools.qrCodeSize.toString(),
                            onChange = { value ->
                                container.updateSettings { it.copy(tools = it.tools.copy(qrCodeSize = value.toInt())) }
                            }
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
                    SettingRow(container.t("settings.tools.randomLength") + ": ${settings.tools.randomStringLength}") {
                        MooSegmented(
                            options = listOf(8, 16, 32, 64).map { it.toString() to it.toString() },
                            value = settings.tools.randomStringLength.toString(),
                            onChange = { value ->
                                container.updateSettings { it.copy(tools = it.tools.copy(randomStringLength = value.toInt())) }
                            }
                        )
                    }
                    SettingRow(container.t("settings.tools.exportDir")) {
                        SettingTextField(settings.tools.exportDirectory, {
                            container.updateSettings { current -> current.copy(tools = current.tools.copy(exportDirectory = it)) }
                        })
                    }
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
                        SettingTextField(settings.tools.translationSourceLang, {
                            container.updateSettings { current -> current.copy(tools = current.tools.copy(translationSourceLang = it)) }
                        })
                    }
                    SettingRow(container.t("settings.tools.targetLang")) {
                        SettingTextField(settings.tools.translationTargetLang, {
                            container.updateSettings { current -> current.copy(tools = current.tools.copy(translationTargetLang = it)) }
                        })
                    }
                }
                SettingsCategory.Shortcuts -> SettingsGroup(container.t("settings.group.shortcuts")) {
                    var searchDraft by remember(settings.shortcuts.search) { mutableStateOf(settings.shortcuts.search) }
                    var settingsDraft by remember(settings.shortcuts.settings) { mutableStateOf(settings.shortcuts.settings) }
                    var shortcutError by remember { mutableStateOf("") }
                    Text(container.t("settings.shortcuts.hint"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    SettingRow(container.t("settings.shortcuts.search")) {
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
                    }
                    SettingRow(container.t("settings.shortcuts.settings")) {
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

@Composable
private fun Label(text: String) {
    Text(text, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
}

@Composable
private fun Toggle(container: AppContainer, label: String, value: Boolean, onClick: () -> Unit) {
    SettingRow(label) {
        MooSwitch(value) { onClick() }
    }
}

private fun updateStatusLabel(container: AppContainer, state: UpdateUiState): String {
    if (state.error.isNotBlank()) return state.error
    val result = state.result
    return when (state.status) {
        "checking" -> container.t("settings.update.checking")
        "downloading" -> container.t("settings.update.downloading")
        "ready" -> container.t("settings.update.ready")
        "cancelled" -> container.t("settings.update.cancelled")
        "available" -> if (result?.download == null) {
            container.t("settings.update.noPackage")
        } else {
            container.t("settings.update.available", mapOf("version" to result.latestVersion))
        }
        "latest" -> container.t("settings.update.upToDate")
        "unpublished" -> container.t("settings.update.unpublished")
        "idle" -> container.t("settings.update.idle")
        else -> state.message.ifBlank { container.t("settings.update.idle") }
    }
}

private fun chooseBackupZip(): java.nio.file.Path? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Backup", java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return java.nio.file.Path.of(directory, file)
}
