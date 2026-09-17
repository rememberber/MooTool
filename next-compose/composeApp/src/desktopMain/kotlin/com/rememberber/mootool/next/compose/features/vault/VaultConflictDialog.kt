package com.rememberber.mootool.next.compose.features.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.VaultConflictPresentation
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.mooVaultConflictActions
import com.rememberber.mootool.next.compose.ui.components.mooVaultConflictDiffPreview
import com.rememberber.mootool.next.compose.ui.components.mooVaultConflictHintRow
import com.rememberber.mootool.next.compose.ui.components.mooVaultConflictPathRow
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun VaultConflictDialog(
    container: AppContainer,
    conflict: VaultConflictState,
    onReload: () -> Unit,
    onSaveCopy: () -> Unit,
    onKeep: () -> Unit,
    reloadButtonModifier: Modifier = Modifier,
    saveCopyButtonModifier: Modifier = Modifier,
    keepButtonModifier: Modifier = Modifier,
) {
    val colors = MooTheme.colors
    val unified = if (conflict.deleted) {
        ""
    } else {
        VaultConflictPresentation.buildUnifiedPreview(conflict.editorText, conflict.diskText.orEmpty())
    }
    val preview = VaultConflictPresentation.previewText(
        deleted = conflict.deleted,
        deletedMessage = container.t("vault.conflict.deleted"),
        noDiffMessage = container.t("vault.conflict.noDiff"),
        unifiedDiff = unified,
    )
    MooOverlay(onDismiss = onKeep) {
        Column(
            Modifier.width(640.dp).height(420.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("vault.conflict.title"))
            Text(
                conflict.relativePath,
                color = colors.warning,
                fontSize = 12.sp,
                modifier = Modifier.mooVaultConflictPathRow(),
            )
            Text(
                container.t(VaultConflictPresentation.hintMessageKey(conflict.deleted)),
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.mooVaultConflictHintRow(),
            )
            val previewModifier = if (VaultConflictPresentation.showDiffPreview(conflict.deleted)) {
                Modifier.weight(1f).mooVaultConflictDiffPreview().verticalScroll(rememberScrollState())
            } else {
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            }
            Text(
                preview,
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = previewModifier,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.mooVaultConflictActions(),
            ) {
                if (VaultConflictPresentation.showReloadAction(conflict.deleted)) {
                    MooButton(
                        container.t("vault.conflict.reload"),
                        prominent = VaultConflictPresentation.reloadProminent(conflict.deleted),
                        onClick = onReload,
                        modifier = reloadButtonModifier.semantics {
                            contentDescription = container.t("vault.conflict.reload")
                        },
                    )
                }
                MooButton(
                    container.t("vault.conflict.saveCopy"),
                    prominent = VaultConflictPresentation.saveCopyProminent(conflict.deleted),
                    onClick = onSaveCopy,
                    modifier = saveCopyButtonModifier.semantics {
                        contentDescription = container.t("vault.conflict.saveCopy")
                    },
                )
                MooButton(
                    container.t("vault.conflict.keep"),
                    onClick = onKeep,
                    modifier = keepButtonModifier.semantics {
                        contentDescription = container.t("vault.conflict.keep")
                    },
                )
            }
        }
    }
}
