package com.rememberber.mootool.next.compose.features.cron

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.CronEngine
import com.rememberber.mootool.next.compose.domain.CronWiringPresentation
import com.rememberber.mootool.next.compose.domain.CronException
import com.rememberber.mootool.next.compose.domain.CronFields
import com.rememberber.mootool.next.compose.domain.CronHistoryMetadata
import com.rememberber.mootool.next.compose.domain.CronHistoryRestore
import com.rememberber.mootool.next.compose.domain.TimeEngine
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CronSession
import com.rememberber.mootool.next.compose.storage.CronFavorite
import com.rememberber.mootool.next.compose.ui.components.FavoriteRow
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooCronBuilder
import com.rememberber.mootool.next.compose.ui.components.mooCronRunCell
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
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

@Composable
fun CronScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.cronSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Cron) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    var favorites by remember { mutableStateOf(container.cronFavorites.list()) }
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    val language = settings.general.language

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistCron()
    }

    LaunchedEffect(session.expression, language) {
        session.description = runCatching { CronEngine.describe(session.expression, language) }.getOrDefault("")
        refresh()
    }
    LaunchedEffect(session.favoritesOpen, revision) {
        if (session.favoritesOpen) favorites = container.cronFavorites.list()
    }
    LaunchedEffect(settings.data.directory, sessionGeneration) {
        favorites = container.cronFavorites.list()
        container.sessionManager.bump()
        container.sessionManager.persistCron()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val contentMaxWidth = maxWidth.value
        val overflow = LayoutPolicy.overflowToolbar(contentMaxWidth)
        val minBuilder = 460f
        val minExpression = 260f
        val paneHandle = 10f
        val maxBuilder = (contentMaxWidth - paneHandle - minExpression).coerceAtLeast(minBuilder)
        val defaultBuilder = (contentMaxWidth * 0.66f).coerceIn(minBuilder, maxBuilder)
        val builderWidth = settings.layout.pane(ToolId.Cron.id, 0, defaultBuilder, minBuilder, maxBuilder)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("cron.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("favorite.title")) { session.favoritesOpen = true; refresh() })
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Cron) })
                }
            )
        }
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.width(builderWidth.dp).widthIn(min = 460.dp).fillMaxHeight().mooCronBuilder(),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Text(container.t("cron.builder"), color = colors.textBody, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FieldCell(container, "cron.second", session.fields.second) { updateField(session, session.fields.copy(second = it), refresh = { refresh() }) }
                        FieldCell(container, "cron.minute", session.fields.minute) { updateField(session, session.fields.copy(minute = it), refresh = { refresh() }) }
                        FieldCell(container, "cron.hour", session.fields.hour) { updateField(session, session.fields.copy(hour = it), refresh = { refresh() }) }
                        FieldCell(container, "cron.day", session.fields.day) { updateField(session, session.fields.copy(day = it), refresh = { refresh() }) }
                        FieldCell(container, "cron.month", session.fields.month) { updateField(session, session.fields.copy(month = it), refresh = { refresh() }) }
                        FieldCell(container, "cron.week", session.fields.week) { updateField(session, session.fields.copy(week = it), refresh = { refresh() }) }
                        FieldCell(container, "cron.year", session.fields.year) { updateField(session, session.fields.copy(year = it), refresh = { refresh() }) }
                    }
                    CronPresets(container, session) { refresh() }
                }
                VerticalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.Cron.id, 0, builderWidth + it, 1) },
                    onReset = { container.setPaneSize(ToolId.Cron.id, 0, defaultBuilder, 1) }
                )
                Column(
                    Modifier.weight(1f).widthIn(min = 260.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(container.t("cron.expression"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.onPreviewKeyEvent { event ->
                            if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                parseRuns(container, session, language) { refresh() }
                                true
                            } else false
                        }
                    ) {
                        MooTextField(
                            session.expression,
                            {
                                applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                                    session.expression = it
                                    runCatching { session.fields = CronEngine.split(it) }
                                    session.error = ""
                                }
                                refresh()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MooButton(
                            container.t("cron.parse"),
                            prominent = true,
                            enabled = CronWiringPresentation.canParse(session.expression),
                            onClick = { parseRuns(container, session, language) { refresh() } },
                            p5Toolbar = true
                        )
                    }
                    ZonePicker(container, session) { refresh() }
                    Text(container.t("cron.humanReadable"), color = colors.textMuted, fontSize = 9.sp)
                    Text(session.description.ifEmpty { "—" }, color = colors.textBody, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
            Column(Modifier.weight(1f).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(container.t("cron.nextRuns"), color = colors.textBody, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    if (CronWiringPresentation.canCopyRuns(session.runs)) {
                        MooButton(
                            container.t("time.copy"),
                            onClick = {
                                container.copyText(session.runs.joinToString("\n"))
                                session.notice = container.t("common.copied")
                                refresh()
                            },
                            p5Toolbar = true
                        )
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(MooTheme.dimens.radiusLarge))
                        .background(colors.borderSoft).verticalScroll(rememberScrollState())
                ) {
                    when {
                        session.error.isNotEmpty() -> Text(
                            session.error,
                            color = colors.danger,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().background(colors.workspace).padding(12.dp)
                        )
                        session.runs.isEmpty() -> Text(
                            container.t("cron.parse"),
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().background(colors.workspace).padding(12.dp)
                        )
                        else -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            session.runs.chunked(2).forEachIndexed { rowIndex, pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                    pair.forEachIndexed { col, run ->
                                        val index = rowIndex * 2 + col + 1
                                        Row(
                                            Modifier
                                                .weight(1f)
                                                .mooCronRunCell()
                                                .background(colors.workspace)
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                index.toString(),
                                                color = colors.textMuted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.width(24.dp)
                                            )
                                            Text(
                                                run,
                                                color = colors.textBody,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f).heightIn(min = 38.dp).background(colors.workspace))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(session.error.ifEmpty { session.notice }, color = if (session.error.isNotEmpty()) colors.danger else colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
        }
    }

    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Cron.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                CronHistoryRestore.apply(session, item, container.settings.value.general.language)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
    if (session.favoritesOpen) {
        CronFavoritesDialog(container, session, favorites) {
            favorites = container.cronFavorites.list()
            refresh()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CronPresets(container: AppContainer, session: CronSession, onChanged: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            container.t("cron.preset"),
            color = MooTheme.colors.textMuted,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        CronEngine.presets.forEach { preset ->
            MooButton(
                container.t(preset.labelKey),
                onClick = {
                    applyExpression(session, preset.expression)
                    onChanged()
                },
                p5Toolbar = true
            )
        }
    }
}

@Composable
private fun RowScope.FieldCell(container: AppContainer, labelKey: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.weight(1f).widthIn(min = 65.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(container.t(labelKey), color = MooTheme.colors.textMuted, fontSize = 9.sp)
        MooTextField(value, onChange, modifier = Modifier.fillMaxWidth(), compact = true)
    }
}

@Composable
private fun ZonePicker(container: AppContainer, session: CronSession, onChanged: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val zones =
        remember {
            CronWiringPresentation.timezoneMenu(TimeEngine.systemZone(), TimeEngine.commonTimezones)
        }
    Box {
        MooButton("${container.t("time.timezone")} · ${session.zone}", onClick = { open = true }, p5Toolbar = true)
        MooMenu(expanded = open, onDismissRequest = { open = false }) {
            zones.forEach { zone ->
                MooMenuItem(zone) {
                    session.zone = zone
                    open = false
                    onChanged()
                }
            }
        }
    }
}

private fun updateField(session: CronSession, fields: CronFields, refresh: () -> Unit) {
    session.fields = fields
    session.error = ""
    runCatching { session.expression = CronEngine.build(fields) }
    refresh()
}

private fun applyExpression(session: CronSession, expression: String) {
    session.expression = expression
    session.error = ""
    runCatching { session.fields = CronEngine.split(expression) }
}

private fun parseRuns(container: AppContainer, session: CronSession, language: String, onChanged: () -> Unit) {
    container.scope.launch {
        val result = CronWiringPresentation.runPreview(session.expression, session.zone, language)
        withContext(Dispatchers.Swing) {
            when (result) {
                is CronWiringPresentation.ScheduleOutcome.Success -> {
                    val runs = result.runs
                    val description = result.description
                    session.runs = runs
                    session.description = description
                    session.error = ""
                    session.notice = container.t("cron.nextRuns")
                    container.toastSuccess(session.notice)
                    container.history.save(
                        ToolId.Cron.id,
                        session.notice,
                        session.notice,
                        session.expression,
                        runs.joinToString("\n"),
                        CronHistoryMetadata.encodeTimeZone(session.zone),
                    )
                }
                is CronWiringPresentation.ScheduleOutcome.Failure -> {
                    val error = result.error
                    session.runs = emptyList()
                    session.notice = ""
                    val message = (error as? CronException)?.message ?: error.message.orEmpty()
                    session.error = container.t("cron.invalid", mapOf("message" to message.ifBlank { "invalid" }))
                    container.toastError(session.error)
                }
            }
            onChanged()
        }
    }
}

@Composable
private fun CronFavoritesDialog(
    container: AppContainer,
    session: CronSession,
    items: List<CronFavorite>,
    onChanged: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(items, query) { container.cronFavorites.list(query) }
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
                    if (session.expression.isNotBlank()) {
                        container.cronFavorites.add(session.favoriteName, session.expression, session.favoriteGroup)
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
                            snippet = item.expression,
                            group = item.group,
                            deleteLabel = container.t("common.delete"),
                            onOpen = {
                                applyExpression(session, item.expression)
                                session.favoritesOpen = false
                                onChanged()
                            },
                            onDelete = {
                                container.cronFavorites.delete(item.id)
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
