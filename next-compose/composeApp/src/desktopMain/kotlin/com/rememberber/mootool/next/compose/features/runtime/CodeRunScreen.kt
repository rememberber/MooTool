package com.rememberber.mootool.next.compose.features.runtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.rememberber.mootool.next.compose.domain.CodeRunEngine
import com.rememberber.mootool.next.compose.domain.CodeRunErrorCode
import com.rememberber.mootool.next.compose.domain.CodeRunInput
import com.rememberber.mootool.next.compose.domain.CodeRunPaths
import com.rememberber.mootool.next.compose.domain.CodeRunResult
import com.rememberber.mootool.next.compose.domain.CodeRuntime
import com.rememberber.mootool.next.compose.domain.CodeRuntimeStatus
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CodeRunSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.UUID

@Composable
fun CodeRunScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.codeRunSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var statuses by remember { mutableStateOf(emptyList<CodeRuntimeStatus>()) }
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val runtime = session.currentRuntime()
    val status = statuses.firstOrNull { it.id == runtime }

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistCodeRun()
    }

    fun paths() = settings.runtime.let {
        CodeRunPaths(it.javaPath, it.groovyPath, it.pythonPath, it.nodePath)
    }

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
    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Java.id)
    }

    fun run() {
        if (session.running) return
        if (status?.available == false) {
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
                    listOf(result.stdout, result.stderr).filter { it.isNotBlank() }.joinToString("\n").take(8_000)
                )
                persist()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && (event.isMetaPressed || event.isCtrlPressed)) {
                if (!session.running) run()
                true
            } else false
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("runtime.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            val available = statuses.count { it.available }
            Text(
                if (session.detecting) container.t("common.processing") else container.t("runtime.detected", mapOf("count" to available.toString())),
                color = if (available > 0) colors.textSecondary else colors.danger,
                fontSize = 12.sp
            )
            if (session.error.isNotEmpty()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; persist() })
            if (!detached) MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Java) })
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("java", "python", "node").forEach { tab ->
                MooButton(
                    container.t("runtime.tab.$tab"),
                    primary = session.tab == tab,
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
                MooButton(container.t("runtime.mode.java"), primary = session.javaMode == "java", enabled = !session.running, onClick = {
                    session.javaMode = "java"; persist()
                })
                MooButton(container.t("runtime.mode.groovy"), primary = session.javaMode == "groovy", enabled = !session.running, onClick = {
                    session.javaMode = "groovy"; persist()
                })
            }
            Spacer(Modifier.weight(1f))
            MooButton(container.t("runtime.detect"), enabled = !session.detecting && !session.running, onClick = { detect() })
            MooButton(container.t("runtime.options"), onClick = { session.optionsOpen = true; persist() })
            MooButton(container.t("runtime.format"), enabled = !session.running, onClick = {
                runCatching {
                    session.editor(runtime).setText(CodeRunEngine.formatSource(session.editor(runtime).text, runtime), recordUndo = true)
                    persist()
                }.onFailure { session.error = it.message ?: container.t("runtime.failed"); persist() }
            })
            if (session.running) {
                MooButton(container.t("runtime.stop"), onClick = {
                    if (session.requestId.isNotBlank()) CodeRunEngine.cancel(session.requestId)
                })
            } else {
                MooButton(container.t("runtime.run"), primary = true, onClick = { run() })
            }
            MooButton(container.t("runtime.clear"), onClick = {
                session.stdout = ""
                session.stderr = ""
                session.result = null
                persist()
            })
        }
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                status?.let {
                    if (it.available) "${it.command} · ${it.version}" else container.t("runtime.missing", mapOf("name" to CodeRunEngine.displayName(runtime)))
                } ?: container.t("runtime.ready"),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            EditorHost(
                buffer = session.editor(runtime),
                dark = MooTheme.dark,
                fontName = "Monospaced",
                fontSize = settings.editor.jsonFontSize,
                wrap = settings.editor.softWrap,
                modifier = Modifier.fillMaxWidth().weight(1.2f)
            )
            SelectionContainer(Modifier.fillMaxWidth().weight(1f).background(colors.sidebar, RoundedCornerShape(8.dp)).padding(10.dp)) {
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
                val meta = result?.let {
                    listOfNotNull(
                        it.command.takeIf { command -> command.isNotBlank() }?.let { command -> "${container.t("runtime.command")}: $command" },
                        it.exitCode?.let { code -> container.t("runtime.exitCode", mapOf("code" to code.toString())) },
                        container.t("runtime.duration", mapOf("duration" to it.durationMs.toString()))
                    ).joinToString(" · ")
                }.orEmpty()
                val body = listOf(session.stdout, session.stderr).filter { it.isNotBlank() }.joinToString("\n")
                Text(
                    listOf(header, meta, body).filter { it.isNotBlank() }.joinToString("\n"),
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                )
            }
        }
    }
    if (session.optionsOpen) {
        Dialog(onDismissRequest = { session.optionsOpen = false; persist() }) {
            Column(
                Modifier.width(520.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
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
                    MooButton(container.t("runtime.workingDirectory"), onClick = {
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
        Dialog(onDismissRequest = { session.historyOpen = false; persist() }) {
            Column(
                Modifier.width(560.dp).height(420.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("runtime.history"), color = colors.textPrimary)
                if (historyItems.isEmpty()) {
                    Text(container.t("time.history.empty"), color = colors.textSecondary)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(historyItems.size) { index ->
                            val item = historyItems[index]
                            Column(Modifier.fillMaxWidth().background(colors.sidebar, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(item.summary, color = colors.textPrimary, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MooButton(container.t("common.restore"), onClick = {
                                        session.editor(runtime).setText(item.input, recordUndo = false)
                                        session.historyOpen = false
                                        persist()
                                    })
                                    MooButton(container.t("common.delete"), onClick = {
                                        container.history.delete(item.id)
                                        historyItems = container.history.list(ToolId.Java.id)
                                    })
                                }
                            }
                        }
                    }
                }
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; persist() })
            }
        }
    }
}

private fun messageFor(container: AppContainer, code: CodeRunErrorCode, fallback: String): String {
    val key = "runtime.error.${code.name}"
    val localized = container.t(key)
    return if (localized == key) fallback.ifBlank { container.t("runtime.failed") } else localized
}
