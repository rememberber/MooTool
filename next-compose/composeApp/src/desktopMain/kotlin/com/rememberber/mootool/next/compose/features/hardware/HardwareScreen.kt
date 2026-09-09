package com.rememberber.mootool.next.compose.features.hardware

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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.HardwareEngine
import com.rememberber.mootool.next.compose.domain.HardwareTab
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HardwareScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.hardwareSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var collectJob by remember { mutableStateOf<Job?>(null) }

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistHardware()
    }

    fun collect() {
        if (session.loading) return
        session.loading = true
        session.error = ""
        persist()
        collectJob?.cancel()
        collectJob = scope.launch(Dispatchers.Default) {
            val result = runCatching { HardwareEngine.collect() }
            withContext(Dispatchers.Main) {
                session.loading = false
                result.onSuccess {
                    session.snapshot = it
                    session.error = ""
                }.onFailure {
                    session.error = it.message ?: container.t("hardware.error.generic")
                }
                persist()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session.snapshot == null && !session.loading) collect()
    }
    DisposableEffect(Unit) {
        onDispose {
            collectJob?.cancel()
            session.loading = false
        }
    }

    val snapshot = session.snapshot
    val groups = snapshot?.sections?.get(session.tab).orEmpty()
    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("hardware.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            Text(
                snapshot?.collectedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("HH:mm:ss")).orEmpty(),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(session.revealSensitive, {
                    session.revealSensitive = it
                    persist()
                })
                Text(container.t("hardware.revealSerials"), color = colors.textSecondary, fontSize = 12.sp)
            }
            MooButton(container.t("hardware.copy"), onClick = {
                if (snapshot == null) return@MooButton
                val text = HardwareEngine.plainText(snapshot, session.tab, session.revealSensitive) { container.t(it) }
                runCatching {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                    container.setStatus(container.t("json.notice.copied"))
                }.onFailure { session.error = container.t("common.copyFailed"); persist() }
            }, enabled = groups.isNotEmpty() && !session.loading)
            MooButton(container.t("hardware.refresh"), onClick = { collect() }, enabled = !session.loading)
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Hardware) })
            }
        }
        Row(
            Modifier.fillMaxWidth().background(colors.toolbar).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HardwareTab.entries.forEach { tab ->
                MooButton(
                    container.t(tabKey(tab)),
                    primary = session.tab == tab,
                    onClick = { session.tab = tab; persist() }
                )
            }
        }
        when {
            session.error.isNotEmpty() -> Text(session.error, color = colors.danger, modifier = Modifier.padding(16.dp))
            session.loading && snapshot == null -> Text(container.t("hardware.loading"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            groups.isEmpty() -> Text(container.t("hardware.empty"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groups.forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title(container, group.titleKey), color = colors.textPrimary, fontSize = 15.sp)
                        group.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    container.t(item.labelKey),
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(160.dp)
                                )
                                Text(
                                    HardwareEngine.displayValue(item, session.revealSensitive),
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tabKey(tab: HardwareTab) = when (tab) {
    HardwareTab.System -> "hardware.tab.system"
    HardwareTab.Cpu -> "hardware.tab.cpu"
    HardwareTab.Memory -> "hardware.tab.memory"
    HardwareTab.Storage -> "hardware.tab.storage"
    HardwareTab.Network -> "hardware.tab.network"
}

private fun title(container: AppContainer, key: String): String =
    if (key.startsWith("hardware.")) container.t(key) else key
