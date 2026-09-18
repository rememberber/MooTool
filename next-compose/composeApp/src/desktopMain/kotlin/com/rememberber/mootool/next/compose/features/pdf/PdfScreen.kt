package com.rememberber.mootool.next.compose.features.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ToolsExportWiringPresentation
import com.rememberber.mootool.next.compose.domain.PdfEngine
import com.rememberber.mootool.next.compose.domain.PdfImportPresentation
import com.rememberber.mootool.next.compose.domain.PdfWiringPresentation
import com.rememberber.mootool.next.compose.domain.PdfHistoryMetadata
import com.rememberber.mootool.next.compose.domain.PdfHistoryRestore
import com.rememberber.mootool.next.compose.domain.PdfException
import com.rememberber.mootool.next.compose.domain.PdfSplitRule
import com.rememberber.mootool.next.compose.domain.PdfTab
import com.rememberber.mootool.next.compose.domain.PdfTaskStatus
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.PdfMergeRow
import com.rememberber.mootool.next.compose.sessions.PdfSession
import com.rememberber.mootool.next.compose.sessions.PdfSplitRow
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.desktopFileDropTarget
import com.rememberber.mootool.next.compose.ui.components.MooGhostButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.mooToolTabsBackground
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.mooPdfEmptyState
import com.rememberber.mootool.next.compose.ui.components.mooPdfOutputStrip
import com.rememberber.mootool.next.compose.ui.components.mooPdfTableWrap
import com.rememberber.mootool.next.compose.ui.components.mooPdfToolbarActions
import com.rememberber.mootool.next.compose.ui.chooseFileWithExportDirectory
import com.rememberber.mootool.next.compose.ui.chooseFilesWithExportDirectory
import com.rememberber.mootool.next.compose.ui.persistToolsExportDirectory
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

