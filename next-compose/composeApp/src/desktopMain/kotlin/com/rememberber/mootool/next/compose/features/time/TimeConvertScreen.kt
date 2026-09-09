package com.rememberber.mootool.next.compose.features.time

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.TimeEngine
import com.rememberber.mootool.next.compose.domain.TimeException
import com.rememberber.mootool.next.compose.domain.TimestampUnit
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.TimeSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class TimeHistoryOptions(val zone: String, val unit: String)

@Composable
fun TimeConvertScreen(container: AppContainer, detached: Boolean, active: Boolean) {
    val session = remember { container.sessionManager.timeSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    var zoneMenuOpen by remember { mutableStateOf(false) }
    val colors = MooTheme.colors
    val zones = remember { (listOf(TimeEngine.systemZone()) + TimeEngine.commonTimezones).distinct() }

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistTime()
    }

    LaunchedEffect(active) {
        if (!active) {
            session.clockOpen = false
            return@LaunchedEffect
        }
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.TimeConvert.id)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.workspace).padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("time.title"), color = colors.textPrimary, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.TimeConvert) })
            }
        }

        CurrentBand(container, session, nowMillis, onChanged = { refresh() })

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceSubtle)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(container.t("time.timezone"), color = colors.textSecondary, fontSize = 12.sp)
            Box {
                MooButton(TimeEngine.formatTimezoneLabel(session.zone, nowMillis), onClick = { zoneMenuOpen = true })
                DropdownMenu(expanded = zoneMenuOpen, onDismissRequest = { zoneMenuOpen = false }) {
                    zones.forEach { zone ->
                        DropdownMenuItem(onClick = {
                            session.zone = zone
                            session.localTime = TimeEngine.formatLocalTime(nowMillis, zone)
                            session.error = ""
                            zoneMenuOpen = false
                            refresh()
                        }) {
                            Text(TimeEngine.formatTimezoneLabel(zone, nowMillis), fontSize = 13.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeEngine.quickTimezones.forEach { (zone, label) ->
                    MooButton(label, primary = session.zone == zone, onClick = {
                        session.zone = zone
                        session.localTime = TimeEngine.formatLocalTime(nowMillis, zone)
                        session.error = ""
                        refresh()
                    })
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceSubtle)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(container.t("time.timestamp"), color = colors.textSecondary, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooTextField(
                    session.timestamp,
                    { session.timestamp = it; session.error = ""; refresh() },
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            convertToLocal(container, session, refresh = { refresh() })
                            true
                        } else false
                    }
                )
                MooButton(container.t("time.unit.second"), primary = session.unit == TimestampUnit.Second, onClick = {
                    session.unit = TimestampUnit.Second
                    refresh()
                })
                MooButton(container.t("time.unit.millisecond"), primary = session.unit == TimestampUnit.Millisecond, onClick = {
                    session.unit = TimestampUnit.Millisecond
                    refresh()
                })
                MooButton(container.t("time.copy"), onClick = { copyField(container, session, session.timestamp, refresh = { refresh() }) })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("time.toLocal"), primary = true, onClick = {
                    convertToLocal(container, session, refresh = { refresh() })
                })
                MooButton(container.t("time.toTimestamp"), onClick = {
                    convertToTimestamp(container, session, refresh = { refresh() })
                })
            }

            Text("${container.t("time.localTime")} · ${session.zone}", color = colors.textSecondary, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooTextField(
                    session.localTime,
                    { session.localTime = it; session.error = ""; refresh() },
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            convertToTimestamp(container, session, refresh = { refresh() })
                            true
                        } else false
                    }
                )
                Text(container.t("time.formatHint"), color = colors.textSecondary, fontSize = 12.sp)
                MooButton(container.t("time.copy"), onClick = { copyField(container, session, session.localTime, refresh = { refresh() }) })
            }
        }

        if (session.error.isNotEmpty()) {
            Text(session.error, color = colors.danger, fontSize = 13.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }

    if (session.historyOpen) {
        TimeHistoryDialog(container, session, historyItems) { refresh() }
    }
    if (session.clockOpen && active) {
        ClockOverlay(container, session, nowMillis) { refresh() }
    }
}

