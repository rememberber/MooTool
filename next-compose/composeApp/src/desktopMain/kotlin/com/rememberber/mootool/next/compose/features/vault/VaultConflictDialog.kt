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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DiffEngine
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun VaultConflictDialog(
    container: AppContainer,
    conflict: VaultConflictState,
    onReload: () -> Unit,
    onSaveCopy: () -> Unit,
    onKeep: () -> Unit
) {
    val colors = MooTheme.colors
    val preview = if (conflict.deleted) {
        container.t("vault.conflict.deleted")
    } else {
        DiffEngine.compare(conflict.editorText, conflict.diskText.orEmpty(), ignoreWhitespace = false)
            .unified
            .take(4_000)
            .ifBlank { container.t("vault.conflict.noDiff") }
    }
    MooOverlay(onDismiss = onKeep) {
        Column(
            Modifier.width(640.dp).height(420.dp)
                .background(colors.workspace, RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("vault.conflict.title"), color = colors.textPrimary, fontSize = 16.sp)
            Text(conflict.relativePath, color = colors.warning, fontSize = 12.sp)
            Text(container.t("vault.conflict.hint"), color = colors.textSecondary, fontSize = 12.sp)
            Text(
                preview,
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!conflict.deleted) {
                    MooButton(container.t("vault.conflict.reload"), primary = true, onClick = onReload)
                }
                MooButton(container.t("vault.conflict.saveCopy"), onClick = onSaveCopy)
                MooButton(container.t("vault.conflict.keep"), onClick = onKeep)
            }
        }
    }
}
