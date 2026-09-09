package com.rememberber.mootool.next.compose.features.regex

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.RegexEngine
import com.rememberber.mootool.next.compose.domain.RegexMatch
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.RegexSession
import com.rememberber.mootool.next.compose.storage.RegexFavorite
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun RegexScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.regexSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    var favorites by remember { mutableStateOf(container.regexFavorites.list()) }
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistRegex()
    }

    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Regex.id)
    }
    LaunchedEffect(session.favoritesOpen, revision) {
        if (session.favoritesOpen) favorites = container.regexFavorites.list()
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("regex.title"), color = colors.textPrimary, fontSize = 16.sp)
            Text(container.t("regex.engine", mapOf("name" to RegexEngine.ENGINE_NAME)), color = colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("favorite.title"), onClick = { session.favoritesOpen = true; refresh() })
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Regex) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MooButton(container.t("regex.tab.test"), primary = session.tab == "test", onClick = { session.tab = "test"; refresh() })
            MooButton(container.t("regex.tab.common"), primary = session.tab == "common", onClick = { session.tab = "common"; refresh() })
        }
        if (session.tab == "common") {
            CommonPatterns(container, session) { refresh() }
        } else {
            TestWorkspace(container, session) { refresh() }
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

    if (session.historyOpen) {
        RegexHistoryDialog(container, session, historyItems) { refresh() }
    }
    if (session.favoritesOpen) {
        RegexFavoritesDialog(container, session, favorites) {
            favorites = container.regexFavorites.list()
            refresh()
        }
    }
}

