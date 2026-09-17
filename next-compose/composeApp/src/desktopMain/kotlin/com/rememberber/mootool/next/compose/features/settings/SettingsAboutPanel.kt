package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.InstallIdentity
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.app.UpdateUiState
import com.rememberber.mootool.next.compose.domain.UpdateAboutPresentation
import com.rememberber.mootool.next.compose.domain.UpdateInstallApplyPresentation
import com.rememberber.mootool.next.compose.domain.UpdateCheckResult
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooSettingsAboutHero
import com.rememberber.mootool.next.compose.ui.components.mooSettingsOpenInstallerButton
import com.rememberber.mootool.next.compose.ui.components.mooSettingsUpdateActionsRow
import com.rememberber.mootool.next.compose.ui.components.mooSettingsUpdateResult
import com.rememberber.mootool.next.compose.ui.components.mooSettingsUpdateResultFile
import com.rememberber.mootool.next.compose.ui.components.mooSettingsUpdateResultNotes
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun SettingsAboutPanel(
    container: AppContainer,
    settings: AppSettings,
    update: UpdateUiState,
    checkButtonModifier: Modifier = Modifier,
    downloadButtonModifier: Modifier = Modifier,
    openInstallerButtonModifier: Modifier = Modifier,
) {
    val colors = MooTheme.colors
    val result = update.result
    Column(Modifier.mooSettingsAboutHero(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    }
    Text(container.t("settings.update.current", mapOf("version" to ProductIdentity.VERSION)), color = colors.textSecondary)
    if (UpdateAboutPresentation.showUpdateResultSection(result != null) && result != null) {
        SettingsUpdateResultCard(
            container,
            update,
            result,
            settings,
            checkButtonModifier,
            downloadButtonModifier,
            openInstallerButtonModifier,
        )
    } else {
        SettingsUpdateActionsRow(container, settings, update, checkButtonModifier, downloadButtonModifier, openInstallerButtonModifier)
        Text(updateStatusLabel(container, update), color = if (update.error.isNotBlank()) colors.danger else colors.textPrimary)
        Text(container.t("settings.update.manualInstall"), color = colors.warning, fontSize = 12.sp)
    }
}

@Composable
fun SettingsUpdateResultCard(
    container: AppContainer,
    update: UpdateUiState,
    result: UpdateCheckResult,
    settings: AppSettings,
    checkButtonModifier: Modifier = Modifier,
    downloadButtonModifier: Modifier = Modifier,
    openInstallerButtonModifier: Modifier = Modifier,
) {
    val colors = MooTheme.colors
    val statusName = result.status.name.lowercase()
    Column(Modifier.mooSettingsUpdateResult(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val headlineKey = UpdateAboutPresentation.resultHeadlineKey(statusName)
        Text(
            if (headlineKey == "settings.update.available") {
                container.t(headlineKey, mapOf("version" to result.latestVersion))
            } else {
                container.t(headlineKey)
            },
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (UpdateAboutPresentation.showTargetPlatformLine(hasResult = true)) {
            Text(
                container.t(
                    "settings.update.target",
                    mapOf(
                        "product" to result.productName,
                        "platform" to result.platform,
                        "architecture" to result.architecture,
                    ),
                ),
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
        }
        if (UpdateAboutPresentation.showErrorLine(update.error, update.message)) {
            Text(
                UpdateAboutPresentation.errorDisplayText(update.error, update.message),
                color = colors.danger,
                fontSize = 12.sp,
            )
        } else {
            Text(updateStatusLabel(container, update), color = colors.textPrimary, fontSize = 12.sp)
        }
        if (UpdateAboutPresentation.showAvailableDetail(statusName)) {
            SettingsUpdateActionsRow(container, settings, update, checkButtonModifier, downloadButtonModifier, openInstallerButtonModifier)
            if (UpdateAboutPresentation.showDownloadFileLine(result.download != null, result.download?.fileName)) {
                Text(
                    result.download!!.fileName,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.mooSettingsUpdateResultFile(),
                )
            }
            if (UpdateAboutPresentation.showMissingDownloadLine(statusName, result.download != null)) {
                Text(container.t("settings.update.noDownload"), color = colors.textSecondary, fontSize = 12.sp)
            }
            if (UpdateAboutPresentation.showReleaseNotes(result.releaseNotes, result.latestVersion, statusName)) {
                Text(container.t("settings.update.notes"), color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    result.releaseNotes,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.mooSettingsUpdateResultNotes().verticalScroll(rememberScrollState()),
                )
            }
        } else if (result.latestVersion.isNotBlank() && statusName != "unpublished") {
            Text(container.t("settings.update.latest", mapOf("version" to result.latestVersion)), color = colors.textSecondary, fontSize = 12.sp)
            SettingsUpdateActionsRow(container, settings, update, checkButtonModifier, downloadButtonModifier, openInstallerButtonModifier)
        } else {
            SettingsUpdateActionsRow(container, settings, update, checkButtonModifier, downloadButtonModifier, openInstallerButtonModifier)
        }
        Text(container.t("settings.update.manualInstall"), color = colors.warning, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsUpdateActionsRow(
    container: AppContainer,
    settings: AppSettings,
    update: UpdateUiState,
    checkButtonModifier: Modifier = Modifier,
    downloadButtonModifier: Modifier = Modifier,
    openInstallerButtonModifier: Modifier = Modifier,
) {
    val result = update.result
    val progress = update.progress
    val colors = MooTheme.colors
    if (progress != null && UpdateAboutPresentation.showDownloadProgress(update.status, hasProgress = true)) {
        Text(
            container.t(
                "settings.update.progress",
                mapOf(
                    "percent" to progress.percent.toString(),
                    "transferred" to progress.transferred.toString(),
                    "total" to progress.total.toString(),
                ),
            ),
            color = colors.textSecondary,
            fontSize = 12.sp,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.mooSettingsUpdateActionsRow()) {
        MooButton(
            container.t("settings.update.check"),
            enabled = UpdateAboutPresentation.canCheckForUpdates(update.busy),
            onClick = { container.updates.check(autoDownload = settings.general.autoDownloadUpdates) },
            modifier = checkButtonModifier,
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
                onClick = { container.updates.download() },
                modifier = downloadButtonModifier,
            )
        }
        if (UpdateAboutPresentation.showCancelDownloadAction(update.status)) {
            MooButton(container.t("settings.update.cancel"), onClick = { container.updates.cancel() })
        }
        if (UpdateAboutPresentation.showOpenInstallerAction(update.downloaded != null)) {
            MooButton(
                container.t("settings.update.openInstaller"),
                prominent = true,
                enabled = UpdateInstallApplyPresentation.canOpenInstaller(
                    update.busy,
                    installerReady = update.downloaded != null,
                ),
                onClick = { container.updates.openInstaller() },
                modifier = openInstallerButtonModifier.mooSettingsOpenInstallerButton(),
            )
        }
        MooButton(container.t("settings.update.openRelease"), onClick = { container.updates.openReleasePage() })
    }
}

internal fun updateStatusLabel(container: AppContainer, state: UpdateUiState): String {
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