@Composable
fun PdfScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.pdfSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Pdf) {
        if (session.busy) session.cancelled = true
        session.dismissModalOverlays()
    }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistPdf()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("pdf.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    add(OverflowAction(container.t("pdf.help")) { session.helpOpen = true; refresh() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Pdf) })
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .mooToolTabsBackground()
                .mooPdfToolbarActions()
                .padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PdfTab.entries.forEach { tab ->
                MooToolTab(container.t(tabTitleKey(tab)), selected = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    refresh()
                })
            }
            Spacer(Modifier.weight(1f))
            Text(
                container.t("pdf.toolbar.limits"),
                color = colors.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val splitCount = session.splitRows.size
            val mergeCount = session.mergeRows.size
            MooButton(
                if (session.tab == PdfTab.Split) container.t("pdf.addTask") else container.t("pdf.addFile"),
                enabled = PdfWiringPresentation.canAddTask(session.busy, session.tab, splitCount, mergeCount),
                p5Toolbar = true,
                onClick = { addFiles(container, session, ::refresh) }
            )
            if (session.busy) {
                MooButton(container.t("common.cancel"), p5Toolbar = true, onClick = { session.cancelled = true; refresh() })
            } else {
                MooButton(
                    if (session.tab == PdfTab.Split) container.t("pdf.startSplit") else container.t("pdf.startMerge"),
                    prominent = true,
                    p5Toolbar = true,
                    enabled = when (session.tab) {
                        PdfTab.Split -> PdfWiringPresentation.canStartSplit(
                            session.busy,
                            session.splitRows.count { it.selected },
                        )
                        PdfTab.Merge -> PdfWiringPresentation.canStartMerge(
                            session.busy,
                            session.mergeRows.count { it.selected },
                        )
                    },
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
            modifier = Modifier.fillMaxWidth().mooPdfOutputStrip(),
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
                        modifier = Modifier.mooFocusClickable { reveal(output) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
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
    }

    if (session.helpOpen) {
        HelpDialog(container, session) { refresh() }
    }
    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Pdf.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                PdfHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
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
    Column(
        modifier
            .mooPdfTableWrap()
            .desktopFileDropTarget(enabled = !session.busy, acceptMultiple = true) { dropped ->
                ingestPdfFiles(container, session, dropped, onChanged)
            },
    ) {
        PdfTableHeader {
            Text("#", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
            Text(container.t("pdf.fileName"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(220.dp))
            Text(container.t("pdf.pageRange"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(120.dp))
            Text(container.t("pdf.rule"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(110.dp))
            Text(container.t("pdf.customRule"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(140.dp))
            Text(container.t("pdf.progress"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
            Spacer(Modifier.width(44.dp))
        }
        if (session.splitRows.isEmpty()) {
            PdfEmpty(container)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(session.splitRows, key = { _, row -> row.path }) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(row.selected, { checked -> row.selected = checked; onChanged() }, modifier = Modifier.width(44.dp), enabled = !session.busy)
                        Column(Modifier.width(220.dp)) {
                            Text(row.name, color = colors.textBody, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${row.pageCount} ${container.t("pdf.pages")} · ${PdfEngine.formatBytes(row.size)}",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        MooTextField(
                            row.pageRange,
                            { value -> session.onUserInput { row.pageRange = value; onChanged() } },
                            modifier = Modifier.width(120.dp),
                            compact = true
                        )
                        RulePicker(container, row, enabled = !session.busy, onChanged)
                        if (row.rule == PdfSplitRule.Custom) {
                            MooTextField(
                                row.customRule,
                                { value -> session.onUserInput { row.customRule = value; onChanged() } },
                                modifier = Modifier.width(140.dp),
                                placeholder = "1-5;8;10",
                                compact = true
                            )
                        } else {
                            Text("—", color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.width(140.dp))
                        }
                        Text(
                            container.t(statusKey(row.status)),
                            color = when (row.status) {
                                PdfTaskStatus.Done -> colors.success
                                PdfTaskStatus.Error -> colors.danger
                                else -> colors.textMuted
                            },
                            fontSize = 11.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                            if (!session.busy) {
                                MooGhostButton(container.t("common.delete"), onClick = {
                                    session.splitRows = session.splitRows.filterNot { it.path == row.path }
                                    onChanged()
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

@Composable
private fun MergeTable(container: AppContainer, session: PdfSession, modifier: Modifier, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(
        modifier
            .mooPdfTableWrap()
            .desktopFileDropTarget(enabled = !session.busy, acceptMultiple = true) { dropped ->
                ingestPdfFiles(container, session, dropped, onChanged)
            },
    ) {
        PdfTableHeader {
            Text("#", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
            Text(container.t("pdf.fileName"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(container.t("pdf.mergeRange"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(140.dp))
            Text(container.t("pdf.progress"), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
            Spacer(Modifier.width(44.dp))
        }
        if (session.mergeRows.isEmpty()) {
            PdfEmpty(container)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(session.mergeRows, key = { _, row -> row.path }) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(row.selected, { checked -> row.selected = checked; onChanged() }, modifier = Modifier.width(44.dp), enabled = !session.busy)
                        Column(Modifier.weight(1f)) {
                            Text(row.name, color = colors.textBody, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${row.pageCount} ${container.t("pdf.pages")} · ${PdfEngine.formatBytes(row.size)}",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        MooTextField(
                            row.pages,
                            { value -> session.onUserInput { row.pages = value; onChanged() } },
                            modifier = Modifier.width(140.dp),
                            compact = true
                        )
                        Text(
                            container.t(statusKey(row.status)),
                            color = when (row.status) {
                                PdfTaskStatus.Done -> colors.success
                                PdfTaskStatus.Error -> colors.danger
                                else -> colors.textMuted
                            },
                            fontSize = 11.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                            if (!session.busy) {
                                MooGhostButton(container.t("common.delete"), onClick = {
                                    session.mergeRows = session.mergeRows.filterNot { it.path == row.path }
                                    onChanged()
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

@Composable
private fun PdfTableHeader(content: @Composable RowScope.() -> Unit) {
    val colors = MooTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(36.dp).background(colors.toolbar).padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
    }
}

@Composable
private fun PdfEmpty(container: AppContainer) {
    Box(
        Modifier.fillMaxWidth().height(240.dp).mooPdfEmptyState(),
        contentAlignment = Alignment.Center,
    ) {
        Text(container.t("pdf.empty"), color = MooTheme.colors.textMuted, fontSize = 11.sp)
    }
}

@Composable
private fun RulePicker(container: AppContainer, row: PdfSplitRow, enabled: Boolean, onChanged: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.width(110.dp)) {
        MooButton(container.t(ruleKey(row.rule)), enabled = enabled, p5Toolbar = true, onClick = { open = true })
        MooMenu(expanded = open, onDismissRequest = { open = false }) {
            PdfSplitRule.entries.forEach { rule ->
                MooMenuItem(onClick = {
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
    MooOverlay(onDismiss = { session.helpOpen = false; onChanged() }) {
        Column(
            Modifier.width(520.dp).mooDialogSurface().padding(16.dp),
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
    MooOverlay(onDismiss = onCancel) {
        Column(
            Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(container.t("pdf.confirmSplit"), color = MooTheme.colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.yes"), prominent = true, onClick = onConfirm)
                MooButton(container.t("common.no"), onClick = onCancel)
            }
        }
    }
}

private fun addFiles(container: AppContainer, session: PdfSession, onChanged: () -> Unit) {
    val title = container.t(if (session.tab == PdfTab.Split) "pdf.addTask" else "pdf.addFile")
    val picked = chooseFilesWithExportDirectory(container, title, fileFilter = "*.pdf")
    val files = picked.filter { it.extension.equals("pdf", ignoreCase = true) }
    if (files.isEmpty()) return
    files.firstOrNull()?.let { persistToolsExportDirectory(container, it) }
    ingestPdfFiles(container, session, files, onChanged)
}

private fun ingestPdfFiles(container: AppContainer, session: PdfSession, files: List<File>, onChanged: () -> Unit) {
    if (session.busy) return
    val remaining = PdfWiringPresentation.ingestRemainingSlots(
        session.tab,
        session.splitRows.size,
        session.mergeRows.size,
    )
    val errors = mutableListOf<String>()
    files.take(remaining).forEach { file ->
        when (val inspected = PdfWiringPresentation.inspectFile(file.toPath())) {
            is PdfWiringPresentation.InspectOutcome.Success -> {
                val info = inspected.info
                if (PdfImportPresentation.shouldShowStructureNotice(info)) {
                    container.toastInfo(
                        container.t("pdf.structureNotice", PdfImportPresentation.structureNoticeArgs(info)),
                    )
                }
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
            is PdfWiringPresentation.InspectOutcome.Failure -> {
                val error = inspected.error
                val message = messageFor(container, error)
                notifyPdfImportFailure(container, error, message)
                errors += message
            }
        }
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
        val result = PdfWiringPresentation.runSplit(tasks) { session.cancelled }
        withContext(Dispatchers.Swing) {
            session.busy = false
            when (result) {
                is PdfWiringPresentation.JobOutcome.Success -> {
                val value = result.value
                selected.forEach { it.status = PdfTaskStatus.Done }
                session.lastOutputs = value.outputs
                session.notice = container.t("pdf.splitComplete", mapOf("count" to value.pageCount.toString()))
                container.toastSuccess(session.notice)
                session.error = ""
                container.history.save(
                    ToolId.Pdf.id,
                    session.notice,
                    session.notice,
                    selected.joinToString { it.name },
                    value.outputs.joinToString("\n"),
                    PdfHistoryMetadata.OP_SPLIT,
                )
                }
                is PdfWiringPresentation.JobOutcome.Failure -> {
                val error = result.error
                selected.forEach { it.status = if ((error as? PdfException)?.code == "cancelled") PdfTaskStatus.Ready else PdfTaskStatus.Error }
                session.lastOutputs = emptyList()
                session.notice = ""
                notifyPdfJobFailure(container, session, error)
                }
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
    val output = pickMergeOutputFile(container) ?: return
    persistToolsExportDirectory(container, output)
    session.busy = true
    session.cancelled = false
    session.error = ""
    session.notice = container.t("common.processing")
    selected.forEach { it.status = PdfTaskStatus.Running }
    onChanged()
    val sources = selected.map { PdfEngine.MergeSource(it.path, it.pages) }
    scope.launch(Dispatchers.Default) {
        val result = PdfWiringPresentation.runMerge(sources, output.toPath()) { session.cancelled }
        withContext(Dispatchers.Swing) {
            session.busy = false
            when (result) {
                is PdfWiringPresentation.JobOutcome.Success -> {
                val value = result.value
                selected.forEach { it.status = PdfTaskStatus.Done }
                session.lastOutputs = value.outputs
                session.notice = container.t("pdf.mergeComplete", mapOf("count" to value.pageCount.toString()))
                container.toastSuccess(session.notice)
                session.error = ""
                container.history.save(
                    ToolId.Pdf.id,
                    session.notice,
                    session.notice,
                    selected.joinToString { it.name },
                    value.outputs.joinToString("\n"),
                    PdfHistoryMetadata.OP_MERGE,
                )
                }
                is PdfWiringPresentation.JobOutcome.Failure -> {
                val error = result.error
                selected.forEach { it.status = if ((error as? PdfException)?.code == "cancelled") PdfTaskStatus.Ready else PdfTaskStatus.Error }
                if ((error as? PdfException)?.code == "cancelled") runCatching { output.delete() }
                session.notice = ""
                notifyPdfJobFailure(container, session, error)
                }
            }
            onChanged()
        }
    }
}

private fun notifyPdfJobFailure(container: AppContainer, session: PdfSession, error: Throwable) {
    session.error = messageFor(container, error)
    if (PdfWiringPresentation.shouldToastJobFailure(error)) {
        container.toastError(session.error)
    }
}

private fun notifyPdfImportFailure(container: AppContainer, error: Throwable, message: String) {
    if (PdfImportPresentation.showEncryptedImportToast(error)) {
        container.toastError(message)
    }
}

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

private fun pickMergeOutputFile(container: AppContainer): File? {
    val preset = File(
        ToolsExportWiringPresentation.defaultMergePdfPath(container.settings.value.tools.exportDirectory),
    )
    val chosen = chooseFileWithExportDirectory(
        container,
        save = true,
        title = container.t("pdf.startMerge"),
        defaultFileName = preset.name,
    ) ?: return null
    return if (chosen.extension.isBlank()) File(chosen.path + ".pdf") else chosen
}

private fun reveal(path: String) {
    val file = File(path)
    val target = if (file.exists()) file else file.parentFile
    if (target != null && target.exists()) runCatching { Desktop.getDesktop().open(if (file.isFile) file.parentFile else target) }
}
