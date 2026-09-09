package com.rememberber.mootool.next.compose.features.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.HostApplyConfig
import com.rememberber.mootool.next.compose.domain.HostEngine
import com.rememberber.mootool.next.compose.domain.HostErrorCode
import com.rememberber.mootool.next.compose.domain.HostException
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.HostSession
import com.rememberber.mootool.next.compose.storage.HostProfile
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HostScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.hostSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    val config = remember { HostApplyConfig.production(container.directories) }
    var profiles by remember { mutableStateOf(emptyList<HostProfile>()) }
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistHost()
    }

    fun reloadProfiles() {
        profiles = container.hostProfiles.list(session.query, session.includeContent)
    }

    fun saveCurrent(showNotice: Boolean = true): Boolean {
        val nextName = session.name.trim()
        if (nextName.isEmpty()) {
            session.error = container.t("host.error.name")
            persist()
            return false
        }
        return runCatching {
            val saved = container.hostProfiles.save(session.selectedId.ifBlank { null }, nextName, session.content)
            session.markSaved(saved.id, saved.name, saved.content)
            if (showNotice) session.notice = container.t("common.save")
            session.error = ""
            reloadProfiles()
            persist()
            true
        }.getOrElse {
            session.error = it.message ?: container.t("host.error.generic")
            persist()
            false
        }
    }

    fun selectProfile(profile: HostProfile) {
        if (session.dirty && session.name.isNotBlank() && !saveCurrent(showNotice = false)) return
        val loaded = container.hostProfiles.get(profile.id) ?: profile
        session.markSaved(loaded.id, loaded.name, loaded.content)
        session.error = ""
        persist()
    }

    LaunchedEffect(session.query, session.includeContent, revision) { reloadProfiles() }
    LaunchedEffect(Unit) {
        if (session.selectedId.isBlank() && session.name.isBlank() && session.content.isBlank()) {
            val first = container.hostProfiles.list().firstOrNull()
            if (first != null) session.markSaved(first.id, first.name, first.content)
        } else if (session.selectedId.isNotBlank()) {
            container.hostProfiles.get(session.selectedId)?.let { loaded ->
                if (!session.dirty) session.markSaved(loaded.id, loaded.name, loaded.content)
            }
        }
        persist()
    }
    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Host.id)
    }

    Column(
        Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val meta = event.isMetaPressed || event.isCtrlPressed
            if (meta && (event.key == Key.F || event.key == Key.R)) {
                session.findOpen = true
                persist()
                true
            } else false
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("host.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            if (session.error.isNotEmpty()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            else if (session.notice.isNotEmpty()) Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; persist() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Host) })
            }
        }
        Row(Modifier.fillMaxSize()) {
            ProfileList(
                container = container,
                session = session,
                profiles = profiles,
                onSelect = ::selectProfile,
                onNew = {
                    if (session.dirty && session.name.isNotBlank() && !saveCurrent(showNotice = false)) return@ProfileList
                    session.selectedId = ""
                    session.name = container.t("host.untitled")
                    session.content = HostEngine.DEFAULT_TEMPLATE
                    session.savedName = ""
                    session.savedContent = ""
                    persist()
                },
                onCopy = {
                    if (session.selectedId.isBlank() && !saveCurrent(showNotice = false)) return@ProfileList
                    val sourceId = session.selectedId
                    if (sourceId.isBlank()) return@ProfileList
                    val copy = container.hostProfiles.duplicate(sourceId, container.t("host.copySuffix"))
                    session.markSaved(copy.id, copy.name, copy.content)
                    reloadProfiles()
                    persist()
                },
                onImport = {
                    if (session.dirty && session.name.isNotBlank() && !saveCurrent(showNotice = false)) return@ProfileList
                    val file = pickHostFile(false) ?: return@ProfileList
                    session.selectedId = ""
                    session.name = file.nameWithoutExtension.ifBlank { container.t("host.untitled") }
                    session.content = file.readText()
                    session.savedName = ""
                    session.savedContent = ""
                    persist()
                },
                onExport = {
                    val file = pickHostFile(true, "${session.name.ifBlank { "hosts" }}.txt") ?: return@ProfileList
                    file.writeText(session.content)
                    session.notice = container.t("host.exported")
                    persist()
                },
                onDelete = { if (session.selectedId.isNotBlank()) { session.deleteConfirm = true; persist() } },
                onChanged = { persist(); reloadProfiles() }
            )
            Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MooTextField(
                        session.name,
                        { session.name = it; persist() },
                        modifier = Modifier.weight(1f),
                        placeholder = container.t("host.profileName")
                    )
                    MooButton(container.t("host.current"), onClick = {
                        scope.launch(Dispatchers.IO) {
                            val result = runCatching { HostEngine.readSystem(config) }
                            withContext(Dispatchers.Main) {
                                result.onSuccess {
                                    session.systemPath = it.path
                                    session.systemContent = it.content
                                    session.systemWritable = it.writable
                                    session.systemFingerprint = it.fingerprint
                                    session.systemOpen = true
                                    session.error = ""
                                }.onFailure { session.error = messageFor(container, it) }
                                persist()
                            }
                        }
                    })
                    MooButton(container.t("host.find"), onClick = { session.findOpen = !session.findOpen; persist() })
                    MooButton(container.t("common.save"), onClick = { saveCurrent() }, enabled = session.dirty)
                    MooButton(
                        if (session.applying) container.t("host.applying") else container.t("host.apply"),
                        primary = true,
                        enabled = session.content.isNotBlank() && !session.applying,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val preview = runCatching {
                                    val current = HostEngine.readSystem(config)
                                    val diff = HostEngine.previewDiff(current.content, session.content, current.path)
                                    Triple(current, HostEngine.validate(session.content), diff)
                                }
                                withContext(Dispatchers.Main) {
                                    preview.onSuccess { (current, _, diff) ->
                                        session.systemPath = current.path
                                        session.systemContent = current.content
                                        session.systemWritable = current.writable
                                        session.systemFingerprint = current.fingerprint
                                        session.applyDiff = diff
                                        session.applyConfirm = true
                                        session.error = ""
                                    }.onFailure { session.error = messageFor(container, it) }
                                    persist()
                                }
                            }
                        }
                    )
                    MooButton(
                        container.t("host.restore"),
                        enabled = session.lastBackup.isNotBlank() && !session.applying,
                        onClick = {
                            val backup = session.lastBackup
                            session.applying = true
                            persist()
                            scope.launch(Dispatchers.IO) {
                                val result = runCatching {
                                    val current = HostEngine.readSystem(config)
                                    HostEngine.restore(config, backup, current.fingerprint)
                                }
                                withContext(Dispatchers.Main) {
                                    session.applying = false
                                    result.onSuccess {
                                        session.systemPath = it.system.path
                                        session.systemContent = it.system.content
                                        session.systemWritable = it.system.writable
                                        session.systemFingerprint = it.system.fingerprint
                                        session.applyDiff = it.diff
                                        session.notice = container.t("host.restored")
                                        session.error = ""
                                    }.onFailure { session.error = messageFor(container, it) }
                                    persist()
                                }
                            }
                        }
                    )
                }
                if (session.findOpen) FindBar(container, session) { persist() }
                MooTextField(
                    session.content,
                    { session.content = it; persist() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    placeholder = container.t("host.placeholder"),
                    singleLine = false
                )
                Text(container.t("host.saveHint"), color = colors.textSecondary, fontSize = 11.sp)
            }
        }
    }
    if (session.systemOpen) {
        Dialog(onDismissRequest = { session.systemOpen = false; persist() }) {
            Column(
                Modifier.width(720.dp).height(520.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("host.current"), color = colors.textPrimary)
                Text(session.systemPath, color = colors.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(
                    if (session.systemWritable) container.t("host.writable") else container.t("host.requiresPrivilege"),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                SelectionContainer(Modifier.weight(1f).fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(8.dp).verticalScroll(rememberScrollState())) {
                    Text(session.systemContent, color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.action.copy"), onClick = {
                        runCatching {
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(session.systemContent), null)
                            session.notice = container.t("common.copied")
                            persist()
                        }
                    })
                    MooButton(container.t("common.close"), onClick = { session.systemOpen = false; persist() })
                }
            }
        }
    }
    if (session.applyConfirm) {
        Dialog(onDismissRequest = { session.applyConfirm = false; persist() }) {
            Column(
                Modifier.width(640.dp).height(480.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("host.confirmApply"), color = colors.textPrimary)
                Text(session.systemPath, color = colors.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(container.t("host.diff"), color = colors.textSecondary, fontSize = 12.sp)
                SelectionContainer(Modifier.weight(1f).fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(8.dp).verticalScroll(rememberScrollState())) {
                    Text(session.applyDiff, color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("host.apply"), primary = true, enabled = !session.applying, onClick = {
                        session.applying = true
                        persist()
                        scope.launch(Dispatchers.IO) {
                            val result = runCatching {
                                HostEngine.apply(config, session.content, session.systemFingerprint)
                            }
                            withContext(Dispatchers.Main) {
                                session.applying = false
                                session.applyConfirm = false
                                result.onSuccess {
                                    session.systemPath = it.system.path
                                    session.systemContent = it.system.content
                                    session.systemWritable = it.system.writable
                                    session.systemFingerprint = it.system.fingerprint
                                    session.lastBackup = it.backupPath.orEmpty()
                                    session.applyDiff = it.diff
                                    session.notice = if (it.dnsFlushed) container.t("host.applied") else container.t("host.appliedNoDns")
                                    session.error = ""
                                    container.history.save(
                                        ToolId.Host.id,
                                        "apply",
                                        session.name.ifBlank { it.system.path },
                                        session.content.take(4_000),
                                        it.system.path,
                                        it.backupPath.orEmpty()
                                    )
                                }.onFailure { session.error = messageFor(container, it) }
                                persist()
                            }
                        }
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.applyConfirm = false; persist() })
                }
            }
        }
    }
    if (session.deleteConfirm) {
        Dialog(onDismissRequest = { session.deleteConfirm = false; persist() }) {
            Column(
                Modifier.width(420.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(container.t("host.confirmDelete"), color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.delete"), primary = true, onClick = {
                        val id = session.selectedId
                        if (id.isNotBlank()) container.hostProfiles.delete(id)
                        session.selectedId = ""
                        session.name = ""
                        session.content = ""
                        session.savedName = ""
                        session.savedContent = ""
                        session.deleteConfirm = false
                        reloadProfiles()
                        persist()
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.deleteConfirm = false; persist() })
                }
            }
        }
    }
    if (session.historyOpen) {
        HistoryDialog(container, session, historyItems) { persist() }
    }
}

