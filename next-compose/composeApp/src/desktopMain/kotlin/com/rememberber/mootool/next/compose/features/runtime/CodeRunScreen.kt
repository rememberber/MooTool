package com.rememberber.mootool.next.compose.features.runtime

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.features.settings.SettingsNavCategory
import com.rememberber.mootool.next.compose.domain.CodeRunEngine
import com.rememberber.mootool.next.compose.domain.CodeRunWiringPresentation
import com.rememberber.mootool.next.compose.domain.CodeRunHistoryMetadata
import com.rememberber.mootool.next.compose.domain.CodeRunHistoryRestore
import com.rememberber.mootool.next.compose.domain.EditorSettingsLiveApply
import com.rememberber.mootool.next.compose.domain.CodeRunErrorCode
import com.rememberber.mootool.next.compose.domain.CodeRunInput
import com.rememberber.mootool.next.compose.domain.CodeRunPaths
import com.rememberber.mootool.next.compose.domain.CodeRunResult
import com.rememberber.mootool.next.compose.domain.CodeRuntime
import com.rememberber.mootool.next.compose.domain.CodeRuntimeStatus
import com.rememberber.mootool.next.compose.editor.EditorAppShortcuts
import com.rememberber.mootool.next.compose.editor.EditorFindHighlight
import com.rememberber.mootool.next.compose.editor.EditorFindOnlyBar
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.editor.EditorFindShortcutPolicy
import com.rememberber.mootool.next.compose.editor.openFindBarSeedingSelection
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CodeRunSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.IoTwoPaneRow
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.mooToolTabsBackground
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooEditorFrame
import com.rememberber.mootool.next.compose.ui.components.mooRuntimeOutputPane
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.MooStatusKind
import com.rememberber.mootool.next.compose.ui.components.MooStatusPill
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.rememberFollowTailScroll
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.OnToolLeaveUnlessDetached
import com.rememberber.mootool.next.compose.ui.workbench.clearErrorOnUserEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.UUID
import androidx.compose.ui.text.font.FontWeight

private const val RUNTIME_WORKSPACE_PANE_KEY = "runtime-editor-output"

