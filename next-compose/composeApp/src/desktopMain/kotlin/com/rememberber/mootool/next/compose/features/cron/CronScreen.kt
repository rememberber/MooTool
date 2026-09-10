package com.rememberber.mootool.next.compose.features.cron

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import com.rememberber.mootool.next.compose.domain.CronEngine
import com.rememberber.mootool.next.compose.domain.CronException
import com.rememberber.mootool.next.compose.domain.CronFields
import com.rememberber.mootool.next.compose.domain.TimeEngine
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CronSession
import com.rememberber.mootool.next.compose.storage.CronFavorite
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun CronScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.cronSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    var favorites by remember { mutableStateOf(container.cronFavorites.list()) }
    val colors = MooTheme.colors
    val language = container.settings.collectAsState().value.general.language

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistCron()
    }

    LaunchedEffect(session.expression, language) {
        session.description = runCatching { CronEngine.describe(session.expression, language) }.getOrDefault("")
        refresh()
    }
    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Cron.id)
    }
    LaunchedEffect(session.favoritesOpen, revision) {
        if (session.favoritesOpen) favorites = container.cronFavorites.list()
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("cron.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("favorite.title"), onClick = { session.favoritesOpen = true; refresh() })
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Cron) })
            }
        }
        Row(Modifier.weight(1f).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(container.t("cron.builder"), color = colors.textSecondary, fontSize = 12.sp)
                FieldRow(container, "cron.second", session.fields.second) { updateField(session, session.fields.copy(second = it), refresh = { refresh() }) }
                FieldRow(container, "cron.minute", session.fields.minute) { updateField(session, session.fields.copy(minute = it), refresh = { refresh() }) }
                FieldRow(container, "cron.hour", session.fields.hour) { updateField(session, session.fields.copy(hour = it), refresh = { refresh() }) }
                FieldRow(container, "cron.day", session.fields.day) { updateField(session, session.fields.copy(day = it), refresh = { refresh() }) }
                FieldRow(container, "cron.month", session.fields.month) { updateField(session, session.fields.copy(month = it), refresh = { refresh() }) }
                FieldRow(container, "cron.week", session.fields.week) { updateField(session, session.fields.copy(week = it), refresh = { refresh() }) }
                FieldRow(container, "cron.year", session.fields.year) { updateField(session, session.fields.copy(year = it), refresh = { refresh() }) }
                Text(container.t("cron.preset"), color = colors.textSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CronEngine.presets.forEach { preset ->
                        MooButton(container.t(preset.labelKey), onClick = {
                            applyExpression(session, preset.expression)
                            refresh()
                        })
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(container.t("cron.expression"), color = colors.textSecondary, fontSize = 12.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            parseRuns(container, session, language) { refresh() }
                            true
                        } else false
                    }
                ) {
                    MooTextField(session.expression, {
                        session.expression = it
                        runCatching { session.fields = CronEngine.split(it) }
                        session.error = ""
                        refresh()
                    }, modifier = Modifier.weight(1f))
                    MooButton(container.t("cron.parse"), primary = true, onClick = { parseRuns(container, session, language) { refresh() } })
                }
                ZonePicker(container, session) { refresh() }
                Text(container.t("cron.humanReadable"), color = colors.textSecondary, fontSize = 12.sp)
                Text(session.description.ifEmpty { "—" }, color = colors.textPrimary, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(container.t("cron.nextRuns"), color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    if (session.runs.isNotEmpty()) {
                        MooButton(container.t("time.copy"), onClick = {
                            copyText(session.runs.joinToString("\n"))
                            session.notice = container.t("common.copied")
                            refresh()
                        })
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceSubtle).border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(12.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when {
                        session.error.isNotEmpty() -> Text(session.error, color = colors.danger, fontSize = 13.sp)
                        session.runs.isEmpty() -> Text(container.t("cron.parse"), color = colors.textSecondary, fontSize = 13.sp)
                        else -> session.runs.forEachIndexed { index, run ->
                            Text("${index + 1}. $run", color = colors.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(session.error.ifEmpty { session.notice }, color = if (session.error.isNotEmpty()) colors.danger else colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }

    if (session.historyOpen) {
        CronHistoryDialog(container, session, historyItems) { refresh() }
    }
    if (session.favoritesOpen) {
        CronFavoritesDialog(container, session, favorites) {
            favorites = container.cronFavorites.list()
            refresh()
        }
    }
}

@Composable
private fun FieldRow(container: AppContainer, labelKey: String, value: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(container.t(labelKey), color = MooTheme.colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(88.dp))
        MooTextField(value, onChange, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ZonePicker(container: AppContainer, session: CronSession, onChanged: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val zones = remember { (listOf(TimeEngine.systemZone()) + TimeEngine.commonTimezones).distinct() }
    Box {
        MooButton("${container.t("time.timezone")} · ${session.zone}", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            zones.forEach { zone ->
                DropdownMenuItem(onClick = {
                    session.zone = zone
                    open = false
                    onChanged()
                }) {
                    Text(zone)
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
        val result = runCatching {
            val runs = CronEngine.nextRuns(session.expression, session.zone)
            val description = CronEngine.describe(session.expression, language)
            runs to description
        }
        withContext(Dispatchers.Swing) {
            result.fold(
                onSuccess = { (runs, description) ->
                    session.runs = runs
                    session.description = description
                    session.error = ""
                    session.notice = container.t("cron.nextRuns")
                    container.history.save(ToolId.Cron.id, session.notice, session.notice, session.expression, runs.joinToString("\n"), session.zone)
                },
                onFailure = { error ->
                    session.runs = emptyList()
                    session.notice = ""
                    val message = (error as? CronException)?.message ?: error.message.orEmpty()
                    session.error = container.t("cron.invalid", mapOf("message" to message.ifBlank { "invalid" }))
                }
            )
            onChanged()
        }
    }
}

private fun copyText(value: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}

@Composable
private fun CronHistoryDialog(
    container: AppContainer,
    session: CronSession,
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
                            applyExpression(session, item.input)
                            session.runs = item.output.split('\n').filter { it.isNotBlank() }
                            if (item.options.isNotBlank()) session.zone = item.options
                            session.historyOpen = false
                            session.notice = container.t("json.notice.restored")
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.input, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Cron.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
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
                    if (session.expression.isNotBlank()) {
                        container.cronFavorites.add(session.favoriteName, session.expression)
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
                                applyExpression(session, item.expression)
                                session.favoritesOpen = false
                                onChanged()
                            }) {
                                Text(item.name, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                                Text(item.expression, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                            }
                            MooButton(container.t("common.delete"), onClick = {
                                container.cronFavorites.delete(item.id)
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
