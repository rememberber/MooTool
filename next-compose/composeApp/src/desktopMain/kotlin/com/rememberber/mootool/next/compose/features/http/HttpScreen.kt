package com.rememberber.mootool.next.compose.features.http

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DocumentFormatEngine
import com.rememberber.mootool.next.compose.domain.EditorSettingsLiveApply
import com.rememberber.mootool.next.compose.domain.FindMatch
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.HttpCookie
import com.rememberber.mootool.next.compose.domain.HttpEngine
import com.rememberber.mootool.next.compose.domain.HttpHistoryMetadata
import com.rememberber.mootool.next.compose.domain.HttpHistoryRestore
import com.rememberber.mootool.next.compose.domain.HttpErrorCode
import com.rememberber.mootool.next.compose.domain.HttpMethod
import com.rememberber.mootool.next.compose.domain.HttpPair
import com.rememberber.mootool.next.compose.domain.toHttpProxyConfig
import com.rememberber.mootool.next.compose.domain.HttpRequestTab
import com.rememberber.mootool.next.compose.domain.HttpResponseFind
import com.rememberber.mootool.next.compose.domain.HttpRequestPresentation
import com.rememberber.mootool.next.compose.domain.HttpResponsePresentation
import com.rememberber.mootool.next.compose.domain.HttpResponseResult
import com.rememberber.mootool.next.compose.domain.HttpResponseTab
import com.rememberber.mootool.next.compose.domain.HttpTimeoutSettings
import com.rememberber.mootool.next.compose.editor.EditorAppShortcuts
import com.rememberber.mootool.next.compose.editor.EditorFindShortcutPolicy
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.editor.EditorFindHighlight
import com.rememberber.mootool.next.compose.editor.onFindBarRowKeys
import com.rememberber.mootool.next.compose.editor.onFindQueryEnterKey
import com.rememberber.mootool.next.compose.editor.openFindBarSeedingSelection
import com.rememberber.mootool.next.compose.editor.RstaFindNavigation
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.HttpSession
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.storage.SavedHttpRequest
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.HorizontalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooCompactSearch
import com.rememberber.mootool.next.compose.ui.components.MooGhostButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.mooEditorFrame
import com.rememberber.mootool.next.compose.ui.components.mooHttpEntryRow
import com.rememberber.mootool.next.compose.ui.components.mooHttpRequestPane
import com.rememberber.mootool.next.compose.ui.components.mooHttpResponsePane
import com.rememberber.mootool.next.compose.ui.components.mooHttpSavedItem
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooToolTabsBackground
import com.rememberber.mootool.next.compose.ui.components.mooFindBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.MooTooltip
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.theme.toAwtColor
import com.rememberber.mootool.next.compose.ui.workbench.CopyFeedbackPolicy
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.OnToolLeaveUnlessDetached
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.UUID
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Composable
fun HttpScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.httpSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
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
            val formatted = HttpEngine.formatBody(
                session.bodyEditor.text,
                session.bodyType,
                sqlDialect = settings.editor.sqlDialect,
                indent = 2,
            )
            session.bodyEditor.setText(formatted, recordUndo = true)
            session.body = formatted
        }
    }

    fun reload() {
        items = container.httpCollections.list(session.query)
    }

    LaunchedEffect(settings.network.requestTimeoutMs) {
        val fromSettings = HttpTimeoutSettings.clamp(settings.network.requestTimeoutMs)
        if (session.timeoutMs != fromSettings) {
            session.timeoutMs = fromSettings
            persist()
        }
    }

    LaunchedEffect(session.query, revision) { reload() }
    LaunchedEffect(settings.data.directory, sessionGeneration) {
        reload()
        if (session.selectedId.isNotBlank() && items.none { it.id == session.selectedId }) {
            session.selectedId = ""
        }
        persist()
    }
    var methodOpen by remember { mutableStateOf(false) }
    var bodyTypeOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }

    DisposableEffect(session.bodyEditor) {
        session.bodyEditor.onUserDocumentChange = {
            session.onUserInput { }
            persist()
        }
        onDispose { session.bodyEditor.onUserDocumentChange = null }
    }
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
    DismissModalOverlaysOnDispose(container, ToolId.Http) { session.dismissModalOverlays() }
    EditorFindHighlight.ClearOnDispose(session.responseEditor)

    LaunchedEffect(session.copyState, session.copyGeneration) {
        if (session.copyState == CopyFeedbackPolicy.IDLE) return@LaunchedEffect
        delay(CopyFeedbackPolicy.RESET_MS)
        session.copyState = CopyFeedbackPolicy.IDLE
        persist()
    }

    fun copyResponse(payload: String) {
        if (container.copyText(payload)) {
            session.error = ""
            session.notice = container.t("json.notice.copied")
            session.copyState = CopyFeedbackPolicy.afterCopy(true)
        } else {
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
                    val savedNotice = container.t("http.responseSaved", mapOf("path" to target.absolutePath))
                    session.notice = savedNotice
                    container.toastSuccess(savedNotice)
                }
                .onFailure {
                    session.notice = ""
                    session.error = it.message ?: container.t("http.saveFailed")
                    container.toastError(session.error)
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
                val savedNotice = container.t("http.responseSaved", mapOf("path" to target.absolutePath))
                session.notice = savedNotice
                container.toastSuccess(savedNotice)
            }
            .onFailure {
                session.notice = ""
                session.error = it.message ?: container.t("http.saveFailed")
                container.toastError(session.error)
            }
        persist()
    }

    fun send() {
        if (session.url.isBlank()) {
            session.error = container.t("http.urlRequired")
            persist()
            return
        }
        val timeoutResult = HttpTimeoutSettings.commit(session.timeoutMs, settings.network.requestTimeoutMs)
        session.timeoutMs = timeoutResult.sessionMs
        if (timeoutResult.updateGlobal) {
            container.updateSettings { current ->
                current.copy(network = current.network.copy(requestTimeoutMs = timeoutResult.sessionMs))
            }
        }
        val timeout = timeoutResult.sessionMs
        val requestId = "http-${UUID.randomUUID()}"
        session.previousResponse = HttpRequestPresentation.previousResponseBeforeSend(
            session.response,
            session.previousResponse,
        )
        session.requestId = requestId
        session.sending = true
        session.error = ""
        session.notice = container.t("http.sending")
        persist()
        val draft = session.draft()
        val proxy = container.settings.value.network.toHttpProxyConfig()
        scope.launch(Dispatchers.IO) {
            val result = HttpEngine.send(draft, requestId, timeout, proxy)
            withContext(Dispatchers.Main) {
                if (!HttpRequestPresentation.shouldApplyResponse(session.requestId, result.requestId)) return@withContext
                session.sending = false
                session.response = result
                session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
                session.notice = "${result.status} ${result.statusText} · ${result.durationMs} ms"
                session.error = result.errorCode?.let { messageFor(container, it, result.statusText) }.orEmpty()
                container.history.save(
                    ToolId.Http.id,
                    draft.method.name,
                    "${draft.method.name} ${draft.url}".trim(),
                    com.rememberber.mootool.next.compose.storage.HistoryPrivacy.httpUrl(draft.url),
                    result.body.take(8_000),
                    HttpHistoryMetadata.encodeStatus(result.status)
                )
                persist()
            }
        }
    }

    fun cancelInFlightSend() {
        if (!session.sending || session.requestId.isBlank()) return
        HttpEngine.cancel(session.requestId)
        session.sending = false
        session.requestId = ""
    }

    fun commitTimeout(andSend: Boolean = false) {
        val result = HttpTimeoutSettings.commit(session.timeoutMs, settings.network.requestTimeoutMs)
        session.timeoutMs = result.sessionMs
        if (result.updateGlobal) {
            container.updateSettings { current ->
                current.copy(network = current.network.copy(requestTimeoutMs = result.sessionMs))
            }
        }
        persist()
        if (andSend && !session.sending) send()
    }

    OnToolLeaveUnlessDetached(container, ToolId.Http) { cancelInFlightSend() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
    Column(Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return@onPreviewKeyEvent false
        val meta = event.isMetaPressed || event.isCtrlPressed
        when {
            EditorFindShortcutPolicy.opensShellFind(
                ToolId.Http,
                event.key,
                meta = meta,
                shift = event.isShiftPressed,
                alt = event.isAltPressed,
            ) -> {
                if (!HttpFindShortcutPolicy.shouldOpenResponseFindFromShell(session)) {
                    false
                } else {
                    openHttpResponseFind(session) { persist() }
                    true
                }
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
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("http.title"))
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
            val listWidth = settings.layout.pane(ToolId.Http.id, 0, 210f, 180f, 320f)
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
                session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
                persist()
            }, { persist(); reload() }, listWidth)
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Http.id, 0, listWidth + it, 2) },
                onReset = { container.setPaneSize(ToolId.Http.id, 0, 210f, 2) }
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .mooToolShell(p5 = true, endBorder = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        MooButton("${container.t("http.method")}: ${session.method.name}", onClick = { methodOpen = true }, p5Toolbar = true)
                        MooMenu(expanded = methodOpen, onDismissRequest = { methodOpen = false }) {
                            HttpMethod.entries.forEach { method ->
                                MooMenuItem(onClick = {
                                    methodOpen = false
                                    session.method = method
                                    persist()
                                }) {
                                    Text(method.name)
                                }
                            }
                        }
                    }
                    MooTextField(
                        session.url,
                        { session.onUserInput { session.url = it; persist() } },
                        modifier = Modifier.weight(1f),
                        placeholder = "https://api.example.com",
                        dense = true,
                        fieldModifier = Modifier.onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || event.key != Key.Enter) return@onPreviewKeyEvent false
                            if (event.isMetaPressed || event.isCtrlPressed) return@onPreviewKeyEvent false
                            if (!session.sending) send()
                            true
                        }
                    )
                    MooTooltip(container.t("http.timeoutHint")) {
                        MooTextField(
                            session.timeoutMs.toString(),
                            {
                                session.timeoutMs = it.toIntOrNull() ?: session.timeoutMs
                                persist()
                            },
                            modifier = Modifier.width(90.dp),
                            placeholder = container.t("http.timeout"),
                            dense = true,
                            fieldModifier = Modifier
                                .onFocusChanged { if (!it.isFocused) commitTimeout() }
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown || event.key != Key.Enter) return@onPreviewKeyEvent false
                                    commitTimeout(andSend = true)
                                    true
                                }
                        )
                    }
                    if (session.sending) {
                        MooButton(
                            container.t("common.stop"),
                            onClick = {
                                HttpEngine.cancel(session.requestId)
                                session.sending = false
                                session.notice = container.t("http.error.ABORTED")
                                persist()
                            },
                            p5Toolbar = true
                        )
                    } else {
                        MooButton(container.t("http.send"), prominent = true, onClick = { send() }, p5Toolbar = true)
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .mooHttpRequestPane()
                        .focusGroup()
                        .onFocusChanged { session.requestPaneComposeFocused = it.hasFocus },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth().mooToolTabsBackground().padding(start = 10.dp, end = 10.dp, top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    HttpRequestTab.entries.forEach { tab ->
                        MooToolTab(container.t("http.tab.${tab.name.lowercase()}"), selected = session.requestTab == tab, onClick = {
                            session.requestTab = tab
                            persist()
                        })
                    }
                    Spacer(Modifier.weight(1f))
                }
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (session.requestTab) {
                    HttpRequestTab.Params -> PairEditor(container, session.params, {
                        session.onUserInput { session.params = it }
                        persist()
                    })
                    HttpRequestTab.Headers -> PairEditor(container, session.headers, {
                        session.onUserInput { session.headers = it }
                        persist()
                    })
                    HttpRequestTab.Cookies -> CookieEditor(container, session.cookies, {
                        session.onUserInput { session.cookies = it }
                        persist()
                    })
                    HttpRequestTab.Body -> {
                        Column(Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().height(37.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(container.t("http.bodyType"), color = colors.textMuted, fontSize = 10.sp)
                            Box {
                                MooButton(session.bodyType, onClick = { bodyTypeOpen = true }, p5Toolbar = true)
                                MooMenu(expanded = bodyTypeOpen, onDismissRequest = { bodyTypeOpen = false }) {
                                    HttpEngine.BODY_TYPES.forEach { type ->
                                        MooMenuItem(onClick = {
                                            bodyTypeOpen = false
                                            session.bodyType = type
                                            session.bodyEditor.applySyntax(HttpEngine.syntaxForMime(type))
                                            persist()
                                        }) {
                                            Text(type, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            MooButton(
                                container.t("http.formatBody"),
                                onClick = {
                                    formatBody()
                                    persist()
                                },
                                p5Toolbar = true
                            )
                        }
                        EditorHost(
                            buffer = session.bodyEditor,
                            dark = MooTheme.dark,
                            fontName = DocumentFormatEngine.editorFont(settings.editor.jsonFontName),
                            fontSize = EditorSettingsLiveApply.jsonEditorFontSize(settings.editor.jsonFontSize),
                            wrap = EditorSettingsLiveApply.httpEditorWrap(settings.editor.softWrap),
                            modifier = Modifier.weight(1f).fillMaxWidth().mooEditorFrame(flatten = true),
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
                }
                }
                val responseHeight = settings.layout.pane(ToolId.Http.id, 1, 220f, 120f, 480f)
                HorizontalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.Http.id, 1, responseHeight - it, 2) },
                    onReset = { container.setPaneSize(ToolId.Http.id, 1, 220f, 2) }
                )
                val showingPrevious = HttpResponsePresentation.showPreviousLabel(
                    session.sending,
                    session.response,
                    session.previousResponse,
                )
                val visible = HttpResponsePresentation.visibleResponse(
                    session.sending,
                    session.response,
                    session.previousResponse,
                )
                val payload = HttpResponseFind.payload(visible, session.responseTab)
                val matches = if (session.findOpen && payload.isNotEmpty()) {
                    FindReplace.findAll(payload, session.findQuery, session.findOptions)
                } else {
                    emptyList()
                }
                val currentIndex = when {
                    matches.isEmpty() -> 0
                    session.findIndex < 0 -> HttpResponseFind.FIND_INDEX_UNSET
                    else -> session.findIndex.coerceIn(0, matches.lastIndex)
                }
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
                            if (currentIndex >= 0) {
                                val match = matches[currentIndex]
                                session.responseEditor.select(match.start, match.end)
                            }
                        } else {
                            session.responseEditor.clearMatches()
                        }
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(responseHeight.dp)
                        .mooHttpResponsePane(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showingPrevious) container.t("http.previousResponse") else container.t("http.response"),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    HttpResponseTab.entries.forEach { tab ->
                        MooToolTab(container.t("http.response.${tab.name.lowercase()}"), selected = session.responseTab == tab, onClick = {
                            session.responseTab = tab
                            session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
                            persist()
                        })
                    }
                    Spacer(Modifier.weight(1f))
                    MooButton(
                        container.t("http.find"),
                        primary = session.findOpen,
                        onClick = {
                            if (session.findOpen) {
                                session.findOpen = false
                                persist()
                            } else {
                                openHttpResponseFind(session) { persist() }
                            }
                        },
                        p5Toolbar = true
                    )
                    MooButton(
                        container.t(CopyFeedbackPolicy.buttonKey(session.copyState, "common.action.copy")),
                        enabled = visible != null && !session.sending,
                        onClick = { copyResponse(payload) },
                        p5Toolbar = true
                    )
                    if (!overflow) {
                        MooButton(
                            if (visible?.binary == true && session.responseTab == HttpResponseTab.Body) {
                                container.t("http.saveBinary")
                            } else {
                                container.t("http.saveResponse")
                            },
                            enabled = visible != null && !session.sending,
                            onClick = { saveResponse(visible) },
                            p5Toolbar = true
                        )
                    } else {
                        Box {
                            MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true }, p5Toolbar = true)
                            MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                                MooMenuItem(
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
                Box(Modifier.weight(1f).fillMaxWidth().mooEditorFrame(flatten = true)) {
                    EditorHost(
                        buffer = session.responseEditor,
                        dark = MooTheme.dark,
                        fontName = DocumentFormatEngine.editorFont(settings.editor.jsonFontName),
                        fontSize = EditorSettingsLiveApply.jsonEditorFontSize(settings.editor.jsonFontSize),
                        wrap = EditorSettingsLiveApply.httpEditorWrap(settings.editor.softWrap),
                        modifier = Modifier.fillMaxSize(),
                        shortcuts = EditorAppShortcuts(
                            onFind = { openHttpResponseFind(session) { persist() } }
                        )
                    )
                    if (payload.isEmpty()) {
                        Text(
                            placeholder,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
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
            HttpHistoryRestore.apply(session, item)
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
        Modifier.width(width.dp).fillMaxHeight().mooToolShell(colors.sidebar, flatten = true)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MooCompactSearch(session.query, { session.query = it; onChanged() }, placeholder = container.t("common.search"), modifier = Modifier.weight(1f))
        }
        MooButton(container.t("common.new"), onClick = {
            session.loadDraft(HttpEngine.emptyDraft(container.t("http.untitled")))
            session.response = null
            session.previousResponse = null
            onChanged()
        }, modifier = Modifier.padding(horizontal = 7.dp))
        if (items.isEmpty()) {
            Text(container.t("http.savedEmpty"), color = colors.textSecondary, fontSize = 12.sp)
        } else {
            LazyColumn(Modifier.weight(1f).padding(5.dp)) {
                itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                    val interaction = remember(item.id) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val active = item.id == session.selectedId
                    val shape = RoundedCornerShape(5.dp)
                    Column(
                        Modifier.fillMaxWidth()
                            .mooHttpSavedItem(active = active, hovered = hovered)
                            .hoverable(interaction)
                            .mooFocusClickable(shape = shape) { onSelect(item) }
                            .padding(horizontal = 9.dp, vertical = 8.dp)
                    ) {
                        Text(
                            item.draft.name,
                            color = colors.textBody,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${item.draft.method.name} ${item.draft.url.ifBlank { container.t("http.noUrl") }}",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 7.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End)
        ) {
            MooButton(container.t("http.importCurl"), onClick = { session.curlOpen = true; onChanged() }, p5Toolbar = true)
            MooButton(container.t("http.copyCurl"), onClick = {
                if (container.copyText(HttpEngine.toCurl(session.draft()))) {
                    session.notice = container.t("common.copied")
                } else {
                    session.notice = container.t("json.notice.copyFailed")
                }
                onChanged()
            }, p5Toolbar = true)
            MooButton(container.t("common.save"), onClick = {
                session.saveName = session.name.ifBlank { container.t("http.untitled") }
                session.saveOpen = true
                onChanged()
            }, p5Toolbar = true)
            MooButton(
                container.t("common.delete"),
                onClick = { session.deleteConfirm = true; onChanged() },
                enabled = session.selectedId.isNotBlank(),
                p5Toolbar = true
            )
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MooTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Text(
        label,
        color = if (selected) colors.onAccent else colors.textPrimary,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.accent else colors.control)
            .mooFocusClickable(shape = shape, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun PairEditor(container: AppContainer, items: List<HttpPair>, onChange: (List<HttpPair>) -> Unit) {
    val colors = MooTheme.colors
    Column(Modifier.fillMaxWidth().height(200.dp)) {
        Row(
            Modifier.fillMaxWidth().height(35.dp).background(colors.workspace).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Spacer(Modifier.width(22.dp))
            Text(container.t("http.name"), color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
            Text(container.t("http.value"), color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
            Spacer(Modifier.width(30.dp))
        }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                Row(
                    Modifier.fillMaxWidth().mooHttpEntryRow().padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        item.enabled,
                        { checked -> onChange(items.toMutableList().also { it[index] = item.copy(enabled = checked) }) },
                        modifier = Modifier.size(22.dp)
                    )
                    MooTextField(
                        item.name,
                        { value -> onChange(items.toMutableList().also { it[index] = item.copy(name = value) }) },
                        modifier = Modifier.weight(0.8f),
                        placeholder = container.t("http.name"),
                        compact = true
                    )
                    MooTextField(
                        item.value,
                        { value -> onChange(items.toMutableList().also { it[index] = item.copy(value = value) }) },
                        modifier = Modifier.weight(1.2f),
                        placeholder = container.t("http.value"),
                        compact = true
                    )
                    MooGhostButton(container.t("common.delete"), onClick = { onChange(items.filterNot { it.id == item.id }) }, size = 28.dp) {
                        Text("×", color = colors.textMuted, fontSize = 16.sp)
                    }
                }
            }
        }
        Text(
            container.t("http.addEntry"),
            color = colors.textMuted,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.control)
                .mooFocusClickable(shape = RoundedCornerShape(6.dp)) { onChange(items + HttpEngine.pair()) }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        )
        Text(container.t("http.duplicateHint"), color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

@Composable
private fun CookieEditor(container: AppContainer, items: List<HttpCookie>, onChange: (List<HttpCookie>) -> Unit) {
    val colors = MooTheme.colors
    Column(Modifier.fillMaxWidth().height(200.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(35.dp).background(colors.workspace).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Spacer(Modifier.width(22.dp))
            Text(container.t("http.name"), color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.width(100.dp))
            Text(container.t("http.value"), color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.width(130.dp))
            Text(container.t("http.domain"), color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.width(100.dp))
            Text(container.t("http.path"), color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.width(80.dp))
            Text(container.t("http.expires"), color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.width(110.dp))
            Spacer(Modifier.width(30.dp))
        }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).heightIn(min = 35.dp).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        item.enabled,
                        { checked -> onChange(items.toMutableList().also { it[index] = item.copy(enabled = checked) }) },
                        modifier = Modifier.size(22.dp)
                    )
                    MooTextField(item.name, { value -> onChange(items.toMutableList().also { it[index] = item.copy(name = value) }) }, modifier = Modifier.width(100.dp), placeholder = container.t("http.name"), compact = true)
                    MooTextField(item.value, { value -> onChange(items.toMutableList().also { it[index] = item.copy(value = value) }) }, modifier = Modifier.width(130.dp), placeholder = container.t("http.value"), compact = true)
                    MooTextField(item.domain, { value -> onChange(items.toMutableList().also { it[index] = item.copy(domain = value) }) }, modifier = Modifier.width(100.dp), placeholder = container.t("http.domain"), compact = true)
                    MooTextField(item.path, { value -> onChange(items.toMutableList().also { it[index] = item.copy(path = value) }) }, modifier = Modifier.width(80.dp), placeholder = container.t("http.path"), compact = true)
                    MooTextField(item.expires, { value -> onChange(items.toMutableList().also { it[index] = item.copy(expires = value) }) }, modifier = Modifier.width(110.dp), placeholder = container.t("http.expires"), compact = true)
                    MooGhostButton(container.t("common.delete"), onClick = { onChange(items.filterNot { it.id == item.id }) }, size = 28.dp) {
                        Text("×", color = colors.textMuted, fontSize = 16.sp)
                    }
                }
            }
        }
        Text(
            container.t("http.addEntry"),
            color = colors.textMuted,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.control)
                .mooFocusClickable(shape = RoundedCornerShape(6.dp)) { onChange(items + HttpEngine.cookie()) }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun CurlDialog(container: AppContainer, session: HttpSession, onChanged: () -> Unit) {
    MooOverlay(onDismiss = { session.curlOpen = false; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(320.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.curlPrompt"), color = MooTheme.colors.textPrimary)
            MooTextField(session.curlValue, { session.curlValue = it; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("http.importCurl"), prominent = true, onClick = {
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
    MooOverlay(onDismiss = { session.saveOpen = false; onChanged() }) {
        Column(
            Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.saveName"), color = MooTheme.colors.textPrimary)
            MooTextField(session.saveName, { session.saveName = it; onChanged() })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.save"), prominent = true, onClick = {
                    val name = session.saveName.trim()
                    if (name.isEmpty()) return@MooButton
                    val saved = container.httpCollections.save(session.draft().copy(name = name), session.response)
                    session.loadDraft(saved.draft)
                    session.saveOpen = false
                    session.notice = container.t("common.save")
                    container.toastSuccess(container.t("common.save"))
                    onChanged()
                })
                MooButton(container.t("common.cancel"), onClick = { session.saveOpen = false; onChanged() })
            }
        }
    }
}

@Composable
private fun DeleteDialog(container: AppContainer, session: HttpSession, onChanged: () -> Unit) {
    MooOverlay(onDismiss = { session.deleteConfirm = false; onChanged() }) {
        Column(
            Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.confirmDelete"), color = MooTheme.colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.delete"), danger = true, onClick = {
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
    val findFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        findFocus.requestFocus()
    }
    fun step(forward: Boolean) {
        if (session.findQuery.isBlank()) return
        if (matches.isEmpty()) {
            container.toastFindNoMatches()
            return
        }
        onEdt {
            val match = RstaFindNavigation.jump(session.responseEditor, session.findQuery, session.findOptions, forward)
            if (match == null) {
                container.toastFindNoMatches()
            } else {
                val idx = matches.indexOfFirst { it.start == match.start && it.end == match.end }
                if (idx >= 0) session.findIndex = idx
            }
            onChanged()
        }
    }
    fun closeFind() {
        session.findOpen = false
        session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
        onChanged()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .mooFindBarBackground()
            .onFindBarRowKeys(
                onPrevious = { step(false) },
                onNext = { step(true) },
                onClose = ::closeFind,
            )
            .horizontalScroll(rememberScrollState())
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(
            session.findQuery,
            {
                session.findQuery = it
                session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
                onChanged()
            },
            modifier = Modifier.width(220.dp),
            placeholder = container.t("http.findPlaceholder"),
            fieldModifier = Modifier
                .focusRequester(findFocus)
                .onFindQueryEnterKey { step(true) },
        )
        MooButton(
            container.t("find.find"),
            enabled = session.findQuery.isNotBlank(),
            onClick = { step(true) },
        )
        MooButton(container.t("find.matchCase") + ": ${session.findOptions.matchCase}", onClick = {
            session.findOptions = session.findOptions.copy(matchCase = !session.findOptions.matchCase)
            session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
            onChanged()
        })
        MooButton(container.t("find.wholeWord") + ": ${session.findOptions.wholeWord}", onClick = {
            session.findOptions = session.findOptions.copy(wholeWord = !session.findOptions.wholeWord)
            session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
            onChanged()
        })
        MooButton(container.t("find.regex") + ": ${session.findOptions.regex}", onClick = {
            session.findOptions = session.findOptions.copy(regex = !session.findOptions.regex)
            session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
            onChanged()
        })
        Text(
            "${container.t("find.foundPrefix")} ${matches.size}",
            color = MooTheme.colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("find.previous"), onClick = { step(false) })
        MooButton(container.t("find.next"), onClick = { step(true) })
        MooButton(container.t("common.close"), onClick = ::closeFind)
    }
}

/** 对齐 Electron `HttpTool.openFind`：响应编辑器选区预填查找框（仅响应区，不含请求 pane）。 */
private fun openHttpResponseFind(session: HttpSession, onChanged: () -> Unit) {
    openFindBarSeedingSelection(session.responseEditor) { selected ->
        selected?.let { session.findQuery = it }
        session.findOpen = true
        session.findIndex = HttpResponseFind.FIND_INDEX_UNSET
        onChanged()
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