@Composable
private fun TestWorkspace(container: AppContainer, session: RegexSession, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("regex.expression"), color = colors.textSecondary, fontSize = 12.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        runMatch(container, session, onChanged)
                        true
                    } else false
                }
            ) {
                MooTextField(session.pattern, { session.pattern = it; session.error = ""; onChanged() }, modifier = Modifier.weight(1f))
                if (session.running) {
                    MooButton(container.t("regex.cancel"), onClick = {
                        session.matchGeneration += 1
                        container.regexWorker.cancel()
                        session.running = false
                        session.error = container.t("regex.cancelled")
                        onChanged()
                    })
                } else {
                    MooButton(container.t("regex.tab.test"), primary = true, onClick = { runMatch(container, session, onChanged) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlagButton(container.t("regex.flag.global"), session.options.global) {
                    session.options = session.options.copy(global = !session.options.global)
                    onChanged()
                }
                FlagButton(container.t("regex.flag.ignoreCase"), session.options.ignoreCase) {
                    session.options = session.options.copy(ignoreCase = !session.options.ignoreCase)
                    onChanged()
                }
                FlagButton(container.t("regex.flag.multiline"), session.options.multiline) {
                    session.options = session.options.copy(multiline = !session.options.multiline)
                    onChanged()
                }
                FlagButton(container.t("regex.flag.dotAll"), session.options.dotAll) {
                    session.options = session.options.copy(dotAll = !session.options.dotAll)
                    onChanged()
                }
            }
            Text(container.t("regex.source"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(
                session.source,
                { session.source = it; onChanged() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                singleLine = false
            )
        }
        Column(
            modifier = Modifier.weight(0.9f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceSubtle).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(12.dp)
        ) {
            val status = when {
                session.error.isNotEmpty() -> session.error
                else -> container.t("regex.matches", mapOf("count" to session.matches.size.toString()))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    status,
                    color = if (session.error.isNotEmpty()) colors.danger else colors.textPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (session.matches.isNotEmpty()) {
                    MooButton(container.t("regex.copy"), onClick = {
                        val text = session.matches.joinToString("\n") { it.value }
                        copyToClipboard(text)
                        session.notice = container.t("common.copied")
                        onChanged()
                    })
                }
            }
            if (session.matches.isEmpty() && session.error.isEmpty()) {
                Text(container.t("regex.noMatches"), color = colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    session.matches.forEachIndexed { index, match ->
                        MatchCard(index, match)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(index: Int, match: RegexMatch) {
    val colors = MooTheme.colors
    Column(Modifier.fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text("#${index + 1} · ${match.index}", color = colors.textSecondary, fontSize = 12.sp)
        Text(match.value.ifEmpty { "∅" }, color = colors.textPrimary, fontSize = 14.sp)
        if (match.groups.isNotEmpty()) {
            Text(match.groups.joinToString(" · "), color = colors.textSecondary, fontSize = 12.sp)
        }
        if (match.named.isNotEmpty()) {
            Text(match.named.entries.joinToString(" · ") { "${it.key}=${it.value}" }, color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FlagButton(label: String, selected: Boolean, onClick: () -> Unit) {
    MooButton(label, primary = selected, onClick = onClick)
}

@Composable
private fun CommonPatterns(container: AppContainer, session: RegexSession, onChanged: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegexEngine.commonRegexes.forEach { item ->
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MooTheme.colors.surfaceSubtle)
                    .clickable {
                        session.pattern = item.pattern
                        session.tab = "test"
                        session.error = ""
                        onChanged()
                    }.padding(10.dp)
            ) {
                Text(container.t(item.labelKey), color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                Text(item.pattern, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

private val historyCodec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun runMatch(container: AppContainer, session: RegexSession, onChanged: () -> Unit, saveHistory: Boolean = true) {
    if (session.running) return
    session.matchGeneration += 1
    val generation = session.matchGeneration
    session.running = true
    session.error = ""
    session.notice = container.t("regex.running")
    onChanged()
    container.scope.launch {
        val response = container.regexWorker.match(session.pattern, session.source, session.options)
        withContextSwing {
            if (generation != session.matchGeneration) return@withContextSwing
            session.running = false
            if (response.ok) {
                session.matches = response.matches
                session.error = ""
                session.notice = container.t("regex.matches", mapOf("count" to response.matches.size.toString()))
                if (saveHistory) {
                    container.history.save(
                        ToolId.Regex.id,
                        session.notice,
                        session.notice,
                        session.pattern,
                        session.source,
                        historyCodec.encodeToString(session.options)
                    )
                }
            } else {
                session.matches = emptyList()
                session.notice = ""
                session.error = when (response.code) {
                    "timeout" -> container.t("regex.timeout")
                    "limit" -> container.t("regex.limit")
                    "cancelled" -> container.t("regex.cancelled")
                    "worker-unavailable" -> container.t("regex.workerUnavailable")
                    else -> container.t("regex.invalid", mapOf("message" to response.error.ifBlank { response.code }))
                }
            }
            onChanged()
        }
    }
}

private suspend fun withContextSwing(block: () -> Unit) {
    kotlinx.coroutines.withContext(Dispatchers.Swing) { block() }
}

private fun copyToClipboard(value: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}

@Composable
private fun RegexHistoryDialog(
    container: AppContainer,
    session: RegexSession,
    items: List<HistoryRecord>,
    onChanged: () -> Unit
) {
    Dialog(onDismissRequest = { session.historyOpen = false; onChanged() }) {
        Column(
            Modifier.width(520.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("common.action.history"), color = MooTheme.colors.textPrimary)
            if (items.isEmpty()) {
                Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            session.pattern = item.input
                            session.source = item.output
                            session.options = runCatching { historyCodec.decodeFromString(com.rememberber.mootool.next.compose.domain.RegexOptions.serializer(), item.options) }
                                .getOrDefault(session.options)
                            session.tab = "test"
                            session.historyOpen = false
                            session.notice = container.t("json.notice.restored")
                            onChanged()
                            runMatch(container, session, onChanged, saveHistory = false)
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.input, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Regex.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

@Composable
private fun RegexFavoritesDialog(
    container: AppContainer,
    session: RegexSession,
    items: List<RegexFavorite>,
    onChanged: () -> Unit
) {
    Dialog(onDismissRequest = { session.favoritesOpen = false; onChanged() }) {
        Column(
            Modifier.width(520.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("favorite.title"), color = MooTheme.colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MooTextField(
                    session.favoriteName,
                    { session.favoriteName = it; onChanged() },
                    modifier = Modifier.weight(1f),
                    placeholder = container.t("favorite.namePlaceholder")
                )
                MooButton(container.t("favorite.add"), primary = true, onClick = {
                    if (session.pattern.isNotBlank()) {
                        container.regexFavorites.add(session.favoriteName, session.pattern)
                        session.favoriteName = ""
                        session.notice = container.t("favorite.saved")
                        onChanged()
                    }
                })
            }
            if (items.isEmpty()) {
                Text(container.t("favorite.empty"), color = MooTheme.colors.textSecondary)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable {
                                session.pattern = item.pattern
                                session.tab = "test"
                                session.favoritesOpen = false
                                onChanged()
                            }) {
                                Text(item.name, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                                Text(item.pattern, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                            }
                            MooButton(container.t("common.delete"), onClick = {
                                container.regexFavorites.delete(item.id)
                                session.notice = container.t("favorite.deleted")
                                onChanged()
                            })
                        }
                    }
                }
            }
            MooButton(container.t("common.close"), onClick = { session.favoritesOpen = false; onChanged() })
        }
    }
}
