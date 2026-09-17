package com.rememberber.mootool.next.compose.features.regex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.CommonRegex
import com.rememberber.mootool.next.compose.domain.RegexEngine
import com.rememberber.mootool.next.compose.domain.RegexHistoryMetadata
import com.rememberber.mootool.next.compose.domain.RegexHistoryRestore
import com.rememberber.mootool.next.compose.domain.RegexMatch
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.RegexSession
import com.rememberber.mootool.next.compose.storage.RegexFavorite
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.FavoriteRow
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooToolTabsRow
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.OnToolLeaveUnlessDetached
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

@Composable
fun RegexScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.regexSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Regex) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val settings by container.settings.collectAsState()
    var favorites by remember { mutableStateOf(container.regexFavorites.list()) }
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistRegex()
    }

    LaunchedEffect(session.favoritesOpen, revision) {
        if (session.favoritesOpen) favorites = container.regexFavorites.list()
    }
    LaunchedEffect(settings.data.directory, sessionGeneration) {
        favorites = container.regexFavorites.list()
        refresh()
    }

    OnToolLeaveUnlessDetached(container, ToolId.Regex) {
        if (session.running) {
            session.matchGeneration += 1
            container.regexWorker.cancel()
            session.running = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("regex.title"))
            Text(container.t("regex.engine", mapOf("name" to RegexEngine.ENGINE_NAME)), color = colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("favorite.title")) { session.favoritesOpen = true; refresh() })
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Regex) })
                }
            )
        }
        MooToolTabsRow {
            MooToolTab(container.t("regex.tab.test"), selected = session.tab == "test", onClick = { session.tab = "test"; refresh() })
            MooToolTab(container.t("regex.tab.common"), selected = session.tab == "common", onClick = { session.tab = "common"; refresh() })
        }
        if (session.tab == "common") {
            CommonPatterns(container, session) { refresh() }
        } else {
            TestWorkspace(container, session, Modifier.weight(1f).fillMaxWidth()) { refresh() }
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
            toolId = ToolId.Regex.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                RegexHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
                runMatch(container, session, { refresh() }, saveHistory = false)
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
    if (session.favoritesOpen) {
        RegexFavoritesDialog(container, session, favorites) {
            favorites = container.regexFavorites.list()
            refresh()
        }
    }
}

