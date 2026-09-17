package com.rememberber.mootool.next.compose.features.reformat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ReformatEngine
import com.rememberber.mootool.next.compose.domain.ReformatException
import com.rememberber.mootool.next.compose.domain.ReformatType
import com.rememberber.mootool.next.compose.domain.ReformatHistoryMetadata
import com.rememberber.mootool.next.compose.domain.ReformatHistoryRestore
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ReformatSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.FileDropRow
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.mooToolTabsBackground
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.CopyFeedbackPolicy
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import com.rememberber.mootool.next.compose.ui.chooseFileWithExportDirectory
import com.rememberber.mootool.next.compose.ui.persistToolsExportDirectory
import java.io.File
import java.nio.charset.StandardCharsets

@Composable
fun ReformatScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.reformatSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Reformat) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistReformat()
    }

    LaunchedEffect(session.copyGeneration, session.copyState) {
        if (session.copyState == CopyFeedbackPolicy.IDLE) return@LaunchedEffect
        delay(CopyFeedbackPolicy.RESET_MS)
        session.copyState = CopyFeedbackPolicy.IDLE
        refresh()
    }

    fun copyOutput() {
        val content = currentOutput(session)
        if (content.isEmpty()) {
            session.notice = container.t("reformat.nothingToCopy")
            session.copyState = CopyFeedbackPolicy.IDLE
        } else {
            val success = container.copyText(content)
            session.copyState = CopyFeedbackPolicy.afterCopy(success)
            session.copyGeneration += 1
            session.notice = if (success) container.t("common.copied") else container.t("json.notice.copyFailed")
            session.error = ""
        }
        refresh()
    }

    fun saveOutput() {
        saveResult(container, session)
        refresh()
    }

    fun clearOutput() {
        clearTab(session)
        session.error = ""
        refresh()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val contentMaxWidth = maxWidth.value
    val overflow = LayoutPolicy.overflowToolbar(contentMaxWidth)
    var typeOpen by remember { mutableStateOf(false) }
    var indentOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return@onPreviewKeyEvent false
            val meta = event.isMetaPressed || event.isCtrlPressed
            if (meta && event.isShiftPressed && event.key == Key.F) {
                runFormat(container, session) { refresh() }
                true
            } else {
                false
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("reformat.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Reformat) })
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().mooToolTabsBackground().horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, top = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MooToolTab(container.t("reformat.tab.text"), selected = session.tab == "text", onClick = {
                session.tab = "text"
                session.error = ""
                refresh()
            })
            MooToolTab(container.t("reformat.tab.file"), selected = session.tab == "file", onClick = {
                session.tab = "file"
                session.error = ""
                refresh()
            })
            Box {
                MooButton("${container.t("reformat.type")}: ${typeLabel(session.type)}", onClick = { typeOpen = true }, p5Toolbar = true)
                MooMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
                    ReformatEngine.types.forEach { type ->
                        MooMenuItem(onClick = {
                            typeOpen = false
                            changeType(session, type)
                            refresh()
                        }) {
                            Text(typeLabel(type))
                        }
                    }
                }
            }
            Box {
                MooButton("${container.t("reformat.indent")}: ${session.indent}", onClick = { indentOpen = true }, p5Toolbar = true)
                MooMenu(expanded = indentOpen, onDismissRequest = { indentOpen = false }) {
                    listOf(2, 3, 4, 5, 6).forEach { size ->
                        MooMenuItem(onClick = {
                            indentOpen = false
                            session.indent = size
                            refresh()
                        }) {
                            Text(size.toString())
                        }
                    }
                }
            }
            MooButton(
                if (session.busy) container.t("reformat.processing") else container.t("reformat.format"),
                prominent = true,
                p5Toolbar = true,
                enabled = !session.busy && currentInput(session).isNotBlank(),
                onClick = { runFormat(container, session) { refresh() } }
            )
            if (!overflow) {
                MooButton(
                    container.t(CopyFeedbackPolicy.buttonKey(session.copyState, "reformat.copy")),
                    onClick = { copyOutput() },
                    p5Toolbar = true
                )
                MooButton(container.t("reformat.save"), onClick = { saveOutput() }, p5Toolbar = true)
                MooButton(container.t("common.action.clear"), onClick = { clearOutput() }, p5Toolbar = true)
            } else {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true }, p5Toolbar = true)
                    MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MooMenuItem(onClick = { moreOpen = false; copyOutput() }) {
                            Text(container.t(CopyFeedbackPolicy.buttonKey(session.copyState, "reformat.copy")))
                        }
                        MooMenuItem(onClick = { moreOpen = false; saveOutput() }) {
                            Text(container.t("reformat.save"))
                        }
                        MooMenuItem(onClick = { moreOpen = false; clearOutput() }) {
                            Text(container.t("common.action.clear"))
                        }
                    }
                }
            }
        }
        if (session.tab == "text") {
            Column(Modifier.weight(1f).fillMaxWidth().mooToolShell().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(container.t("reformat.input"), color = colors.textSecondary, fontSize = 12.sp)
                MooTextField(
                    session.text,
                    {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            session.text = it
                            session.error = ""
                        }
                        refresh()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false
                )
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FileDropRow(
                    chooseLabel = container.t("reformat.chooseFile"),
                    fileName = session.fileName,
                    emptyLabel = container.t("reformat.noFile"),
                    onChoose = {
                        chooseSourceFile(container, session)
                        refresh()
                    },
                    onDropFiles = { files ->
                        loadSourceFile(container, session, files.first())
                        refresh()
                    }
                )
                val minPane = 260f
                val paneHandle = 10f
                val innerWidth = contentMaxWidth - 24f
                val maxLeft = (innerWidth - paneHandle - minPane).coerceAtLeast(minPane)
                val defaultLeft = (innerWidth * 0.5f).coerceIn(minPane, maxLeft)
                val leftWidth = settings.layout.pane(ToolId.Reformat.id, 0, defaultLeft, minPane, maxLeft)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier.width(leftWidth.dp).widthIn(min = 260.dp).fillMaxHeight().mooToolShell().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(container.t("reformat.original"), color = colors.textSecondary, fontSize = 12.sp)
                        MooTextField(
                            session.fileSource,
                            {
                                applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                                    session.fileSource = it
                                    session.error = ""
                                }
                                refresh()
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            singleLine = false
                        )
                    }
                    VerticalPaneHandle(
                        onDelta = { container.setPaneSize(ToolId.Reformat.id, 0, leftWidth + it, 1) },
                        onReset = { container.setPaneSize(ToolId.Reformat.id, 0, defaultLeft, 1) }
                    )
                    Column(
                        Modifier.weight(1f).widthIn(min = 260.dp).fillMaxHeight().mooToolShell().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(container.t("reformat.result"), color = colors.textSecondary, fontSize = 12.sp)
                        MooTextField(
                            session.fileResult,
                            {},
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            singleLine = false
                        )
                    }
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

    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Reformat.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                ReformatHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

private fun typeLabel(type: ReformatType): String = when (type) {
    ReformatType.Nginx -> "Nginx"
    ReformatType.Java -> "Java"
    ReformatType.Xml -> "XML"
    ReformatType.Html -> "HTML"
}

private fun currentInput(session: ReformatSession): String =
    if (session.tab == "file") session.fileSource else session.text

private fun currentOutput(session: ReformatSession): String =
    if (session.tab == "file") session.fileResult else session.text

private fun changeType(session: ReformatSession, nextType: ReformatType) {
    val samples = ReformatEngine.samples.values
    if (session.tab == "text" && (session.text.isBlank() || session.text in samples)) {
        session.text = ReformatEngine.samples.getValue(nextType)
    }
    session.type = nextType
    session.error = ""
}

private fun clearTab(session: ReformatSession) {
    if (session.tab == "text") {
        session.text = ""
    } else {
        session.fileName = ""
        session.fileSource = ""
        session.fileResult = ""
    }
}

private fun runFormat(container: AppContainer, session: ReformatSession, onChanged: () -> Unit) {
    val input = currentInput(session)
    if (input.isBlank() || session.busy) return
    session.formatGeneration += 1
    val generation = session.formatGeneration
    val type = session.type
    val indent = session.indent
    val tab = session.tab
    val fileName = session.fileName
    session.busy = true
    session.error = ""
    session.notice = container.t("reformat.processing")
    onChanged()
    container.scope.launch {
        val result = runCatching { ReformatEngine.format(input, type, indent) }
        withContext(Dispatchers.Swing) {
            if (generation != session.formatGeneration) return@withContext
            session.busy = false
            result.onSuccess { output ->
                if (tab == "file") session.fileResult = output else session.text = output
                session.error = ""
                session.notice = container.t("reformat.formatted")
                container.toastSuccess(container.t("reformat.formatted"))
                val summary = container.t("reformat.historySummary", mapOf("type" to type.name.uppercase()))
                container.history.save(
                    ToolId.Reformat.id,
                    summary,
                    summary,
                    input,
                    output,
                    ReformatHistoryMetadata.encode(type, tab, indent, fileName),
                )
            }.onFailure { error ->
                session.notice = ""
                session.error = messageFor(container, error)
            }
            onChanged()
        }
    }
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val reformat = error as? ReformatException
    return if (reformat != null && reformat.line >= 1) {
        container.t(
            "reformat.error.located",
            mapOf("line" to reformat.line.toString(), "column" to reformat.column.coerceAtLeast(0).toString(), "message" to (reformat.message ?: ""))
        )
    } else {
        container.t("reformat.error.generic", mapOf("message" to (error.message ?: "")))
    }
}

private fun chooseSourceFile(container: AppContainer, session: ReformatSession) {
    val file = chooseFileWithExportDirectory(container, save = false, title = container.t("reformat.chooseFile")) ?: return
    loadSourceFile(container, session, file)
}

private fun loadSourceFile(container: AppContainer, session: ReformatSession, file: File) {
    runCatching { file.readText(StandardCharsets.UTF_8) }
        .onSuccess { content ->
            session.fileName = file.name
            session.fileSource = content
            session.fileResult = ""
            session.error = ""
            session.notice = container.t("json.notice.imported")
            container.toastSuccess(container.t("json.notice.imported"))
        }
        .onFailure { error ->
            session.error = container.t("reformat.error.read", mapOf("message" to (error.message ?: file.path)))
        }
}

private fun saveResult(container: AppContainer, session: ReformatSession) {
    val content = currentOutput(session)
    if (content.isEmpty()) {
        session.notice = container.t("reformat.nothingToSave")
        return
    }
    val extension = when (session.type) {
        ReformatType.Nginx -> "conf"
        ReformatType.Java -> "java"
        ReformatType.Xml -> "xml"
        ReformatType.Html -> "html"
    }
    val base = session.fileName.replace(Regex("\\.[^.]+$"), "").ifEmpty { "formatted" }
    val file = chooseFileWithExportDirectory(
        container,
        save = true,
        title = container.t("reformat.save"),
        defaultFileName = "$base.$extension",
    ) ?: return
    runCatching { file.writeText(content, StandardCharsets.UTF_8) }
        .onSuccess {
            session.error = ""
            session.notice = container.t("reformat.saved")
            persistToolsExportDirectory(container, file)
            container.toastSuccess(container.t("reformat.saved"))
        }
        .onFailure { error ->
            session.error = container.t("reformat.error.write", mapOf("message" to (error.message ?: file.path)))
        }
}


