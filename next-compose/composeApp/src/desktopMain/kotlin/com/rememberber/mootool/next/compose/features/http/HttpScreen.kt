package com.rememberber.mootool.next.compose.features.http

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DocumentFormatEngine
import com.rememberber.mootool.next.compose.domain.FindMatch
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.HttpCookie
import com.rememberber.mootool.next.compose.domain.HttpEngine
import com.rememberber.mootool.next.compose.domain.HttpErrorCode
import com.rememberber.mootool.next.compose.domain.HttpMethod
import com.rememberber.mootool.next.compose.domain.HttpPair
import com.rememberber.mootool.next.compose.domain.HttpProxyConfig
import com.rememberber.mootool.next.compose.domain.HttpRequestTab
import com.rememberber.mootool.next.compose.domain.HttpResponseFind
import com.rememberber.mootool.next.compose.domain.HttpResponseResult
import com.rememberber.mootool.next.compose.domain.HttpResponseTab
import com.rememberber.mootool.next.compose.editor.EditorAppShortcuts
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.HttpSession
import com.rememberber.mootool.next.compose.storage.SavedHttpRequest
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.HorizontalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.theme.toAwtColor
import com.rememberber.mootool.next.compose.ui.workbench.CopyFeedbackPolicy
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.UUID
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Composable
fun HttpScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.httpSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(emptyList<SavedHttpRequest>()) }

    fun persist() {
        session.syncBodyFromEditor()
        container.sessionManager.bump()
        container.sessionManager.persistHttp()
    }

    fun formatBody() {
        onEdt {
            val formatted = HttpEngine.formatBody(session.bodyEditor.text, session.bodyType)
            session.bodyEditor.setText(formatted, recordUndo = true)
            session.body = formatted
        }
    }

    fun reload() {
        items = container.httpCollections.list(session.query)
    }

    LaunchedEffect(session.query, revision) { reload() }
    var methodOpen by remember { mutableStateOf(false) }
    var bodyTypeOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }

    DisposableEffect(session) {
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                session.syncBodyFromEditor()
                persist()
            }
            override fun removeUpdate(e: DocumentEvent) {
                session.syncBodyFromEditor()
                persist()
            }
            override fun changedUpdate(e: DocumentEvent) = Unit
        }
        session.bodyEditor.document.addDocumentListener(listener)
        onDispose { session.bodyEditor.document.removeDocumentListener(listener) }
    }

    LaunchedEffect(session.copyState, session.copyGeneration) {
        if (session.copyState == CopyFeedbackPolicy.IDLE) return@LaunchedEffect
        delay(CopyFeedbackPolicy.RESET_MS)
        session.copyState = CopyFeedbackPolicy.IDLE
        persist()
    }

    fun copyResponse(payload: String) {
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(payload), null)
            session.error = ""
            session.notice = container.t("json.notice.copied")
            session.copyState = CopyFeedbackPolicy.afterCopy(true)
        } catch (_: Exception) {
            session.notice = ""
            session.error = container.t("json.notice.copyFailed")
            session.copyState = CopyFeedbackPolicy.afterCopy(false)
        }
        session.copyGeneration += 1
        persist()
    }

    fun saveResponse(visible: HttpResponseResult?) {
        val bytes = HttpEngine.downloadBytes(visible)
        if (bytes != null && session.responseTab == HttpResponseTab.Body) {
            val target = chooseSaveFile()
            if (target == null) {
                session.notice = container.t("common.cancel")
                persist()
                return
            }
            runCatching { target.writeBytes(bytes) }
                .onSuccess {
                    session.error = ""
                    session.notice = container.t("http.responseSaved", mapOf("path" to target.absolutePath))
                }
                .onFailure {
                    session.notice = ""
                    session.error = it.message ?: container.t("http.saveFailed")
                }
            persist()
            return
        }
        val payload = HttpResponseFind.payload(visible, session.responseTab)
        if (payload.isBlank()) {
            session.error = container.t("http.responseEmpty")
            persist()
            return
        }
        val target = chooseSaveFile()
        if (target == null) {
            session.notice = container.t("common.cancel")
            persist()
            return
        }
        runCatching { target.writeText(payload) }
            .onSuccess {
                session.error = ""
                session.notice = container.t("http.responseSaved", mapOf("path" to target.absolutePath))
            }
            .onFailure {
                session.notice = ""
                session.error = it.message ?: container.t("http.saveFailed")
            }
        persist()
    }

    fun send() {
        if (session.url.isBlank()) {
            session.error = container.t("http.urlRequired")
            persist()
            return
        }
        val timeout = HttpEngine.clampTimeout(session.timeoutMs)
        session.timeoutMs = timeout
        val requestId = "http-${UUID.randomUUID()}"
        session.previousResponse = HttpEngine.usableResponse(session.response, session.previousResponse)
        session.requestId = requestId
        session.sending = true
        session.error = ""
        session.notice = container.t("http.sending")
        persist()
        val draft = session.draft()
        val proxy = container.settings.value.network.let {
            HttpProxyConfig(it.proxyEnabled, it.proxyHost, it.proxyPort, it.proxyUsername, it.proxyPassword)
        }
        scope.launch(Dispatchers.IO) {
            val result = HttpEngine.send(draft, requestId, timeout, proxy)
            withContext(Dispatchers.Main) {
                if (session.requestId != requestId) return@withContext
                session.sending = false
                session.response = result
                session.notice = "${result.status} ${result.statusText} · ${result.durationMs} ms"
                session.error = result.errorCode?.let { messageFor(container, it, result.statusText) }.orEmpty()
                container.history.save(
                    ToolId.Http.id,
                    draft.method.name,
                    "${draft.method.name} ${draft.url}".trim(),
                    com.rememberber.mootool.next.compose.storage.HistoryPrivacy.httpUrl(draft.url),
                    result.body.take(8_000),
                    result.status.toString()
                )
                persist()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
    Column(Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val meta = event.isMetaPressed || event.isCtrlPressed
        when {
            meta && !event.isShiftPressed && !event.isAltPressed && event.key == Key.F -> {
                session.findOpen = true
                persist()
                true
            }
            meta && event.isShiftPressed && event.key == Key.F && session.requestTab == HttpRequestTab.Body -> {
                formatBody()
                persist()
                true
            }
            meta && event.key == Key.Enter -> {
                send()
                true
            }
            event.key == Key.Escape && session.findOpen -> {
                session.findOpen = false
                persist()
                true
            }
            else -> false
        }
    }) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).background(colors.toolbarBrush()).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            if (session.error.isNotEmpty()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            else if (session.notice.isNotEmpty()) Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; persist() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Http) })
                }
            )
        }
        Row(Modifier.fillMaxSize()) {
            val listWidth = settings.layout.pane(ToolId.Http.id, 0, 240f, 200f, 320f)
            CollectionPane(container, session, items, {
                session.loadDraft(it.draft)
                session.response = if (it.responseBody.isNotEmpty() || it.responseHeaders.isNotEmpty()) {
                    HttpResponseResult(
                        requestId = "saved",
                        ok = true,
                        status = 0,
                        statusText = "",
                        url = it.draft.url,
                        durationMs = 0,
                        body = it.responseBody,
                        headers = it.responseHeaders,
                        cookies = it.responseCookies
                    )
                } else null
                persist()
            }, { persist(); reload() }, listWidth)
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Http.id, 0, listWidth + it, 2) },
                onReset = { container.setPaneSize(ToolId.Http.id, 0, 240f, 2) }
            )
            Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        MooButton("${container.t("http.method")}: ${session.method.name}", onClick = { methodOpen = true })
                        DropdownMenu(expanded = methodOpen, onDismissRequest = { methodOpen = false }) {
                            HttpMethod.entries.forEach { method ->
                                DropdownMenuItem(onClick = {
                                    methodOpen = false
                                    session.method = method
                                    persist()
                                }) {
                                    Text(method.name)
                                }
                            }
                        }
                    }
                    MooTextField(session.url, { session.url = it; persist() }, modifier = Modifier.weight(1f), placeholder = "https://api.example.com")
                    MooTextField(session.timeoutMs.toString(), {
                        session.timeoutMs = it.toIntOrNull() ?: session.timeoutMs
                        persist()
                    }, modifier = Modifier.width(90.dp), placeholder = container.t("http.timeout"))
                    if (session.sending) {
                        MooButton(container.t("common.stop"), onClick = {
                            HttpEngine.cancel(session.requestId)
                            session.sending = false
                            session.notice = container.t("http.error.ABORTED")
                            persist()
                        })
                    } else {
                        MooButton(container.t("http.send"), primary = true, onClick = { send() })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HttpRequestTab.entries.forEach { tab ->
                        TabChip(container.t("http.tab.${tab.name.lowercase()}"), session.requestTab == tab) {
                            session.requestTab = tab
                            persist()
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (session.requestTab == HttpRequestTab.Body) {
                        MooButton(container.t("http.formatBody"), onClick = {
                            formatBody()
                            persist()
                        })
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (session.requestTab) {
                    HttpRequestTab.Params -> PairEditor(container, session.params, { session.params = it; persist() })
                    HttpRequestTab.Headers -> PairEditor(container, session.headers, { session.headers = it; persist() })
                    HttpRequestTab.Cookies -> CookieEditor(container, session.cookies, { session.cookies = it; persist() })
                    HttpRequestTab.Body -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(container.t("http.bodyType"), color = colors.textSecondary, fontSize = 12.sp)
                            Box {
                                MooButton(session.bodyType, onClick = { bodyTypeOpen = true })
                                DropdownMenu(expanded = bodyTypeOpen, onDismissRequest = { bodyTypeOpen = false }) {
                                    HttpEngine.BODY_TYPES.forEach { type ->
                                        DropdownMenuItem(onClick = {
                                            bodyTypeOpen = false
                                            session.bodyType = type
                                            session.bodyEditor.applySyntax(HttpEngine.syntaxForMime(type))
                                            persist()
                                        }) {
                                            Text(type)
                                        }
                                    }
                                }
                            }
                        }
                        EditorHost(
                            buffer = session.bodyEditor,
                            dark = MooTheme.dark,
                            fontName = DocumentFormatEngine.editorFont(settings.editor.jsonFontName),
                            fontSize = settings.editor.jsonFontSize,
                            wrap = settings.editor.softWrap,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            shortcuts = EditorAppShortcuts(
                                onFormat = {
                                    formatBody()
                                    persist()
                                },
                                onSend = { send() }
                            )
                        )
                    }
                }
                }
                val responseHeight = settings.layout.pane(ToolId.Http.id, 1, 220f, 120f, 480f)
                HorizontalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.Http.id, 1, responseHeight - it, 2) },
                    onReset = { container.setPaneSize(ToolId.Http.id, 1, 220f, 2) }
                )
                val showingPrevious = HttpEngine.showPreviousLabel(session.sending, session.response, session.previousResponse)
                val visible = HttpEngine.visibleResponse(session.sending, session.response, session.previousResponse)
                val payload = HttpResponseFind.payload(visible, session.responseTab)
                val matches = if (session.findOpen && payload.isNotEmpty()) {
                    FindReplace.findAll(payload, session.findQuery, session.findOptions)
                } else {
                    emptyList()
                }
                val currentIndex = if (matches.isEmpty()) 0 else session.findIndex.coerceIn(0, matches.lastIndex)
                LaunchedEffect(
                    payload,
                    session.responseTab,
                    visible?.requestId,
                    session.findOpen,
                    session.findQuery,
                    session.findOptions,
                    session.findIndex
                ) {
                    onEdt {
                        session.responseEditor.setEditable(false)
                        session.responseEditor.applySyntax(HttpEngine.syntaxForResponse(visible, session.responseTab))
                        if (session.responseEditor.text != payload) {
                            session.responseEditor.setText(payload, recordUndo = false)
                        }
                        if (session.findOpen && payload.isNotEmpty() && matches.isNotEmpty()) {
                            session.responseEditor.markMatches(
                                HttpResponseFind.spans(payload.length, matches, currentIndex).map { Triple(it.start, it.end, it.current) },
                                colors.selected.toAwtColor(),
                                colors.accent.copy(alpha = 0.45f).toAwtColor()
                            )
                            val match = matches[currentIndex]
                            session.responseEditor.select(match.start, match.end)
                        } else {
                            session.responseEditor.clearMatches()
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().height(responseHeight.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showingPrevious) container.t("http.previousResponse") else container.t("http.response"),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    HttpResponseTab.entries.forEach { tab ->
                        TabChip(container.t("http.response.${tab.name.lowercase()}"), session.responseTab == tab) {
                            session.responseTab = tab
                            persist()
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    MooButton(
                        container.t("http.find"),
                        primary = session.findOpen,
                        onClick = {
                            session.findOpen = !session.findOpen
                            persist()
                        }
                    )
                    MooButton(
                        container.t(CopyFeedbackPolicy.buttonKey(session.copyState, "common.action.copy")),
                        enabled = visible != null && !session.sending,
                        onClick = { copyResponse(payload) }
                    )
                    if (!overflow) {
                        MooButton(
                            if (visible?.binary == true && session.responseTab == HttpResponseTab.Body) {
                                container.t("http.saveBinary")
                            } else {
                                container.t("http.saveResponse")
                            },
                            enabled = visible != null && !session.sending,
                            onClick = { saveResponse(visible) }
                        )
                    } else {
                        Box {
                            MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true })
                            DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                                DropdownMenuItem(
                                    onClick = {
                                        moreOpen = false
                                        saveResponse(visible)
                                    },
                                    enabled = visible != null && !session.sending
                                ) {
                                    Text(
                                        if (visible?.binary == true && session.responseTab == HttpResponseTab.Body) {
                                            container.t("http.saveBinary")
                                        } else {
                                            container.t("http.saveResponse")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (session.findOpen) {
                    HttpFindBar(container, session, matches, currentIndex) { persist() }
                }
                if (visible != null) {
                    Text("${visible.status} · ${visible.durationMs} ms · ${visible.url}", color = colors.textSecondary, fontSize = 11.sp)
                    if (visible.binary && session.responseTab == HttpResponseTab.Body) {
                        Text(
                            container.t("http.binaryHint", mapOf("size" to (visible.bodyBytes?.size ?: 0).toString())),
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                val placeholder = container.t("http.responseEmpty")
                Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(8.dp))) {
                    EditorHost(
                        buffer = session.responseEditor,
                        dark = MooTheme.dark,
                        fontName = DocumentFormatEngine.editorFont(settings.editor.jsonFontName),
                        fontSize = settings.editor.jsonFontSize,
                        wrap = settings.editor.softWrap,
                        modifier = Modifier.fillMaxSize(),
                        shortcuts = EditorAppShortcuts(
                            onFind = {
                                session.findOpen = true
                                persist()
                            }
                        )
                    )
                    if (payload.isEmpty()) {
                        Text(
                            placeholder,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                }
            }
        }
    }
    }
    if (session.curlOpen) CurlDialog(container, session) { persist() }
    if (session.saveOpen) SaveDialog(container, session) { persist(); reload() }
    if (session.deleteConfirm) DeleteDialog(container, session) { persist(); reload() }
    if (session.historyOpen) HistoryBrowser(
        container = container,
        toolId = ToolId.Http.id,
        title = container.t("http.history"),
        onRestore = { item ->
            session.url = item.input
            HttpMethod.entries.find { it.name == item.operation }?.let { session.method = it }
            session.historyOpen = false
            persist()
        },
        onDismiss = { session.historyOpen = false; persist() }
    )
}

@Composable
private fun CollectionPane(
    container: AppContainer,
    session: HttpSession,
    items: List<SavedHttpRequest>,
    onSelect: (SavedHttpRequest) -> Unit,
    onChanged: () -> Unit,
    width: Float
) {
    val colors = MooTheme.colors
    Column(
        Modifier.width(width.dp).fillMaxHeight().background(colors.sidebar).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(session.query, { session.query = it; onChanged() }, placeholder = container.t("common.search"))
        MooButton(container.t("common.new"), onClick = {
            session.loadDraft(HttpEngine.emptyDraft(container.t("http.untitled")))
            session.response = null
            session.previousResponse = null
            onChanged()
        })
        if (items.isEmpty()) {
            Text(container.t("http.savedEmpty"), color = colors.textSecondary, fontSize = 12.sp)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (item.id == session.selectedId) colors.selected else colors.sidebar)
                            .clickable { onSelect(item) }.padding(8.dp)
                    ) {
                        Text(item.draft.name, color = colors.textPrimary, fontSize = 13.sp)
                        Text("${item.draft.method.name} ${item.draft.url.ifBlank { container.t("http.noUrl") }}", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("http.importCurl"), onClick = { session.curlOpen = true; onChanged() })
            MooButton(container.t("http.copyCurl"), onClick = {
                runCatching {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(HttpEngine.toCurl(session.draft())), null)
                    session.notice = container.t("common.copied")
                    onChanged()
                }
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("common.save"), onClick = {
                session.saveName = session.name.ifBlank { container.t("http.untitled") }
                session.saveOpen = true
                onChanged()
            })
            MooButton(container.t("common.delete"), onClick = { session.deleteConfirm = true; onChanged() }, enabled = session.selectedId.isNotBlank())
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MooTheme.colors
    Text(
        label,
        color = if (selected) colors.onAccent else colors.textPrimary,
        fontSize = 11.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.accent else colors.control)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun PairEditor(container: AppContainer, items: List<HttpPair>, onChange: (List<HttpPair>) -> Unit) {
    val colors = MooTheme.colors
    Column(Modifier.fillMaxWidth().height(180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MooButton(container.t("http.addEntry"), onClick = { onChange(items + HttpEngine.pair()) })
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TabChip(if (item.enabled) "on" else "off", item.enabled) {
                        onChange(items.toMutableList().also { it[index] = item.copy(enabled = !item.enabled) })
                    }
                    MooTextField(item.name, { value -> onChange(items.toMutableList().also { it[index] = item.copy(name = value) }) }, modifier = Modifier.weight(1f), placeholder = container.t("http.name"))
                    MooTextField(item.value, { value -> onChange(items.toMutableList().also { it[index] = item.copy(value = value) }) }, modifier = Modifier.weight(1f), placeholder = container.t("http.value"))
                    MooButton(container.t("common.delete"), onClick = { onChange(items.filterNot { it.id == item.id }) })
                }
            }
        }
        Text(container.t("http.duplicateHint"), color = colors.textSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun CookieEditor(container: AppContainer, items: List<HttpCookie>, onChange: (List<HttpCookie>) -> Unit) {
    Column(Modifier.fillMaxWidth().height(180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MooButton(container.t("http.addEntry"), onClick = { onChange(items + HttpEngine.cookie()) })
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TabChip(if (item.enabled) "on" else "off", item.enabled) {
                        onChange(items.toMutableList().also { it[index] = item.copy(enabled = !item.enabled) })
                    }
                    MooTextField(item.name, { value -> onChange(items.toMutableList().also { it[index] = item.copy(name = value) }) }, modifier = Modifier.width(90.dp), placeholder = container.t("http.name"))
                    MooTextField(item.value, { value -> onChange(items.toMutableList().also { it[index] = item.copy(value = value) }) }, modifier = Modifier.width(90.dp), placeholder = container.t("http.value"))
                    MooTextField(item.domain, { value -> onChange(items.toMutableList().also { it[index] = item.copy(domain = value) }) }, modifier = Modifier.width(80.dp), placeholder = container.t("http.domain"))
                    MooTextField(item.path, { value -> onChange(items.toMutableList().also { it[index] = item.copy(path = value) }) }, modifier = Modifier.width(70.dp), placeholder = container.t("http.path"))
                    MooTextField(item.expires, { value -> onChange(items.toMutableList().also { it[index] = item.copy(expires = value) }) }, modifier = Modifier.width(80.dp), placeholder = container.t("http.expires"))
                    MooButton(container.t("common.delete"), onClick = { onChange(items.filterNot { it.id == item.id }) })
                }
            }
        }
    }
}

@Composable
private fun CurlDialog(container: AppContainer, session: HttpSession, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.curlOpen = false; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(320.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.curlPrompt"), color = MooTheme.colors.textPrimary)
            MooTextField(session.curlValue, { session.curlValue = it; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("http.importCurl"), primary = true, onClick = {
                    runCatching { HttpEngine.parseCurl(session.curlValue) }
                        .onSuccess {
                            session.loadDraft(it)
                            session.response = null
                            session.curlOpen = false
                            session.curlValue = ""
                            session.error = ""
                        }
                        .onFailure { session.error = it.message ?: container.t("http.error.INVALID_REQUEST") }
                    onChanged()
                })
                MooButton(container.t("common.cancel"), onClick = { session.curlOpen = false; onChanged() })
            }
        }
    }
}

@Composable
private fun SaveDialog(container: AppContainer, session: HttpSession, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.saveOpen = false; onChanged() }) {
        Column(
            Modifier.width(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.saveName"), color = MooTheme.colors.textPrimary)
            MooTextField(session.saveName, { session.saveName = it; onChanged() })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.save"), primary = true, onClick = {
                    val name = session.saveName.trim()
                    if (name.isEmpty()) return@MooButton
                    val saved = container.httpCollections.save(session.draft().copy(name = name), session.response)
                    session.loadDraft(saved.draft)
                    session.saveOpen = false
                    session.notice = container.t("common.save")
                    onChanged()
                })
                MooButton(container.t("common.cancel"), onClick = { session.saveOpen = false; onChanged() })
            }
        }
    }
}

@Composable
private fun DeleteDialog(container: AppContainer, session: HttpSession, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.deleteConfirm = false; onChanged() }) {
        Column(
            Modifier.width(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.confirmDelete"), color = MooTheme.colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.delete"), primary = true, onClick = {
                    if (session.selectedId.isNotBlank()) container.httpCollections.delete(session.selectedId)
                    session.loadDraft(HttpEngine.emptyDraft(container.t("http.untitled")))
                    session.response = null
                    session.deleteConfirm = false
                    onChanged()
                })
                MooButton(container.t("common.cancel"), onClick = { session.deleteConfirm = false; onChanged() })
            }
        }
    }
}