@Composable
private fun ProfileList(
    container: AppContainer,
    session: HostSession,
    profiles: List<HostProfile>,
    onSelect: (HostProfile) -> Unit,
    onNew: () -> Unit,
    onCopy: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    val stamp = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()) }
    Column(
        Modifier.width(240.dp).fillMaxHeight().background(colors.sidebar).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(session.query, { session.query = it; onChanged() }, placeholder = container.t("common.search"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.clip(RoundedCornerShape(4.dp)).background(if (session.includeContent) colors.accent else colors.workspace)
                    .clickable { session.includeContent = !session.includeContent; onChanged() }.padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    container.t("host.searchContent"),
                    color = if (session.includeContent) colors.onAccent else colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.new"), onClick = onNew)
        }
        if (profiles.isEmpty()) {
            Text(container.t("host.empty"), color = colors.textSecondary, fontSize = 12.sp)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(profiles, key = { it.id }) { profile ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (profile.id == session.selectedId) colors.selected else colors.sidebar)
                            .clickable { onSelect(profile) }.padding(8.dp)
                    ) {
                        Text(profile.name, color = colors.textPrimary, fontSize = 13.sp)
                        Text(stamp.format(Instant.ofEpochMilli(profile.modifiedAt)), color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("host.import"), onClick = onImport)
            MooButton(container.t("host.export"), onClick = onExport, enabled = session.content.isNotBlank())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("host.copy"), onClick = onCopy, enabled = session.selectedId.isNotBlank() || session.content.isNotBlank())
            MooButton(container.t("common.delete"), onClick = onDelete, enabled = session.selectedId.isNotBlank())
        }
    }
}

