package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun LegacyMigrationHintDialog(container: AppContainer) {
    val settings by container.settings.collectAsState()
    val showSettings by container.showSettings.collectAsState()
    val searchOpen by container.searchOpen.collectAsState()
    val groupManagerOpen by container.groupManagerOpen.collectAsState()
    if (!shouldShowLegacyMigrationHint(
            settings.general.legacyMigrationHintDismissed,
            showSettings,
            searchOpen,
            groupManagerOpen,
        )
    ) {
        return
    }
    val colors = MooTheme.colors

    fun dismiss() {
        container.updateSettings { current ->
            current.copy(general = current.general.copy(legacyMigrationHintDismissed = true))
        }
    }

    MooOverlay(onDismiss = { dismiss() }) {
        Column(
            Modifier.width(480.dp).mooDialogSurface().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(container.t("app.migrationHint.title"), color = colors.textPrimary, fontSize = 15.sp)
            Text(container.t("app.migrationHint.body"), color = colors.textSecondary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("app.migrationHint.dismiss"), onClick = { dismiss() })
                MooButton(
                    container.t("app.migrationHint.openSettings"),
                    prominent = true,
                    onClick = {
                        dismiss()
                        container.openSettings(categoryId = SettingsNavCategory.Data.storageId())
                    },
                )
            }
        }
    }
}