@Composable
private fun HttpFindBar(
    container: AppContainer,
    session: HttpSession,
    matches: List<FindMatch>,
    currentIndex: Int,
    onChanged: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MooTheme.colors.surfaceSubtle).horizontalScroll(rememberScrollState()).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(
            session.findQuery,
            {
                session.findQuery = it
                session.findIndex = 0
                onChanged()
            },
            modifier = Modifier.width(220.dp),
            placeholder = container.t("http.findPlaceholder")
        )
        MooButton(container.t("find.matchCase") + ": ${session.findOptions.matchCase}", onClick = {
            session.findOptions = session.findOptions.copy(matchCase = !session.findOptions.matchCase)
            session.findIndex = 0
            onChanged()
        })
        MooButton(container.t("find.wholeWord") + ": ${session.findOptions.wholeWord}", onClick = {
            session.findOptions = session.findOptions.copy(wholeWord = !session.findOptions.wholeWord)
            session.findIndex = 0
            onChanged()
        })
        MooButton(container.t("find.regex") + ": ${session.findOptions.regex}", onClick = {
            session.findOptions = session.findOptions.copy(regex = !session.findOptions.regex)
            session.findIndex = 0
            onChanged()
        })
        Text(container.t("json.find.matches", mapOf("count" to matches.size.toString())), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
        MooButton(container.t("find.previous"), onClick = {
            session.findIndex = HttpResponseFind.nextIndex(matches.size, currentIndex, forward = false)
            onChanged()
        })
        MooButton(container.t("find.next"), onClick = {
            session.findIndex = HttpResponseFind.nextIndex(matches.size, currentIndex, forward = true)
            onChanged()
        })
        MooButton(container.t("common.close"), onClick = {
            session.findOpen = false
            onChanged()
        })
    }
}

private fun onEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}

private fun chooseSaveFile(): File? {
    val dialog = FileDialog(null as Frame?, "", FileDialog.SAVE)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}

private fun messageFor(container: AppContainer, code: HttpErrorCode, raw: String): String {
    val localized = container.t("http.error.${code.name}")
    return if (raw.isNotBlank() && raw != code.name) "$localized ($raw)" else localized
}