@Composable
private fun FindBar(container: AppContainer, session: HostSession, onChanged: () -> Unit) {
    val matches = FindReplace.findAll(session.content, session.findQuery, session.findOptions)
    Row(
        modifier = Modifier.fillMaxWidth().background(MooTheme.colors.surfaceSubtle).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(session.findQuery, { session.findQuery = it; onChanged() }, modifier = Modifier.width(180.dp), placeholder = container.t("host.findPlaceholder"))
        MooTextField(session.replaceText, { session.replaceText = it; onChanged() }, modifier = Modifier.width(140.dp), placeholder = container.t("host.replacePlaceholder"))
        Text(container.t("json.find.matches", mapOf("count" to matches.size.toString())), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
        MooButton(container.t("find.replace"), onClick = {
            val (next, _) = FindReplace.replaceCurrent(session.content, session.findQuery, session.replaceText, session.findOptions, 0)
            session.content = next
            onChanged()
        })
        MooButton(container.t("find.replaceAll"), onClick = {
            val (next, count) = FindReplace.replaceAll(session.content, session.findQuery, session.replaceText, session.findOptions)
            session.content = next
            session.notice = container.t("json.find.matches", mapOf("count" to count.toString()))
            onChanged()
        })
        MooButton(container.t("common.close"), onClick = { session.findOpen = false; onChanged() })
    }
}

@Composable
private fun HistoryDialog(container: AppContainer, session: HostSession, items: List<HistoryRecord>, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.historyOpen = false; onChanged() }) {
        Column(
            Modifier.width(480.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("common.action.history"), color = MooTheme.colors.textPrimary)
            if (items.isEmpty()) {
                Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            session.content = item.input
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.createdAt, color = MooTheme.colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Host.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

private fun pickHostFile(save: Boolean, defaultName: String = "hosts.txt"): File? {
    val dialog = FileDialog(null as Frame?, if (save) "Export Host" else "Import Host", if (save) FileDialog.SAVE else FileDialog.LOAD)
    if (save) dialog.file = defaultName
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val directory = dialog.directory ?: return null
    return File(directory, file)
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? HostException)?.code
    val key = when (code) {
        HostErrorCode.INVALID -> "host.error.invalid"
        HostErrorCode.EMPTY -> "host.error.empty"
        HostErrorCode.CONFLICT -> "host.error.conflict"
        HostErrorCode.PERMISSION -> "host.error.permission"
        HostErrorCode.MISSING -> "host.error.missing"
        HostErrorCode.COMMAND_FAILED, null -> "host.error.generic"
    }
    val localized = container.t(key)
    val detail = error.message?.takeIf { it.isNotBlank() && it != "PERMISSION" && it != "Invalid hosts content" }
    return if (detail != null && localized != key) "$localized ($detail)" else localized
}