@Composable
fun CodeRunScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.codeRunSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Java) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var statuses by remember { mutableStateOf(emptyList<CodeRuntimeStatus>()) }
    val runtime = session.currentRuntime()
    val editorBuffer = session.editor(runtime)
    val status = statuses.firstOrNull { it.id == runtime }
    val outputScroll = rememberFollowTailScroll(
        contentKey = "${session.stdout.length}:${session.stderr.length}:${session.result?.durationMs}",
        resetPinKey = session.requestId
    )

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistCodeRun()
    }

    DisposableEffect(editorBuffer) {
        editorBuffer.onUserDocumentChange = {
            session.clearErrorOnUserEdit { }
            persist()
        }
        onDispose { editorBuffer.onUserDocumentChange = null }
    }

    fun paths() = CodeRunWiringPresentation.pathsFrom(settings.runtime)

    fun detect() {
        session.detecting = true
        persist()
        scope.launch(Dispatchers.IO) {
            val next = CodeRunEngine.detect(paths())
            withContext(Dispatchers.Main) {
                statuses = next
                session.detecting = false
                persist()
            }
        }
    }

    LaunchedEffect(settings.runtime.javaPath, settings.runtime.groovyPath, settings.runtime.pythonPath, settings.runtime.nodePath) {
        detect()
    }

    fun run() {
        if (session.running) return
        if (!CodeRunWiringPresentation.canRun(status)) {
            session.error = container.t("runtime.configure", mapOf("name" to CodeRunEngine.displayName(runtime)))
            persist()
            return
        }
        val arguments = try {
            CodeRunEngine.parseArguments(session.arguments(runtime))
        } catch (error: Exception) {
            session.error = error.message ?: container.t("runtime.error.INVALID_REQUEST")
            persist()
            return
        }
        val requestId = "run-${UUID.randomUUID()}"
        session.requestId = requestId
        session.running = true
        session.stdout = ""
        session.stderr = ""
        session.result = null
        session.error = ""
        persist()
        val input = CodeRunInput(
            requestId = requestId,
            runtime = runtime,
            code = session.editor(runtime).text,
            timeoutMs = settings.network.requestTimeoutMs,
            arguments = arguments,
            workingDirectory = session.workingDirectory(runtime)
        )
        scope.launch(Dispatchers.IO) {
            val result = CodeRunEngine.run(input, paths(), container.directories.cacheRoot.resolve("runtime")) { event ->
                if (event.requestId != requestId) return@run
                scope.launch(Dispatchers.Main) {
                    if (session.requestId != requestId) return@launch
                    if (event.stream == "stdout") session.stdout += event.text else session.stderr += event.text
                    container.sessionManager.bump()
                }
            }
            withContext(Dispatchers.Main) {
                if (session.requestId != requestId) return@withContext
                session.running = false
                session.requestId = ""
                session.result = result
                session.stdout = result.stdout
                session.stderr = result.stderr
                session.error = result.errorCode?.let { messageFor(container, it, result.statusText) }.orEmpty()
                container.history.save(
                    ToolId.Java.id,
                    CodeRunEngine.displayName(runtime),
                    "${CodeRunEngine.displayName(runtime)} · ${result.exitCode ?: "-"}",
                    input.code.take(8_000),
                    listOf(result.stdout, result.stderr).filter { it.isNotBlank() }.joinToString("\n").take(8_000),
                    CodeRunHistoryMetadata.encode(runtime, session.arguments(runtime), session.workingDirectory(runtime)),
                )
                if (session.error.isNotEmpty()) {
                    container.toastError(session.error)
                } else {
                    val code = result.exitCode?.toString() ?: "-"
                    container.toastSuccess(container.t("runtime.exitCode", mapOf("code" to code)))
                }
                persist()
            }
        }
    }

    fun cancelRun() {
        if (session.requestId.isNotBlank()) CodeRunEngine.cancel(session.requestId)
    }

    fun formatSource() {
        if (session.running) return
        runCatching {
            session.editor(runtime).setText(
                CodeRunEngine.formatSource(session.editor(runtime).text, runtime),
                recordUndo = true
            )
            persist()
        }.onFailure {
            session.error = it.message ?: container.t("runtime.failed")
            persist()
        }
    }

    fun openSourceFind() {
        openFindBarSeedingSelection(session.editor(runtime)) { selected ->
            if (selected != null) session.findQuery = selected
            session.findOpen = true
            persist()
        }
    }

    val sourceEditor = session.editor(runtime)

    OnToolLeaveUnlessDetached(container, ToolId.Java) { cancelRun() }

    LaunchedEffect(session.findOpen, session.findQuery, session.findOptions, revision, sourceEditor.revision) {
        EditorFindHighlight.sync(sourceEditor, session.findOpen, session.findQuery, session.findOptions, colors)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
    Column(
        Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
            if (event.blockedByIme()) return@onPreviewKeyEvent false
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val meta = event.isMetaPressed || event.isCtrlPressed
            when {
                meta && event.key == Key.Enter -> {
                    if (!session.running) run()
                    true
                }
                EditorFindShortcutPolicy.opensShellFind(
                    ToolId.Java,
                    event.key,
                    meta = meta,
                    shift = event.isShiftPressed,
                    alt = event.isAltPressed,
                ) -> {
                    openSourceFind()
                    true
                }
                event.key == Key.Escape && session.findOpen -> {
                    session.findOpen = false
                    persist()
                    true
                }
                else -> false
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("runtime.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            val available = CodeRunWiringPresentation.availableCount(statuses)
            MooStatusPill(
                if (session.detecting) container.t("common.processing") else container.t("runtime.detected", mapOf("count" to available.toString())),
                kind = if (available > 0) MooStatusKind.Valid else MooStatusKind.Error
            )
            if (session.error.isNotEmpty()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; persist() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Java) })
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().mooToolTabsBackground().padding(start = 10.dp, end = 10.dp, top = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf("java", "python", "node").forEach { tab ->
                MooToolTab(
                    container.t("runtime.tab.$tab"),
                    selected = session.tab == tab,
                    enabled = !session.running,
                    onClick = {
                        session.tab = tab
                        session.stdout = ""
                        session.stderr = ""
                        session.result = null
                        persist()
                    }
                )
            }
            if (session.tab == "java") {
                MooButton(container.t("runtime.mode.java"), primary = session.javaMode == "java", enabled = !session.running, p5Toolbar = true, onClick = {
                    session.javaMode = "java"; persist()
                })
                MooButton(container.t("runtime.mode.groovy"), primary = session.javaMode == "groovy", enabled = !session.running, p5Toolbar = true, onClick = {
                    session.javaMode = "groovy"; persist()
                })
            }
            Spacer(Modifier.weight(1f))
            if (session.running) {
                MooButton(container.t("runtime.stop"), p5Toolbar = true, onClick = {
                    if (session.requestId.isNotBlank()) CodeRunEngine.cancel(session.requestId)
                })
            } else {
                MooButton(container.t("runtime.run"), prominent = true, p5Toolbar = true, onClick = { run() })
            }
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                p5Toolbar = true,
                actions = listOf(
                    OverflowAction(container.t("runtime.detect"), enabled = !session.detecting && !session.running) { detect() },
                    OverflowAction(container.t("runtime.options")) { session.optionsOpen = true; persist() },
                    OverflowAction(container.t("runtime.format"), enabled = !session.running) { formatSource() },
                    OverflowAction(container.t("runtime.clear")) {
                        session.stdout = ""
                        session.stderr = ""
                        session.result = null
                        persist()
                    }
                )
            )
        }
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (CodeRunWiringPresentation.showConfigureBanner(status)) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(colors.control).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        container.t("runtime.configure", mapOf("name" to CodeRunEngine.displayName(runtime))),
                        color = colors.textBody,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    MooButton(container.t("runtime.openSettings"), p5Toolbar = true, onClick = {
                        container.openSettings(categoryId = SettingsNavCategory.Runtime.storageId())
                    })
                }
            }
            Text(
                status?.let {
                    if (it.available) "${it.command} · ${it.version}" else container.t("runtime.missing", mapOf("name" to CodeRunEngine.displayName(runtime)))
                } ?: container.t("runtime.ready"),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            IoTwoPaneRow(
                container = container,
                settings = settings,
                paneKey = RUNTIME_WORKSPACE_PANE_KEY,
                minLeft = 300f,
                minRight = 300f,
                defaultLeftFraction = 0.5f,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                left = {
                    Column(Modifier.fillMaxSize().mooEditorFrame(flatten = true)) {
                        if (session.findOpen) {
                            EditorFindOnlyBar(
                                container = container,
                                editor = sourceEditor,
                                findQuery = session.findQuery,
                                onFindQueryChange = { session.findQuery = it; persist() },
                                findOptions = session.findOptions,
                                onFindOptionsChange = { session.findOptions = it; persist() },
                                onClose = { session.findOpen = false; persist() },
                                onChanged = { persist() },
                                placeholderKey = "json.find.placeholder",
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(container.t("runtime.editor"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(CodeRunEngine.displayName(runtime), color = colors.textSecondary, fontSize = 11.sp)
                        }
                        EditorHost(
                            buffer = sourceEditor,
                            dark = MooTheme.dark,
                            fontName = com.rememberber.mootool.next.compose.domain.DocumentFormatEngine.editorFont(settings.editor.jsonFontName),
                            fontSize = EditorSettingsLiveApply.jsonEditorFontSize(settings.editor.jsonFontSize),
                            wrap = EditorSettingsLiveApply.runtimeEditorWrap(),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            shortcuts = EditorAppShortcuts(
                                onFind = { openSourceFind() },
                                onFormat = { formatSource() },
                                onSend = { if (!session.running) run() }
                            )
                        )
                    }
                },
                right = {
                    RuntimeOutputPane(container, session, runtime, outputScroll, Modifier.fillMaxSize())
                }
            )
        }
    }
    }
    if (session.optionsOpen) {
        MooOverlay(onDismiss = { session.optionsOpen = false; persist() }) {
            Column(
                Modifier.width(520.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(container.t("runtime.options"), color = colors.textPrimary)
                MooTextField(
                    session.arguments(runtime),
                    { session.setArguments(runtime, it); persist() },
                    placeholder = container.t("runtime.argumentsPlaceholder")
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooTextField(
                        session.workingDirectory(runtime),
                        { session.setWorkingDirectory(runtime, it); persist() },
                        modifier = Modifier.weight(1f),
                        placeholder = container.t("runtime.defaultWorkingDirectory")
                    )
                    MooButton(container.t("runtime.workingDirectory"), p5Toolbar = true, onClick = {
                        val dialog = FileDialog(null as Frame?, container.t("runtime.workingDirectory"), FileDialog.LOAD)
                        dialog.isMultipleMode = false
                        dialog.isVisible = true
                        val dir = dialog.directory
                        if (!dir.isNullOrBlank()) {
                            session.setWorkingDirectory(runtime, File(dir).absolutePath)
                            persist()
                        }
                    })
                }
                MooButton(container.t("common.close"), onClick = { session.optionsOpen = false; persist() })
            }
        }
    }
    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Java.id,
            title = container.t("runtime.history"),
            onRestore = { item ->
                CodeRunHistoryRestore.apply(session, item)
                session.historyOpen = false
                persist()
            },
            onDismiss = { session.historyOpen = false; persist() }
        )
    }
}

