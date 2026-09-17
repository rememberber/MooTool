package com.rememberber.mootool.next.compose.features.time

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.TimeEngine
import com.rememberber.mootool.next.compose.domain.TimeException
import com.rememberber.mootool.next.compose.domain.TimeHistoryMetadata
import com.rememberber.mootool.next.compose.domain.TimeHistoryRestore
import com.rememberber.mootool.next.compose.domain.TimestampUnit
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.TimeSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooStatusPill
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooTimeCurrentBand
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput
import kotlinx.coroutines.delay
@Composable
fun TimeConvertScreen(container: AppContainer, detached: Boolean, active: Boolean) {
    val session = remember { container.sessionManager.timeSession() }
    DismissModalOverlaysOnDispose(container, ToolId.TimeConvert) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var zoneMenuOpen by remember { mutableStateOf(false) }
    var unitMenuOpen by remember { mutableStateOf(false) }
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

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("time.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("time.history")) { session.historyOpen = true; refresh() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.TimeConvert) })
                }
            )
        }
        Column(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(colors.workspace)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp)).verticalScroll(rememberScrollState())
        ) {

        CurrentBand(container, session, nowMillis, onChanged = { refresh() })

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(container.t("time.timezone"), color = colors.textMuted, fontSize = 11.sp)
            Box(Modifier.weight(1f)) {
                MooButton(TimeEngine.formatTimezoneLabel(session.zone, nowMillis), onClick = { zoneMenuOpen = true }, p5Toolbar = true)
                MooMenu(expanded = zoneMenuOpen, onDismissRequest = { zoneMenuOpen = false }) {
                    zones.forEach { zone ->
                        MooMenuItem(onClick = {
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
            Row(
                Modifier.clip(RoundedCornerShape(7.dp)).background(colors.control).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                TimeEngine.quickTimezones.forEach { (zone, label) ->
                    MooButton(
                        label,
                        primary = session.zone == zone,
                        onClick = {
                            session.zone = zone
                            session.localTime = TimeEngine.formatLocalTime(nowMillis, zone)
                            session.error = ""
                            refresh()
                        },
                        p5Toolbar = true
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(container.t("time.timestamp"), color = colors.textMuted, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooTextField(
                    session.timestamp,
                    {
                        session.onUserInput { session.timestamp = it; session.error = "" }
                        refresh()
                    },
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                        if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            convertToLocal(container, session, refresh = { refresh() })
                            true
                        } else false
                    }
                )
                MooButton(container.t("time.copy"), onClick = { copyField(container, session, session.timestamp, refresh = { refresh() }) }, p5Toolbar = true)
                Box {
                    MooButton(
                        if (session.unit == TimestampUnit.Second) container.t("time.unit.second") else container.t("time.unit.millisecond"),
                        onClick = { unitMenuOpen = true },
                        p5Toolbar = true
                    )
                    MooMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                        MooMenuItem(onClick = {
                            session.unit = TimestampUnit.Second
                            unitMenuOpen = false
                            refresh()
                        }) { Text(container.t("time.unit.second")) }
                        MooMenuItem(onClick = {
                            session.unit = TimestampUnit.Millisecond
                            unitMenuOpen = false
                            refresh()
                        }) { Text(container.t("time.unit.millisecond")) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(
                    container.t("time.toLocal"),
                    prominent = true,
                    onClick = { convertToLocal(container, session, refresh = { refresh() }) },
                    p5Toolbar = true
                )
                MooButton(
                    container.t("time.toTimestamp"),
                    onClick = { convertToTimestamp(container, session, refresh = { refresh() }) },
                    p5Toolbar = true
                )
            }

            Text("${container.t("time.localTime")} · ${session.zone}", color = colors.textMuted, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooTextField(
                    session.localTime,
                    {
                        session.onUserInput { session.localTime = it; session.error = "" }
                        refresh()
                    },
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                        if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            convertToTimestamp(container, session, refresh = { refresh() })
                            true
                        } else false
                    }
                )
                Text(container.t("time.formatHint"), color = colors.textMuted, fontSize = 10.sp)
                MooButton(container.t("time.copy"), onClick = { copyField(container, session, session.localTime, refresh = { refresh() }) }, p5Toolbar = true)
            }
        }

        if (session.error.isNotEmpty()) {
            Text(session.error, color = colors.danger, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session.notice.isNotEmpty()) {
                Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
        }
        }

        if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.TimeConvert.id,
            title = container.t("time.history"),
            onRestore = { item ->
                TimeHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
    if (session.clockOpen && active) {
        ClockOverlay(container, session, nowMillis) { refresh() }
    }
    }
}

@Composable
private fun CurrentBand(container: AppContainer, session: TimeSession, nowMillis: Long, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    val currentTimestamp = (nowMillis / 1000).toString()
    val currentLocal = TimeEngine.formatLocalTime(nowMillis, session.zone)
    Row(
        modifier = Modifier.fillMaxWidth().mooTimeCurrentBand().background(colors.surfaceSubtle)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(container.t("time.current"), color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TimeValue(container, container.t("time.timestamp"), currentTimestamp, Modifier.weight(0.8f)) {
                copyField(container, session, currentTimestamp, onChanged)
            }
            TimeValue(container, "${container.t("time.localTime")} · ${session.zone}", currentLocal, Modifier.weight(1.2f)) {
                copyField(container, session, currentLocal, onChanged)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            MooButton(container.t("time.clock"), onClick = { session.clockOpen = true; onChanged() }, p5Toolbar = true)
            MooStatusPill(TimeEngine.formatTimezoneLabel(session.zone, nowMillis))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
}

@Composable
private fun TimeValue(container: AppContainer, label: String, value: String, modifier: Modifier = Modifier, onCopy: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MooTheme.colors.textMuted, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                value,
                color = MooTheme.colors.textBody,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            MooButton(container.t("time.copy"), onClick = onCopy, p5Toolbar = true)
        }
    }
}


@Composable
private fun ClockOverlay(container: AppContainer, session: TimeSession, nowMillis: Long, onChanged: () -> Unit) {
    val formatted = TimeEngine.formatLocalTime(nowMillis, session.zone)
    val parts = formatted.split(" ")
    val date = parts.getOrNull(0).orEmpty()
    val time = parts.getOrNull(1).orEmpty()
    MooOverlay(
        onDismiss = { session.clockOpen = false; onChanged() },
        contentPadding = PaddingValues(0.dp),
        fillMaxSize = true
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF111113)),
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

private fun convertToLocal(container: AppContainer, session: TimeSession, refresh: () -> Unit) {
    runCatching { TimeEngine.timestampToLocal(session.timestamp, session.unit, session.zone) }
        .onSuccess { result ->
            session.localTime = result.localTime
            session.unit = result.unit
            session.error = ""
            val notice = container.t("time.notice.toLocal", mapOf("zone" to session.zone))
            session.notice = notice
            container.toastSuccess(notice)
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
            container.toastSuccess(notice)
            saveHistory(container, session, notice, session.localTime, result)
        }
        .onFailure { error ->
            session.notice = ""
            session.error = messageFor(container, error)
        }
    refresh()
}

private fun saveHistory(container: AppContainer, session: TimeSession, summary: String, input: String, output: String) {
    val options = TimeHistoryMetadata.encode(session.zone, session.unit)
    container.history.save(ToolId.TimeConvert.id, summary, summary, input, output, options)
}

private fun copyField(container: AppContainer, session: TimeSession, value: String, refresh: () -> Unit) {
    session.notice = copyText(value, container)
    session.error = ""
    refresh()
}

private fun copyText(value: String, container: AppContainer): String {
    return if (container.copyText(value)) container.t("time.notice.copied") else container.t("json.notice.copyFailed")
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
