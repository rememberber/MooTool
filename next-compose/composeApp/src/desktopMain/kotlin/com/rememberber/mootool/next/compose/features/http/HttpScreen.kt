package com.rememberber.mootool.next.compose.features.http

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.HttpCookie
import com.rememberber.mootool.next.compose.domain.HttpEngine
import com.rememberber.mootool.next.compose.domain.HttpErrorCode
import com.rememberber.mootool.next.compose.domain.HttpMethod
import com.rememberber.mootool.next.compose.domain.HttpPair
import com.rememberber.mootool.next.compose.domain.HttpProxyConfig
import com.rememberber.mootool.next.compose.domain.HttpRequestTab
import com.rememberber.mootool.next.compose.domain.HttpResponseResult
import com.rememberber.mootool.next.compose.domain.HttpResponseTab
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.HttpSession
import com.rememberber.mootool.next.compose.storage.SavedHttpRequest
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.UUID

@Composable
fun HttpScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.httpSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(emptyList<SavedHttpRequest>()) }
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistHttp()
    }

    fun reload() {
        items = container.httpCollections.list(session.query)
    }

    LaunchedEffect(session.query, revision) { reload() }
    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Http.id)
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
        session.previousResponse = session.response
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
                    draft.url,
                    result.body.take(8_000),
                    result.status.toString()
                )
                persist()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            if (session.error.isNotEmpty()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            else if (session.notice.isNotEmpty()) Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; persist() })
            if (!detached) MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Http) })
        }
        Row(Modifier.fillMaxSize()) {
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
            }, { persist(); reload() })
            Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    HttpMethod.entries.forEach { method ->
                        val selected = session.method == method
                        Text(
                            method.name,
                            color = if (selected) colors.onAccent else colors.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(if (selected) colors.accent else colors.control)
                                .clickable { session.method = method; persist() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            session.body = HttpEngine.formatBody(session.body, session.bodyType)
                            persist()
                        })
                    }
                }
                when (session.requestTab) {
                    HttpRequestTab.Params -> PairEditor(container, session.params, { session.params = it; persist() })
                    HttpRequestTab.Headers -> PairEditor(container, session.headers, { session.headers = it; persist() })
                    HttpRequestTab.Cookies -> CookieEditor(container, session.cookies, { session.cookies = it; persist() })
                    HttpRequestTab.Body -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(container.t("http.bodyType"), color = colors.textSecondary, fontSize = 12.sp)
                            HttpEngine.BODY_TYPES.forEach { type ->
                                TabChip(type.substringAfter('/'), session.bodyType == type) {
                                    session.bodyType = type
                                    persist()
                                }
                            }
                        }
                        MooTextField(
                            session.body,
                            { session.body = it; persist() },
                            modifier = Modifier.height(140.dp).fillMaxWidth(),
                            placeholder = container.t("http.bodyPlaceholder"),
                            singleLine = false
                        )
                    }
                }
                val showingPrevious = session.sending && session.previousResponse != null
                val visible = if (showingPrevious) session.previousResponse else session.response
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
                }
                if (visible != null) {
                    Text("${visible.status} · ${visible.durationMs} ms · ${visible.url}", color = colors.textSecondary, fontSize = 11.sp)
                }
                val text = when (session.responseTab) {
                    HttpResponseTab.Body -> visible?.body
                    HttpResponseTab.Headers -> visible?.headers
                    HttpResponseTab.Cookies -> visible?.cookies
                }.orEmpty().ifBlank { container.t("http.responseEmpty") }
                SelectionContainer(
                    Modifier.weight(1f).fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(8.dp).verticalScroll(rememberScrollState())
                ) {
                    Text(text, color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
    if (session.curlOpen) CurlDialog(container, session) { persist() }
    if (session.saveOpen) SaveDialog(container, session) { persist(); reload() }
    if (session.deleteConfirm) DeleteDialog(container, session) { persist(); reload() }
    if (session.historyOpen) HistoryDialog(container, session, historyItems) { persist() }
}

@Composable
private fun CollectionPane(
    container: AppContainer,
    session: HttpSession,
    items: List<SavedHttpRequest>,
    onSelect: (SavedHttpRequest) -> Unit,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    Column(
        Modifier.width(240.dp).fillMaxHeight().background(colors.sidebar).padding(8.dp),
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
private fun HistoryDialog(container: AppContainer, session: HttpSession, items: List<HistoryRecord>, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.historyOpen = false; onChanged() }) {
        Column(
            Modifier.width(480.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("http.history"), color = MooTheme.colors.textPrimary)
            if (items.isEmpty()) Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary)
            else {
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            session.url = item.input
                            HttpMethod.entries.find { it.name == item.operation }?.let { session.method = it }
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
                MooButton(container.t("common.clear"), onClick = { container.history.clear(ToolId.Http.id); onChanged() })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

private fun messageFor(container: AppContainer, code: HttpErrorCode, raw: String): String {
    val localized = container.t("http.error.${code.name}")
    return if (raw.isNotBlank() && raw != code.name) "$localized ($raw)" else localized
}