@Composable
private fun TestWorkspace(
    container: AppContainer,
    session: RegexSession,
    modifier: Modifier = Modifier,
    onChanged: () -> Unit
) {
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("regex.expression"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        runMatch(container, session, onChanged)
                        true
                    } else false
                }
            ) {
                MooTextField(
                    session.pattern,
                    {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            session.pattern = it
                            session.error = ""
                        }
                        onChanged()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (session.running) {
                    MooButton(
                        container.t("regex.cancel"),
                        onClick = {
                            session.matchGeneration += 1
                            container.regexWorker.cancel()
                            session.running = false
                            session.error = container.t("regex.cancelled")
                            onChanged()
                        },
                        p5Toolbar = true
                    )
                } else {
                    MooButton(
                        container.t("regex.tab.test"),
                        prominent = true,
                        onClick = { runMatch(container, session, onChanged) },
                        p5Toolbar = true
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
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
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val minSource = 320f
            val minResults = 220f
            val paneHandle = 10f
            val maxSource = (maxWidth.value - paneHandle - minResults).coerceAtLeast(minSource)
            val defaultSource = (maxWidth.value * 0.71f).coerceIn(minSource, maxSource)
            val sourceWidth = settings.layout.pane(ToolId.Regex.id, 0, defaultSource, minSource, maxSource)
            Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(sourceWidth.dp).widthIn(min = 320.dp).fillMaxHeight().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("regex.source"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                MooTextField(
                    session.source,
                    {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            session.source = it
                        }
                        onChanged()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false
                )
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Regex.id, 0, sourceWidth + it, 1) },
                onReset = { container.setPaneSize(ToolId.Regex.id, 0, defaultSource, 1) }
            )
            Column(
                modifier = Modifier.weight(1f).widthIn(min = 220.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(14.dp)
            ) {
                val status = when {
                    session.error.isNotEmpty() -> session.error
                    else -> container.t("regex.matches", mapOf("count" to session.matches.size.toString()))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        status,
                        color = if (session.error.isNotEmpty()) colors.danger else colors.textPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (session.matches.isNotEmpty()) {
                        MooButton(
                            container.t("regex.copy"),
                            onClick = {
                                val text = session.matches.joinToString("\n") { it.value }
                                container.copyText(text)
                                session.notice = container.t("common.copied")
                                onChanged()
                            },
                            p5Toolbar = true
                        )
                    }
                }
                if (session.matches.isEmpty() && session.error.isEmpty()) {
                    Text(container.t("regex.noMatches"), color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        session.matches.forEachIndexed { index, match ->
                            MatchCard(index, match)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun MatchCard(index: Int, match: RegexMatch) {
    val colors = MooTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.workspace)
            .border(1.dp, colors.borderSoft, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("#${index + 1} · ${match.index}", color = colors.textMuted, fontSize = 9.sp)
        Text(
            match.value.ifEmpty { "∅" },
            color = colors.textBody,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        if (match.groups.isNotEmpty()) {
            Text(match.groups.joinToString(" · "), color = colors.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        if (match.named.isNotEmpty()) {
            Text(
                match.named.entries.joinToString(" · ") { "${it.key}=${it.value}" },
                color = colors.textMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun FlagButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Checkbox(selected, { onClick() })
        Text(label, color = MooTheme.colors.textMuted, fontSize = 10.sp)
    }
}

@Composable
private fun CommonPatterns(container: AppContainer, session: RegexSession, onChanged: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegexEngine.commonRegexes.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { item ->
                    PatternCard(container, item, Modifier.weight(1f)) {
                        session.pattern = item.pattern
                        session.tab = "test"
                        session.error = ""
                        onChanged()
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PatternCard(container: AppContainer, item: CommonRegex, modifier: Modifier, onClick: () -> Unit) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(7.dp)
    val fill = if (hovered) colors.controlHover.compositeOver(colors.surfaceSubtle) else colors.surfaceSubtle
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, colors.borderSoft, shape)
            .hoverable(interaction)
            .mooFocusClickable(shape = shape, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(container.t(item.labelKey), color = colors.textBody, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(item.pattern, color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

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
                container.toastSuccess(session.notice)
                if (saveHistory) {
                    container.history.save(
                        ToolId.Regex.id,
                        session.notice,
                        session.notice,
                        session.pattern,
                        session.source,
                        RegexHistoryMetadata.encode(session.options)
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
                container.toastError(session.error)
            }
            onChanged()
        }
    }
}

private suspend fun withContextSwing(block: () -> Unit) {
    kotlinx.coroutines.withContext(Dispatchers.Swing) { block() }
}

@Composable
private fun RegexFavoritesDialog(
    container: AppContainer,
    session: RegexSession,
    items: List<RegexFavorite>,
    onChanged: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(items, query) { container.regexFavorites.list(query) }
    MooOverlay(onDismiss = { session.favoritesOpen = false; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(460.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("favorite.title"), color = MooTheme.colors.textPrimary)
            MooTextField(
                query,
                { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = container.t("favorite.queryPlaceholder")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MooTextField(
                    session.favoriteName,
                    { session.favoriteName = it; onChanged() },
                    modifier = Modifier.weight(1f),
                    placeholder = container.t("favorite.namePlaceholder")
                )
                MooTextField(
                    session.favoriteGroup,
                    { session.favoriteGroup = it; onChanged() },
                    modifier = Modifier.weight(1f),
                    placeholder = container.t("favorite.groupPlaceholder")
                )
                MooButton(container.t("favorite.add"), prominent = true, onClick = {
                    if (session.pattern.isNotBlank()) {
                        container.regexFavorites.add(session.favoriteName, session.pattern, session.favoriteGroup)
                        session.favoriteName = ""
                        session.notice = container.t("favorite.saved")
                        container.toastSuccess(container.t("favorite.saved"))
                        onChanged()
                    }
                })
            }
            if (visible.isEmpty()) {
                Text(container.t("favorite.empty"), color = MooTheme.colors.textSecondary, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(visible) { item ->
                        FavoriteRow(
                            name = item.name,
                            snippet = item.pattern,
                            group = item.group,
                            deleteLabel = container.t("common.delete"),
                            onOpen = {
                                session.pattern = item.pattern
                                session.tab = "test"
                                session.favoritesOpen = false
                                onChanged()
                            },
                            onDelete = {
                                container.regexFavorites.delete(item.id)
                                session.notice = container.t("favorite.deleted")
                                container.toastSuccess(container.t("favorite.deleted"))
                                onChanged()
                            }
                        )
                    }
                }
            }
            MooButton(container.t("common.close"), onClick = { session.favoritesOpen = false; onChanged() })
        }
    }
}
