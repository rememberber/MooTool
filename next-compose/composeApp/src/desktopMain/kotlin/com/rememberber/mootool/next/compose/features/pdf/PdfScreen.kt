package com.rememberber.mootool.next.compose.features.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.PdfEngine
import com.rememberber.mootool.next.compose.domain.PdfException
import com.rememberber.mootool.next.compose.domain.PdfSplitRule
import com.rememberber.mootool.next.compose.domain.PdfTab
import com.rememberber.mootool.next.compose.domain.PdfTaskStatus
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.PdfMergeRow
import com.rememberber.mootool.next.compose.sessions.PdfSession
import com.rememberber.mootool.next.compose.sessions.PdfSplitRow
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun PdfScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.pdfSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistPdf()
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("pdf.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            MooButton(container.t("pdf.help"), onClick = { session.helpOpen = true; refresh() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Pdf) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PdfTab.entries.forEach { tab ->
                MooButton(container.t(tabTitleKey(tab)), primary = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    refresh()
                })
            }
            Spacer(Modifier.weight(1f))
            val limitReached = currentCount(session) >= PdfEngine.MAX_TASKS
            MooButton(
                if (session.tab == PdfTab.Split) container.t("pdf.addTask") else container.t("pdf.addFile"),
                enabled = !session.busy && !limitReached,
                onClick = { addFiles(container, session, ::refresh) }
            )
            if (session.busy) {
                MooButton(container.t("common.cancel"), onClick = { session.cancelled = true; refresh() })
            } else {
                MooButton(
                    if (session.tab == PdfTab.Split) container.t("pdf.startSplit") else container.t("pdf.startMerge"),
                    primary = true,
                    onClick = {
                        if (session.tab == PdfTab.Split) {
                            session.confirmSplit = true
                            refresh()
                        } else {
                            runMerge(container, session, scope, ::refresh)
                        }
                    }
                )
            }
        }
        if (session.tab == PdfTab.Split) {
            SplitTable(container, session, Modifier.weight(1f).fillMaxWidth(), ::refresh)
        } else {
            MergeTable(container, session, Modifier.weight(1f).fillMaxWidth(), ::refresh)
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("pdf.output"), color = colors.textSecondary, fontSize = 12.sp)
            if (session.lastOutputs.isEmpty()) {
                Text("—", color = colors.textSecondary, fontSize = 12.sp)
            } else {
                session.lastOutputs.forEach { output ->
                    Text(
                        output,
                        color = colors.accent,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { reveal(output) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                session.error.ifEmpty { session.notice },
                color = if (session.error.isNotEmpty()) colors.danger else colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }

    if (session.helpOpen) {
        HelpDialog(container, session) { refresh() }
    }
    if (session.confirmSplit) {
        ConfirmDialog(container, session, onConfirm = {
            session.confirmSplit = false
            runSplit(container, session, scope, ::refresh)
        }, onCancel = {
            session.confirmSplit = false
            refresh()
        })
    }
}

@Composable
private fun SplitTable(container: AppContainer, session: PdfSession, modifier: Modifier, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#", color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Text(container.t("pdf.fileName"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(220.dp))
            Text(container.t("pdf.pageRange"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(120.dp))
            Text(container.t("pdf.rule"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(110.dp))
            Text(container.t("pdf.customRule"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(140.dp))
            Text(container.t("pdf.progress"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        }
        if (session.splitRows.isEmpty()) {
            Text(container.t("pdf.empty"), color = colors.textSecondary)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(session.splitRows, key = { _, row -> row.path }) { index, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(row.selected, { checked -> row.selected = checked; onChanged() }, modifier = Modifier.width(28.dp), enabled = !session.busy)
                        Column(Modifier.width(220.dp)) {
                            Text(row.name, color = colors.textPrimary, fontSize = 13.sp)
                            Text("${row.pageCount} ${container.t("pdf.pages")} · ${PdfEngine.formatBytes(row.size)}", color = colors.textSecondary, fontSize = 11.sp)
                        }
                        MooTextField(row.pageRange, { row.pageRange = it; onChanged() }, modifier = Modifier.width(120.dp))
                        RulePicker(container, row, enabled = !session.busy, onChanged)
                        if (row.rule == PdfSplitRule.Custom) {
                            MooTextField(
                                row.customRule,
                                { row.customRule = it; onChanged() },
                                modifier = Modifier.width(140.dp),
                                placeholder = "1-5;8;10"
                            )
                        } else {
                            Text("—", color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(140.dp))
                        }
                        Text(container.t(statusKey(row.status)), color = if (row.status == PdfTaskStatus.Error) colors.danger else colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                        if (!session.busy) {
                            MooButton(container.t("common.delete"), onClick = {
                                session.splitRows = session.splitRows.filterNot { it.path == row.path }
                                onChanged()
                            })
                        }
                        Text("${index + 1}", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeTable(container: AppContainer, session: PdfSession, modifier: Modifier, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (session.mergeRows.isEmpty()) {
            Text(container.t("pdf.empty"), color = colors.textSecondary)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(session.mergeRows, key = { _, row -> row.path }) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(row.selected, { checked -> row.selected = checked; onChanged() }, enabled = !session.busy)
                        Column(Modifier.weight(1f)) {
                            Text(row.name, color = colors.textPrimary, fontSize = 13.sp)
                            Text("${row.pageCount} ${container.t("pdf.pages")} · ${PdfEngine.formatBytes(row.size)}", color = colors.textSecondary, fontSize = 11.sp)
                        }
                        MooTextField(row.pages, { row.pages = it; onChanged() }, modifier = Modifier.width(140.dp))
                        Text(container.t(statusKey(row.status)), color = if (row.status == PdfTaskStatus.Error) colors.danger else colors.textSecondary, fontSize = 12.sp)
                        if (!session.busy) {
                            MooButton(container.t("common.delete"), onClick = {
                                session.mergeRows = session.mergeRows.filterNot { it.path == row.path }
                                onChanged()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RulePicker(container: AppContainer, row: PdfSplitRow, enabled: Boolean, onChanged: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.width(110.dp)) {
        MooButton(container.t(ruleKey(row.rule)), enabled = enabled, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PdfSplitRule.entries.forEach { rule ->
                DropdownMenuItem(onClick = {
                    row.rule = rule
                    open = false
                    onChanged()
                }) { Text(container.t(ruleKey(rule))) }
            }
        }
    }
}

@Composable
private fun HelpDialog(container: AppContainer, session: PdfSession, onChanged: () -> Unit) {
    val keys = if (session.tab == PdfTab.Split) {
        listOf("pdf.help.split1", "pdf.help.split2", "pdf.help.split3", "pdf.help.split4")
    } else {
        listOf("pdf.help.merge1", "pdf.help.merge2", "pdf.help.merge3")
    }
    Dialog(onDismissRequest = { session.helpOpen = false; onChanged() }) {
        Column(
            Modifier.width(520.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t(if (session.tab == PdfTab.Split) "pdf.helpSplitTitle" else "pdf.helpMergeTitle"), color = MooTheme.colors.textPrimary)
            keys.forEach { Text(container.t(it), color = MooTheme.colors.textSecondary, fontSize = 13.sp) }
            MooButton(container.t("common.close"), onClick = { session.helpOpen = false; onChanged() })
        }
    }
}

@Composable
private fun ConfirmDialog(container: AppContainer, session: PdfSession, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.width(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(container.t("pdf.confirmSplit"), color = MooTheme.colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.yes"), primary = true, onClick = onConfirm)
                MooButton(container.t("common.no"), onClick = onCancel)
            }
        }
    }
}

private fun addFiles(container: AppContainer, session: PdfSession, onChanged: () -> Unit) {
    val files = choosePdfs(container.t(if (session.tab == PdfTab.Split) "pdf.addTask" else "pdf.addFile"))
    if (files.isEmpty()) return
    val remaining = PdfEngine.MAX_TASKS - currentCount(session)
    val errors = mutableListOf<String>()
    files.take(remaining).forEach { file ->
        runCatching { PdfEngine.inspect(file.toPath()) }
            .onSuccess { info ->
                if (session.tab == PdfTab.Split) {
                    if (session.splitRows.none { it.path == info.path }) {
                        session.splitRows = session.splitRows + PdfSplitRow(
                            path = info.path,
                            name = info.name,
                            size = info.size,
                            pageCount = info.pageCount,
                            pageRange = "1-${info.pageCount}"
                        )
                    }
                } else if (session.mergeRows.none { it.path == info.path }) {
                    session.mergeRows = session.mergeRows + PdfMergeRow(
                        path = info.path,
                        name = info.name,
                        size = info.size,
                        pageCount = info.pageCount,
                        pages = "1-${info.pageCount}"
                    )
                }
            }
            .onFailure { error -> errors += messageFor(container, error) }
    }
    session.error = errors.firstOrNull().orEmpty()
    session.notice = if (session.error.isEmpty()) "" else session.notice
    onChanged()
}

private fun runSplit(
    container: AppContainer,
    session: PdfSession,
    scope: kotlinx.coroutines.CoroutineScope,
    onChanged: () -> Unit
) {
    val selected = session.splitRows.filter { it.selected }
    if (selected.isEmpty()) {
        session.error = container.t("pdf.selectTask")
        onChanged()
        return
    }
    session.busy = true
    session.cancelled = false
    session.error = ""
    session.notice = container.t("common.processing")
    selected.forEach { it.status = PdfTaskStatus.Running }
    onChanged()
    val tasks = selected.map { PdfEngine.SplitTask(it.path, it.pageRange, it.rule, it.customRule) }
    scope.launch(Dispatchers.Default) {
        val result = runCatching { PdfEngine.split(tasks) { session.cancelled } }
        withContext(Dispatchers.Swing) {
            session.busy = false
            result.onSuccess { value ->
                selected.forEach { it.status = PdfTaskStatus.Done }
                session.lastOutputs = value.outputs
                session.notice = container.t("pdf.splitComplete", mapOf("count" to value.pageCount.toString()))
                session.error = ""
                container.history.save(ToolId.Pdf.id, session.notice, session.notice, selected.joinToString { it.name }, value.outputs.joinToString("\n"), "split")
            }.onFailure { error ->
                selected.forEach { it.status = if ((error as? PdfException)?.code == "cancelled") PdfTaskStatus.Ready else PdfTaskStatus.Error }
                session.lastOutputs = emptyList()
                session.notice = ""
                session.error = messageFor(container, error)
            }
            onChanged()
        }
    }
}

private fun runMerge(
    container: AppContainer,
    session: PdfSession,
    scope: kotlinx.coroutines.CoroutineScope,
    onChanged: () -> Unit
) {
    val selected = session.mergeRows.filter { it.selected }
    if (selected.size < 2) {
        session.error = container.t("pdf.selectTwo")
        onChanged()
        return
    }
    val output = chooseSave(container.t("pdf.startMerge"), defaultMergeName(container)) ?: return
    session.busy = true
    session.cancelled = false
    session.error = ""
    session.notice = container.t("common.processing")
    selected.forEach { it.status = PdfTaskStatus.Running }
    onChanged()
    val sources = selected.map { PdfEngine.MergeSource(it.path, it.pages) }
    scope.launch(Dispatchers.Default) {
        val result = runCatching { PdfEngine.merge(sources, output.toPath()) { session.cancelled } }
        withContext(Dispatchers.Swing) {
            session.busy = false
            result.onSuccess { value ->
                selected.forEach { it.status = PdfTaskStatus.Done }
                session.lastOutputs = value.outputs
                session.notice = container.t("pdf.mergeComplete", mapOf("count" to value.pageCount.toString()))
                session.error = ""
                container.history.save(ToolId.Pdf.id, session.notice, session.notice, selected.joinToString { it.name }, value.outputs.joinToString("\n"), "merge")
            }.onFailure { error ->
                selected.forEach { it.status = if ((error as? PdfException)?.code == "cancelled") PdfTaskStatus.Ready else PdfTaskStatus.Error }
                if ((error as? PdfException)?.code == "cancelled") runCatching { output.delete() }
                session.notice = ""
                session.error = messageFor(container, error)
            }
            onChanged()
        }
    }
}

private fun currentCount(session: PdfSession): Int =
    if (session.tab == PdfTab.Split) session.splitRows.size else session.mergeRows.size

private fun tabTitleKey(tab: PdfTab): String = when (tab) {
    PdfTab.Split -> "pdf.tab.split"
    PdfTab.Merge -> "pdf.tab.merge"
}

private fun ruleKey(rule: PdfSplitRule): String = when (rule) {
    PdfSplitRule.Odd -> "pdf.rule.odd"
    PdfSplitRule.Even -> "pdf.rule.even"
    PdfSplitRule.Custom -> "pdf.rule.custom"
}

private fun statusKey(status: PdfTaskStatus): String = when (status) {
    PdfTaskStatus.Ready -> "pdf.status.ready"
    PdfTaskStatus.Running -> "pdf.status.running"
    PdfTaskStatus.Done -> "pdf.status.done"
    PdfTaskStatus.Error -> "pdf.status.error"
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? PdfException)?.code
    return when (code) {
        "not-pdf" -> container.t("pdf.error.notPdf")
        "encrypted" -> container.t("pdf.error.encrypted")
        "invalid-range", "no-pages" -> container.t("pdf.error.range")
        "empty-pages", "empty-selection" -> container.t("pdf.error.emptyPages")
        "need-two" -> container.t("pdf.selectTwo")
        "too-many" -> container.t("pdf.error.tooMany")
        "cancelled" -> container.t("pdf.cancelled")
        "missing" -> container.t("pdf.error.missing")
        else -> error.message ?: container.t("pdf.error.generic")
    }
}

private fun defaultMergeName(container: AppContainer): String {
    val export = container.settings.value.tools.exportDirectory
    val directory = export.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.isDirectory }
        ?: File(System.getProperty("user.home"), "Desktop").takeIf { it.isDirectory }
        ?: File(System.getProperty("user.home"))
    return File(directory, "merge.pdf").absolutePath
}

private fun choosePdfs(title: String): List<File> {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.file = "*.pdf"
    dialog.isVisible = true
    val files = dialog.files?.toList().orEmpty()
    if (files.isNotEmpty()) return files.filter { it.extension.equals("pdf", ignoreCase = true) }
    val directory = dialog.directory ?: return emptyList()
    val file = dialog.file ?: return emptyList()
    return listOf(File(directory, file)).filter { it.exists() }
}

private fun chooseSave(title: String, defaultPath: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    val preset = File(defaultPath)
    dialog.directory = preset.parent
    dialog.file = preset.name
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    val chosen = File(directory, file)
    return if (chosen.extension.isBlank()) File(chosen.path + ".pdf") else chosen
}

private fun reveal(path: String) {
    val file = File(path)
    val target = if (file.exists()) file else file.parentFile
    if (target != null && target.exists()) runCatching { Desktop.getDesktop().open(if (file.isFile) file.parentFile else target) }
}