@Composable
private fun CurrentBand(container: AppContainer, session: TimeSession, nowMillis: Long, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    val currentTimestamp = (nowMillis / 1000).toString()
    val currentLocal = TimeEngine.formatLocalTime(nowMillis, session.zone)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(container.t("time.current"), color = colors.textPrimary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
            TimeValue(container, container.t("time.timestamp"), currentTimestamp) {
                copyField(container, session, currentTimestamp, onChanged)
            }
            TimeValue(container, "${container.t("time.localTime")} · ${session.zone}", currentLocal) {
                copyField(container, session, currentLocal, onChanged)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MooButton(container.t("time.history"), onClick = { session.historyOpen = true; onChanged() })
            MooButton(container.t("time.clock"), onClick = { session.clockOpen = true; onChanged() })
            Text(TimeEngine.formatTimezoneLabel(session.zone, nowMillis), color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TimeValue(container: AppContainer, label: String, value: String, onCopy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.width(320.dp)) {
        Text(label, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, color = MooTheme.colors.textPrimary, fontSize = 16.sp)
            MooButton(container.t("time.copy"), onClick = onCopy)
        }
    }
}

@Composable
private fun TimeHistoryDialog(
    container: AppContainer,
    session: TimeSession,
    items: List<HistoryRecord>,
    onChanged: () -> Unit
) {
    Dialog(onDismissRequest = { session.historyOpen = false; onChanged() }) {
        Column(
            Modifier.width(520.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("time.history"), color = MooTheme.colors.textPrimary)
            if (items.isEmpty()) {
                Text(container.t("time.history.empty"), color = MooTheme.colors.textSecondary)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            applyHistory(session, item)
                            session.notice = container.t("json.notice.restored")
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text("${item.input} → ${item.output}", color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                            Text(item.createdAt, color = MooTheme.colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.TimeConvert.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

@Composable
private fun ClockOverlay(container: AppContainer, session: TimeSession, nowMillis: Long, onChanged: () -> Unit) {
    val formatted = TimeEngine.formatLocalTime(nowMillis, session.zone)
    val parts = formatted.split(" ")
    val date = parts.getOrNull(0).orEmpty()
    val time = parts.getOrNull(1).orEmpty()
    Dialog(
        onDismissRequest = { session.clockOpen = false; onChanged() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF111113)).onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    session.clockOpen = false
                    onChanged()
                    true
                } else false
            },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(session.zone, color = Color(0xFFB0B0B8), fontSize = 18.sp)
                Text(time, color = Color.White, fontSize = 72.sp)
                Text(date, color = Color(0xFFB0B0B8), fontSize = 22.sp)
                MooButton(container.t("time.clock.close"), onClick = { session.clockOpen = false; onChanged() })
            }
        }
    }
}

private val historyCodec = Json { ignoreUnknownKeys = true }
private val timestampPattern = Regex("^[+-]?\\d+$")

private fun convertToLocal(container: AppContainer, session: TimeSession, refresh: () -> Unit) {
    runCatching { TimeEngine.timestampToLocal(session.timestamp, session.unit, session.zone) }
        .onSuccess { result ->
            session.localTime = result.localTime
            session.unit = result.unit
            session.error = ""
            val notice = container.t("time.notice.toLocal", mapOf("zone" to session.zone))
            session.notice = notice
            saveHistory(container, session, notice, session.timestamp, result.localTime)
        }
        .onFailure { error ->
            session.notice = ""
            session.error = messageFor(container, error)
        }
    refresh()
}

private fun convertToTimestamp(container: AppContainer, session: TimeSession, refresh: () -> Unit) {
    runCatching { TimeEngine.localToTimestamp(session.localTime, session.unit, session.zone) }
        .onSuccess { result ->
            session.timestamp = result
            session.error = ""
            val notice = container.t("time.notice.toTimestamp")
            session.notice = notice
            saveHistory(container, session, notice, session.localTime, result)
        }
        .onFailure { error ->
            session.notice = ""
            session.error = messageFor(container, error)
        }
    refresh()
}

private fun saveHistory(container: AppContainer, session: TimeSession, summary: String, input: String, output: String) {
    val options = historyCodec.encodeToString(
        TimeHistoryOptions(session.zone, if (session.unit == TimestampUnit.Millisecond) "millisecond" else "second")
    )
    container.history.save(ToolId.TimeConvert.id, summary, summary, input, output, options)
}

private fun applyHistory(session: TimeSession, item: HistoryRecord) {
    val options = runCatching { historyCodec.decodeFromString<TimeHistoryOptions>(item.options) }.getOrNull()
    if (options != null) {
        session.zone = options.zone.ifBlank { session.zone }
        session.unit = if (options.unit == "millisecond") TimestampUnit.Millisecond else TimestampUnit.Second
    }
    val input = item.input.trim()
    val output = item.output.trim()
    if (timestampPattern.matches(input)) {
        session.timestamp = input
        if (output.isNotEmpty()) session.localTime = output
    } else if (timestampPattern.matches(output)) {
        session.localTime = input.ifBlank { session.localTime }
        session.timestamp = output
    } else {
        val value = output.ifBlank { input }
        if (timestampPattern.matches(value)) session.timestamp = value else session.localTime = value
    }
    session.error = ""
}

private fun copyField(container: AppContainer, session: TimeSession, value: String, refresh: () -> Unit) {
    session.notice = copyText(value, container)
    session.error = ""
    refresh()
}

private fun copyText(value: String, container: AppContainer): String {
    return try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        container.t("time.notice.copied")
    } catch (_: Exception) {
        container.t("json.notice.copyFailed")
    }
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? TimeException)?.code
    return when (code) {
        "dst-gap" -> container.t("time.error.dstGap")
        "dst-overlap" -> container.t("time.error.dstOverlap")
        "invalid-local-time" -> container.t("time.error.localTime")
        else -> container.t("time.error.timestamp")
    }
}
