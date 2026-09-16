package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.CrossProductImporter
import com.rememberber.mootool.next.compose.domain.ElectronNextSettingsImport
import com.rememberber.mootool.next.compose.domain.LegacyJavaSettings
import com.rememberber.mootool.next.compose.domain.ImportPreview
import com.rememberber.mootool.next.compose.domain.totalItems
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.SettingRow
import com.rememberber.mootool.next.compose.ui.components.SettingsGroup
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

@Composable
fun MigrationSettingsPanel(container: AppContainer) {
    val colors = MooTheme.colors
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val scope = rememberCoroutineScope()
    var sourceDirectory by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var migrating by remember { mutableStateOf(false) }
    var importNotice by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf("") }
    var confirmOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sourceDirectory = container.defaultLegacyImportSource()
    }
    LaunchedEffect(sessionGeneration) {
        if (sessionGeneration == 0L) return@LaunchedEffect
        preview = null
        confirmOpen = false
    }

    SettingsGroup(container.t("settings.group.migration")) {
        Text(
            container.t("settings.import.hint"),
            color = colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.End
        ) {
            MooButton(
                container.t("settings.migration.scan"),
                p5Toolbar = true,
                enabled = !scanning && !migrating && sourceDirectory.trim().isNotEmpty(),
                onClick = {
                    scope.launch {
                        scanning = true
                        importError = ""
                        importNotice = ""
                        preview = null
                        val result = withContext(Dispatchers.IO) {
                            runCatching { CrossProductImporter.inspect(Path.of(sourceDirectory.trim())) }
                        }
                        scanning = false
                        result.onSuccess { preview = it }
                            .onFailure {
                                importError = it.message ?: container.t("settings.import.failed")
                            }
                    }
                }
            )
        }
        SettingRow(container.t("settings.migration.source")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MooTextField(
                    sourceDirectory,
                    {
                        sourceDirectory = it
                        preview = null
                    },
                    modifier = Modifier.weight(1f)
                )
                MooButton(
                    container.t("settings.chooseDirectory"),
                    p5Toolbar = true,
                    onClick = {
                        container.chooseDirectory(container.t("settings.migration.source"), sourceDirectory)?.let { picked ->
                            sourceDirectory = picked
                            preview = null
                        }
                    }
                )
            }
        }
        if (importError.isNotBlank()) {
            Text(importError, color = colors.danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
        }
        if (importNotice.isNotBlank()) {
            Text(importNotice, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
        }
        preview?.let { scanned ->
            val importedFingerprints = container.importedFingerprints()
            val alreadyMigrated = scanned.fingerprint in importedFingerprints ||
                scanned.legacyElectronFingerprint.orEmpty() in importedFingerprints
            val total = scanned.totalItems()
            val canImportSettings =
                scanned.electronSettings != null || LegacyJavaSettings.hasMigratableSettings(scanned.legacyJavaConfig)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (alreadyMigrated) {
                        container.t("settings.migration.alreadyMigrated")
                    } else if (scanned.electronMigrationRunComplete && canImportSettings) {
                        container.t("settings.migration.electronDataSkipped")
                    } else if (total > 0) {
                        container.t("settings.migration.ready", mapOf("count" to total.toString()))
                    } else {
                        container.t("settings.import.empty")
                    },
                    color = colors.textPrimary,
                    fontSize = 12.sp
                )
                Text(
                    if (scanned.databaseFound) container.t("settings.migration.databaseFound")
                    else container.t("settings.migration.databaseMissing"),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Text(
                    if (scanned.configFound) container.t("settings.migration.configFound")
                    else container.t("settings.migration.configMissing"),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Text(
                    if (scanned.legacyJavaConfigFound) container.t("settings.migration.legacyConfigFound")
                    else container.t("settings.migration.legacyConfigMissing"),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                if (
                    scanned.regexFavorites.isNotEmpty() ||
                    scanned.cronFavorites.isNotEmpty() ||
                    scanned.colorFavorites.isNotEmpty() ||
                    scanned.legacyHistory.isNotEmpty() ||
                    scanned.httpCollections.isNotEmpty() ||
                    scanned.hostProfiles.isNotEmpty() ||
                    scanned.translationWords.isNotEmpty() ||
                    scanned.translationHistory.isNotEmpty()
                ) {
                    Text(
                        container.t(
                            "settings.migration.sqliteExtras",
                            mapOf(
                                "regex" to scanned.regexFavorites.size.toString(),
                                "cron" to scanned.cronFavorites.size.toString(),
                                "color" to scanned.colorFavorites.size.toString(),
                                "history" to scanned.legacyHistory.size.toString()
                            )
                        ),
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
                if (
                    scanned.httpCollections.isNotEmpty() ||
                    scanned.hostProfiles.isNotEmpty() ||
                    scanned.translationWords.isNotEmpty() ||
                    scanned.translationHistory.isNotEmpty()
                ) {
                    Text(
                        container.t(
                            "settings.migration.sqliteP5",
                            mapOf(
                                "http" to scanned.httpCollections.size.toString(),
                                "host" to scanned.hostProfiles.size.toString(),
                                "words" to scanned.translationWords.size.toString(),
                                "transHistory" to scanned.translationHistory.size.toString()
                            )
                        ),
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
                scanned.warnings.forEach { warning ->
                    val message = when {
                        warning == "legacy:${LegacyJavaSettings.WARNING_DIFFERENT_VAULT_REMOTES}" ->
                            container.t("settings.migration.warning.differentVaultRemotes")
                        warning == "legacy:${LegacyJavaSettings.WARNING_SECRETS_SKIPPED}" ||
                            warning == "electron:${ElectronNextSettingsImport.WARNING_ENCRYPTED_SECRETS_SKIPPED}" ->
                            container.t("settings.migration.warning.secretsSkipped")
                        warning == "electron-migration-run" ->
                            container.t("settings.migration.electronDataSkipped")
                        warning.startsWith("electron-migration-row:") -> warning
                        else -> warning
                    }
                    Text(message, color = colors.warning, fontSize = 11.sp)
                }
                if (!alreadyMigrated && (total > 0 || (scanned.electronMigrationRunComplete && canImportSettings))) {
                    MooButton(
                        container.t("settings.migration.import"),
                        prominent = true,
                        enabled = !migrating,
                        onClick = { confirmOpen = true }
                    )
                }
            }
        }
    }

    if (confirmOpen && preview != null) {
        val scanned = preview!!
        MooOverlay(onDismiss = { if (!migrating) confirmOpen = false }) {
            Column(Modifier.width(440.dp).mooDialogSurface().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(container.t("settings.migration.confirmTitle"), color = colors.textPrimary, fontSize = 14.sp)
                Text(
                    container.t("settings.migration.confirmBody", mapOf("count" to scanned.totalItems().toString())),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.cancel"), onClick = { confirmOpen = false }, enabled = !migrating)
                    MooButton(
                        if (migrating) container.t("settings.migration.importing") else container.t("settings.migration.import"),
                        prominent = true,
                        enabled = !migrating,
                        onClick = {
                            scope.launch {
                                migrating = true
                                importError = ""
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val backup = container.exportBackup(
                                            Files.createTempDirectory("mootool-compose-pre-import").resolve("backup.zip")
                                        )
                                        val applied = CrossProductImporter.apply(
                                            scanned,
                                            container.noteVault(),
                                            container.jsonVault,
                                            container.importedFingerprints(),
                                            container.regexFavorites,
                                            container.cronFavorites,
                                            container.colorFavorites,
                                            container.history,
                                            container.httpCollections,
                                            container.hostProfiles,
                                            container.translations,
                                            container.migrationRows
                                        )
                                        container.updateSettings { current ->
                                            var next = current
                                            scanned.electronSettings?.let { electron ->
                                                val retainSecrets = scanned.electronStorePath
                                                    ?.let(ElectronNextSettingsImport::shouldRetainPlaintextSecrets)
                                                    ?: false
                                                next = ElectronNextSettingsImport.mergeInto(next, electron, retainSecrets)
                                            }
                                            if (LegacyJavaSettings.hasMigratableSettings(scanned.legacyJavaConfig)) {
                                                next = LegacyJavaSettings.applyPatch(next, scanned.legacyJavaConfig)
                                            }
                                            if (scanned.legacyJavaConfig.isNotEmpty()) {
                                                next = LegacyJavaSettings.applyVaultRootsAfterRelativeJavaConfig(
                                                    next,
                                                    scanned.legacyJavaConfig,
                                                    container.noteVault().root(),
                                                    container.jsonVault.root(),
                                                    applied.importedNotes,
                                                    applied.importedJson,
                                                )
                                            }
                                            next.copy(general = next.general.copy(legacyMigrationHintDismissed = true))
                                        }
                                        scanned.electronStorePath?.let { container.mergeElectronCodeRunFromStore(it) }
                                        if (scanned.legacyJavaConfig.isNotEmpty()) {
                                            LegacyJavaSettings.applySessionPatches(
                                                container.sessionManager,
                                                scanned.legacyJavaConfig,
                                                container.hostProfiles
                                            )
                                        }
                                        val pendingDrafts = scanned.legacyHistory.filter { row ->
                                            row.operation == "draft" &&
                                                row.dedupeKey.isNotBlank() &&
                                                row.dedupeKey !in container.appliedLegacyDraftKeys() &&
                                                !container.migrationRows.wasMigrated(scanned.sourceRoot, row.dedupeKey)
                                        }
                                        if (pendingDrafts.isNotEmpty()) {
                                            container.applyLegacyToolDrafts(pendingDrafts)
                                            container.rememberLegacyDraftKeys(pendingDrafts.map { it.dedupeKey })
                                            pendingDrafts.forEach { row ->
                                                container.migrationRows.record(scanned.sourceRoot, row.dedupeKey)
                                            }
                                        }
                                        container.rememberImport(
                                            scanned.fingerprint,
                                            scanned.legacyElectronFingerprint.orEmpty()
                                        )
                                        Triple(applied, backup, scanned)
                                    }
                                }
                                migrating = false
                                confirmOpen = false
                                result.onSuccess { (applied, backup, previewSnapshot) ->
                                    container.reloadToolSessionsFromStore()
                                    importNotice = container.t(
                                        "settings.import.done",
                                        mapOf(
                                            "kind" to previewSnapshot.sourceKind,
                                            "notes" to applied.importedNotes.toString(),
                                            "json" to applied.importedJson.toString(),
                                            "groups" to applied.importedGroups.toString(),
                                            "regex" to applied.importedRegex.toString(),
                                            "cron" to applied.importedCron.toString(),
                                            "color" to applied.importedColor.toString(),
                                            "history" to applied.importedHistory.toString(),
                                            "http" to applied.importedHttp.toString(),
                                            "host" to applied.importedHosts.toString(),
                                            "words" to applied.importedTranslationWords.toString(),
                                            "transHistory" to applied.importedTranslationHistory.toString(),
                                            "skipped" to applied.skipped.toString(),
                                            "backup" to backup.zipPath.fileName.toString()
                                        )
                                    )
                                    container.toastSuccess(importNotice)
                                    preview = CrossProductImporter.inspect(Path.of(sourceDirectory.trim()))
                                }.onFailure {
                                    importError = it.message ?: container.t("settings.import.failed")
                                    container.toastError(importError)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