@Composable
private fun RuntimeOutputPane(
    container: AppContainer,
    session: CodeRunSession,
    runtime: CodeRuntime,
    outputScroll: ScrollState,
    modifier: Modifier
) {
    val colors = MooTheme.colors
    val result = session.result
    val header = when {
        session.running -> container.t("runtime.running", mapOf("name" to CodeRunEngine.displayName(runtime)))
        result?.timedOut == true -> container.t("runtime.timeout")
        result?.cancelled == true -> container.t("runtime.cancelled")
        result?.truncated == true -> container.t("runtime.truncated")
        result != null && result.exitCode != 0 && result.exitCode != null -> container.t("runtime.failed")
        result != null -> container.t("runtime.completed")
        else -> container.t("runtime.ready")
    }
    Column(modifier.mooToolShell(colors.sidebar).mooRuntimeOutputPane()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(container.t("runtime.output"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(header, color = colors.textSecondary, fontSize = 11.sp)
        }
        SelectionContainer(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp)
        ) {
            val body = listOf(session.stdout, session.stderr).filter { it.isNotBlank() }.joinToString("\n")
            if (body.isBlank() && !session.running) {
                Text(container.t("runtime.ready"), color = colors.textMuted, fontSize = 12.sp, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    body,
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxSize().verticalScroll(outputScroll)
                )
            }
        }
        result?.let {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (it.command.isNotBlank()) {
                    Text(
                        "${container.t("runtime.command")}: ${it.command}",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                it.exitCode?.let { code ->
                    Text(container.t("runtime.exitCode", mapOf("code" to code.toString())), color = colors.textMuted, fontSize = 10.sp)
                }
                Text(container.t("runtime.duration", mapOf("duration" to it.durationMs.toString())), color = colors.textMuted, fontSize = 10.sp)
            }
        }
    }
}

private fun messageFor(container: AppContainer, code: CodeRunErrorCode, fallback: String): String {
    val key = "runtime.error.${code.name}"
    val localized = container.t(key)
    return if (localized == key) fallback.ifBlank { container.t("runtime.failed") } else localized
}
