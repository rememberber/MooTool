package com.rememberber.mootool.next.compose.features.variables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.EnvDisplayScope
import com.rememberber.mootool.next.compose.domain.EnvEngine
import com.rememberber.mootool.next.compose.domain.EnvWiringPresentation
import com.rememberber.mootool.next.compose.domain.EnvEntry
import com.rememberber.mootool.next.compose.domain.EnvException
import com.rememberber.mootool.next.compose.domain.EnvPersistScope
import com.rememberber.mootool.next.compose.domain.EnvStoreConfig
import com.rememberber.mootool.next.compose.domain.EnvTab
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.VariablesSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooCompactSearch
import com.rememberber.mootool.next.compose.ui.components.MooGhostButton
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.mooEnvStatusFooter
import com.rememberber.mootool.next.compose.ui.components.mooEnvTableHead
import com.rememberber.mootool.next.compose.ui.components.mooEnvVarRow
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun VariablesScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.variablesSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Variables) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    val config = remember(settings.data.directory) { EnvStoreConfig.production(container.dataDirectories()) }

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistVariables()
    }

    fun refresh() {
        if (session.loading) return
        session.loading = true
        session.error = ""
        persist()
        scope.launch(Dispatchers.IO) {
            val result = when (val outcome = EnvWiringPresentation.runSnapshot(config)) {
                is EnvWiringPresentation.SnapshotOutcome.Success -> Result.success(outcome.snapshot)
                is EnvWiringPresentation.SnapshotOutcome.Failure -> Result.failure(outcome.error)
            }
            withContext(Dispatchers.Main) {
                session.loading = false
                result.onSuccess {
                    session.snapshot = it
                    session.error = ""
                }.onFailure {
                    session.error = messageFor(container, it)
                }
                persist()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session.snapshot == null && !session.loading) refresh()
    }
    LaunchedEffect(settings.data.directory, sessionGeneration) {
        if (!session.loading) refresh()
    }

    val snapshot = session.snapshot
    val source = when {
        session.tab == EnvTab.Runtime -> snapshot?.runtime.orEmpty()
        session.scope == EnvDisplayScope.User -> snapshot?.user.orEmpty()
        session.scope == EnvDisplayScope.System -> snapshot?.system.orEmpty()
        else -> snapshot?.process.orEmpty()
    }
    val query = session.query.trim()
    val entries = if (query.isEmpty()) source else source.filter {
        it.key.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true)
    }
    val canEdit = session.tab == EnvTab.Environment
    val canDelete = canEdit && session.scope != EnvDisplayScope.Process
    var scopeMenuOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("variables.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            if (canEdit) {
                MooButton(container.t("variables.add"), onClick = {
                    session.editorExisting = false
                    session.editorKey = ""
                    session.editorValue = ""
                    session.targetScope = if (session.scope == EnvDisplayScope.System) EnvPersistScope.System else EnvPersistScope.User
                    session.editorOpen = true
                    persist()
                }, enabled = EnvWiringPresentation.addVariableEnabled(canEdit, session.saving), p5Toolbar = true)
            }
            MooButton(
                container.t("common.refresh"),
                onClick = { refresh() },
                enabled = EnvWiringPresentation.refreshEnabled(session.loading, session.saving),
                p5Toolbar = true
            )
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.export"), enabled = EnvWiringPresentation.exportEnabled(snapshot != null)) {
                        val current = session.snapshot ?: return@OverflowAction
                        val file = chooseSave(container.t("common.export"), "mootool-next-compose-environment.txt") ?: return@OverflowAction
                        runCatching { file.writeText(EnvEngine.formatExport(current)) }
                            .onSuccess {
                                session.notice = container.t("variables.exported")
                                container.toastSuccess(container.t("variables.exported"))
                            }
                            .onFailure { session.error = it.message ?: container.t("variables.error.generic") }
                        persist()
                    })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Variables) })
                }
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .mooToolShell(p5 = true, endBorder = false)
        ) {
        Row(
            Modifier.fillMaxWidth().mooToolbarBackground().padding(start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnvTab.entries.forEach { tab ->
                MooToolTab(
                    container.t(if (tab == EnvTab.Environment) "variables.tab.environment" else "variables.tab.runtime"),
                    selected = session.tab == tab,
                    onClick = { session.tab = tab; persist() }
                )
            }
            if (session.tab == EnvTab.Environment) {
                Text(container.t("variables.scope"), color = colors.textMuted, fontSize = 10.sp)
                Box {
                    MooButton(
                        container.t(scopeKey(session.scope)),
                        onClick = { scopeMenuOpen = true },
                        modifier = Modifier.widthIn(min = 130.dp),
                        dense = true
                    )
                    MooMenu(expanded = scopeMenuOpen, onDismissRequest = { scopeMenuOpen = false }) {
                        EnvDisplayScope.entries.forEach { item ->
                            MooMenuItem(onClick = {
                                scopeMenuOpen = false
                                session.scope = item
                                persist()
                            }) {
                                Text(container.t(scopeKey(item)), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            MooCompactSearch(
                session.query,
                { session.query = it; persist() },
                modifier = Modifier.widthIn(min = 140.dp, max = 260.dp),
                placeholder = container.t("common.search")
            )
        }
        if (session.error.isNotEmpty()) {
            Text(session.error, color = colors.danger, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp)
        } else if (session.notice.isNotEmpty()) {
            Text(session.notice, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        when {
            session.loading && snapshot == null -> Text(container.t("variables.loading"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            entries.isEmpty() -> Text(container.t("variables.empty"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            else -> Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().mooEnvTableHead().padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        container.t("variables.key"),
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.34f)
                    )
                    Text(
                        container.t("variables.value"),
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(116.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
                LazyColumn(Modifier.weight(1f)) {
                    items(entries, key = { it.key }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().mooEnvVarRow()
                                .mooFocusClickable(enabled = canEdit) {
                                    openEditor(session, entry)
                                    persist()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                entry.key,
                                color = colors.textBody,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(0.34f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                entry.value,
                                color = colors.textBody,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(Modifier.width(116.dp), horizontalArrangement = Arrangement.End) {
                                MooGhostButton(container.t("common.action.copy"), onClick = {
                                    if (container.copyText("${entry.key}=${entry.value}")) {
                                        session.notice = container.t("json.notice.copied")
                                    } else {
                                        session.notice = container.t("json.notice.copyFailed")
                                    }
                                    persist()
                                }, size = 28.dp) {
                                    Text("⎘", color = colors.textMuted, fontSize = 13.sp)
                                }
                                if (canDelete) {
                                    MooGhostButton(container.t("common.delete"), onClick = {
                                        if (!EnvWiringPresentation.deleteRowEnabled(canDelete = true, saving = session.saving)) {
                                            return@MooGhostButton
                                        }
                                        session.deleteKey = entry.key
                                        persist()
                                    }, size = 28.dp) {
                                        Text("×", color = colors.textMuted, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
                    }
                }
            }
        }
        }
        Row(
            Modifier.fillMaxWidth().mooEnvStatusFooter().mooToolbarBackground().padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                container.t("variables.count", mapOf("count" to entries.size.toString(), "total" to source.size.toString())),
                color = colors.textMuted,
                fontSize = 10.sp
            )
            val hint = when {
                session.tab == EnvTab.Environment && snapshot != null ->
                    container.t(scopeHintKey(session.scope)) + " · " + snapshot.userFile
                session.tab == EnvTab.Environment -> container.t(scopeHintKey(session.scope))
                else -> ""
            }
            Text(hint, color = colors.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
        }
        }
    }
    }
    if (session.editorOpen) {
        EditorDialog(container, session, snapshot, config, ::persist) { refresh() }
    }
    if (session.deleteKey.isNotEmpty()) {
        MooOverlay(onDismiss = { session.deleteKey = ""; persist() }) {
            Column(
                Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(container.t("variables.confirmDelete", mapOf("key" to session.deleteKey)), color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(
                        container.t("common.delete"),
                        danger = true,
                        enabled = EnvWiringPresentation.confirmDeleteEnabled(session.saving),
                        onClick = {
                        val persistScope = persistScope(session.scope) ?: return@MooButton
                        val key = session.deleteKey
                        session.deleteKey = ""
                        session.saving = true
                        persist()
                        scope.launch(Dispatchers.IO) {
                            val result = runCatching { EnvEngine.delete(config, persistScope, key) }
                            withContext(Dispatchers.Main) {
                                session.saving = false
                                result.onSuccess {
                                    session.snapshot = it.snapshot
                                    session.lastBackup = it.backupPath.orEmpty()
                                    session.lastDiff = it.diff
                                    session.notice = container.t("variables.deleted")
                                    container.toastSuccess(container.t("variables.deleted"))
                                    session.error = ""
                                }.onFailure {
                                    session.error = messageFor(container, it)
                                }
                                persist()
                            }
                        }
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.deleteKey = ""; persist() })
                }
            }
        }
    }
}

@Composable
private fun EditorDialog(
    container: AppContainer,
    session: VariablesSession,
    snapshot: com.rememberber.mootool.next.compose.domain.EnvSnapshot?,
    config: EnvStoreConfig,
    persist: () -> Unit,
    onSaved: () -> Unit
) {
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    val persistScope = if (session.scope == EnvDisplayScope.Process) session.targetScope else persistScope(session.scope) ?: session.targetScope
    val diff = snapshot?.let {
        EnvWiringPresentation.runPreviewDiff(
            it,
            persistScope,
            session.editorKey.trim().ifBlank { "(key)" },
            session.editorValue,
        )
    }.orEmpty()
    MooOverlay(onDismiss = { session.editorOpen = false; persist() }) {
        Column(
            Modifier.width(560.dp).mooDialogSurface().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(container.t(if (session.editorExisting) "variables.editTitle" else "variables.addTitle"), color = colors.textPrimary)
            Text(container.t("variables.key"), color = colors.textMuted, fontSize = 10.sp)
            MooTextField(
                session.editorKey,
                {
                    if (!session.editorExisting) {
                        session.onUserInput { session.editorKey = it }
                        persist()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                dense = true
            )
            Text(container.t("variables.value"), color = colors.textMuted, fontSize = 10.sp)
            MooTextField(
                session.editorValue,
                {
                    session.onUserInput { session.editorValue = it }
                    persist()
                },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
                singleLine = false,
                dense = true,
                mono = true
            )
            if (session.scope == EnvDisplayScope.Process) {
                Text(container.t("variables.targetScope"), color = colors.textSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(
                        container.t("variables.scope.user"),
                        primary = session.targetScope == EnvPersistScope.User,
                        onClick = { session.targetScope = EnvPersistScope.User; persist() }
                    )
                    MooButton(
                        container.t("variables.scope.system"),
                        primary = session.targetScope == EnvPersistScope.System,
                        onClick = { session.targetScope = EnvPersistScope.System; persist() }
                    )
                }
            }
            Text(container.t("variables.applyHint"), color = colors.textMuted, fontSize = 10.sp)
            if (diff.isNotBlank()) {
                Text(container.t("variables.diff"), color = colors.textSecondary, fontSize = 12.sp)
                Text(diff, color = colors.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.save"), prominent = true, onClick = {
                    val key = session.editorKey.trim()
                    if (key.isEmpty() || session.saving) return@MooButton
                    val target = persistScope
                    session.saving = true
                    persist()
                    scope.launch(Dispatchers.IO) {
                        val result = runCatching { EnvEngine.set(config, target, key, session.editorValue) }
                        withContext(Dispatchers.Main) {
                            session.saving = false
                            result.onSuccess {
                                session.snapshot = it.snapshot
                                session.lastBackup = it.backupPath.orEmpty()
                                session.lastDiff = it.diff
                                session.editorOpen = false
                                session.notice = container.t("variables.saved")
                                container.toastSuccess(container.t("variables.saved"))
                                session.error = ""
                            }.onFailure {
                                session.error = messageFor(container, it)
                            }
                            persist()
                            if (result.isSuccess) onSaved()
                        }
                    }
                }, enabled = EnvWiringPresentation.saveEditorEnabled(session.editorKey.trim(), session.saving))
                MooButton(container.t("common.cancel"), onClick = { session.editorOpen = false; persist() })
            }
        }
    }
}

private fun openEditor(session: VariablesSession, entry: EnvEntry) {
    session.editorExisting = true
    session.editorKey = entry.key
    session.editorValue = entry.value
    session.targetScope = persistScope(session.scope) ?: EnvPersistScope.User
    session.editorOpen = true
}

private fun persistScope(scope: EnvDisplayScope): EnvPersistScope? = when (scope) {
    EnvDisplayScope.User -> EnvPersistScope.User
    EnvDisplayScope.System -> EnvPersistScope.System
    EnvDisplayScope.Process -> null
}

private fun scopeKey(scope: EnvDisplayScope) = when (scope) {
    EnvDisplayScope.User -> "variables.scope.user"
    EnvDisplayScope.System -> "variables.scope.system"
    EnvDisplayScope.Process -> "variables.scope.process"
}

private fun scopeHintKey(scope: EnvDisplayScope) = when (scope) {
    EnvDisplayScope.User -> "variables.scopeHint.user"
    EnvDisplayScope.System -> "variables.scopeHint.system"
    EnvDisplayScope.Process -> "variables.scopeHint.process"
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? EnvException)?.code?.name
    return when (code) {
        "INVALID_NAME" -> container.t("variables.error.name")
        "INVALID_VALUE" -> container.t("variables.error.value")
        "PERMISSION" -> container.t("variables.error.permission") + (error.message?.let { "\n$it" } ?: "")
        else -> error.message ?: container.t("variables.error.generic")
    }
}

private fun chooseSave(title: String, defaultName: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    dialog.file = defaultName
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}
