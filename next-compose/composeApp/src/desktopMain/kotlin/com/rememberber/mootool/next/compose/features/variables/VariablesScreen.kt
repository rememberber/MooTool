package com.rememberber.mootool.next.compose.features.variables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.EnvDisplayScope
import com.rememberber.mootool.next.compose.domain.EnvEngine
import com.rememberber.mootool.next.compose.domain.EnvEntry
import com.rememberber.mootool.next.compose.domain.EnvException
import com.rememberber.mootool.next.compose.domain.EnvPersistScope
import com.rememberber.mootool.next.compose.domain.EnvStoreConfig
import com.rememberber.mootool.next.compose.domain.EnvTab
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.VariablesSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@Composable
fun VariablesScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.variablesSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    val config = remember { EnvStoreConfig.production(container.directories) }

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
            val result = runCatching { EnvEngine.snapshot(config) }
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

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("variables.title"), color = colors.textPrimary, fontSize = 16.sp)
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
                }, enabled = !session.saving)
            }
            MooButton(container.t("common.refresh"), onClick = { refresh() }, enabled = !session.loading && !session.saving)
            MooButton(container.t("common.export"), onClick = {
                val current = session.snapshot ?: return@MooButton
                val file = chooseSave(container.t("common.export"), "mootool-next-compose-environment.txt") ?: return@MooButton
                runCatching { file.writeText(EnvEngine.formatExport(current)) }
                    .onSuccess { session.notice = container.t("variables.exported") }
                    .onFailure { session.error = it.message ?: container.t("variables.error.generic") }
                persist()
            }, enabled = snapshot != null)
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Variables) })
            }
        }
        Row(
            Modifier.fillMaxWidth().background(colors.toolbar).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnvTab.entries.forEach { tab ->
                MooButton(
                    container.t(if (tab == EnvTab.Environment) "variables.tab.environment" else "variables.tab.runtime"),
                    primary = session.tab == tab,
                    onClick = { session.tab = tab; persist() }
                )
            }
            if (session.tab == EnvTab.Environment) {
                EnvDisplayScope.entries.forEach { item ->
                    MooButton(
                        container.t(scopeKey(item)),
                        primary = session.scope == item,
                        onClick = { session.scope = item; persist() }
                    )
                }
            }
            MooTextField(
                session.query,
                { session.query = it; persist() },
                modifier = Modifier.weight(1f),
                placeholder = container.t("common.search")
            )
        }
        if (session.error.isNotEmpty()) {
            Text(session.error, color = colors.danger, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp)
        } else if (session.notice.isNotEmpty()) {
            Text(session.notice, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                container.t("variables.count", mapOf("count" to entries.size.toString(), "total" to source.size.toString())),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            if (session.tab == EnvTab.Environment) {
                Text(container.t(scopeHintKey(session.scope)), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
            }
        }
        if (session.tab == EnvTab.Environment && snapshot != null) {
            Text(
                container.t(
                    "variables.pathHint",
                    mapOf(
                        "user" to snapshot.userFile,
                        "system" to snapshot.systemFile,
                        "profile" to snapshot.shellProfile
                    )
                ),
                color = colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        when {
            session.loading && snapshot == null -> Text(container.t("variables.loading"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            entries.isEmpty() -> Text(container.t("variables.empty"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            else -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(container.t("variables.key"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(220.dp))
                    Text(container.t("variables.value"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
                LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    items(entries, key = { it.key }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = canEdit) {
                                openEditor(session, entry)
                                persist()
                            }.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(entry.key, color = colors.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.width(220.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.value, color = colors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            MooButton(container.t("common.action.copy"), onClick = {
                                runCatching {
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection("${entry.key}=${entry.value}"), null)
                                    session.notice = container.t("json.notice.copied")
                                    persist()
                                }
                            })
                            if (canEdit) {
                                MooButton(container.t("variables.edit"), onClick = { openEditor(session, entry); persist() })
                            }
                            if (canDelete) {
                                MooButton(container.t("common.delete"), onClick = {
                                    session.deleteKey = entry.key
                                    persist()
                                })
                            }
                        }
                    }
                }
            }
        }
    }
    if (session.editorOpen) {
        EditorDialog(container, session, snapshot, config, ::persist) { refresh() }
    }
    if (session.deleteKey.isNotEmpty()) {
        Dialog(onDismissRequest = { session.deleteKey = ""; persist() }) {
            Column(
                Modifier.width(420.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(container.t("variables.confirmDelete", mapOf("key" to session.deleteKey)), color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.delete"), primary = true, onClick = {
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
    val diff = snapshot?.let { EnvEngine.previewDiff(it, persistScope, session.editorKey.trim().ifBlank { "(key)" }, session.editorValue) }.orEmpty()
    Dialog(onDismissRequest = { session.editorOpen = false; persist() }) {
        Column(
            Modifier.width(560.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(container.t(if (session.editorExisting) "variables.editTitle" else "variables.addTitle"), color = colors.textPrimary)
            Text(container.t("variables.key"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.editorKey, { if (!session.editorExisting) { session.editorKey = it; persist() } }, modifier = Modifier.fillMaxWidth())
            Text(container.t("variables.value"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.editorValue, { session.editorValue = it; persist() }, modifier = Modifier.fillMaxWidth(), singleLine = false)
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
            Text(container.t("variables.applyHint"), color = colors.textSecondary, fontSize = 12.sp)
            if (diff.isNotBlank()) {
                Text(container.t("variables.diff"), color = colors.textSecondary, fontSize = 12.sp)
                Text(diff, color = colors.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.save"), primary = true, onClick = {
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
                                session.error = ""
                            }.onFailure {
                                session.error = messageFor(container, it)
                            }
                            persist()
                            if (result.isSuccess) onSaved()
                        }
                    }
                }, enabled = session.editorKey.trim().isNotEmpty() && !session.saving)
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
