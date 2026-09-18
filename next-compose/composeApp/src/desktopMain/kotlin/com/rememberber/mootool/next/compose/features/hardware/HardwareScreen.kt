package com.rememberber.mootool.next.compose.features.hardware

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.HardwareEngine
import com.rememberber.mootool.next.compose.domain.HardwareWiringPresentation
import com.rememberber.mootool.next.compose.domain.HardwareTab
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooHardwareStatRow
import com.rememberber.mootool.next.compose.ui.components.mooHardwareToolbarMeta
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.sessions.HardwareSession
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.OnToolLeaveUnlessDetached
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        collectJob?.cancel()
        session.loading = true
        session.error = ""
        persist()
        collectJob = scope.launch(Dispatchers.Default) {
            val outcome = try {
                when (val collected = HardwareWiringPresentation.runCollect()) {
                    is HardwareWiringPresentation.CollectOutcome.Success -> Result.success(collected.snapshot)
                    is HardwareWiringPresentation.CollectOutcome.Failure -> Result.failure(collected.error)
                }
            } catch (e: CancellationException) {
                throw e
            }
            withContext(Dispatchers.Main) {
                session.loading = false
                outcome.onSuccess {
                    session.snapshot = it
                    session.error = ""
                    container.toastSuccess(container.t("hardware.refresh"))
                }.onFailure {
                    val message = it.message ?: container.t("hardware.error.generic")
                    notifyHardwareFailure(container, session, message, it)
                }
                persist()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session.snapshot == null && !session.loading) collect()
    }
    OnToolLeaveUnlessDetached(container, ToolId.Hardware) {
        collectJob?.cancel()
        session.loading = false
    }

    val snapshot = session.snapshot
    val groups = snapshot?.sections?.get(session.tab).orEmpty()
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("hardware.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                HardwareTab.entries.forEach { tab ->
                    MooToolTab(
                        container.t(tabKey(tab)),
                        selected = session.tab == tab,
                        onClick = { session.tab = tab; persist() }
                    )
                }
            }
            Text(
                snapshot?.collectedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("HH:mm:ss")).orEmpty(),
                color = colors.textMuted,
                fontSize = 10.sp,
                modifier = Modifier.mooHardwareToolbarMeta(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Checkbox(session.revealSensitive, {
                    session.revealSensitive = it
                    persist()
                })
                Text(container.t("hardware.revealSerials"), color = colors.textMuted, fontSize = 10.sp)
            }
            MooButton(
                container.t("hardware.refresh"),
                onClick = { collect() },
                enabled = HardwareWiringPresentation.refreshActionEnabled(session.loading),
                p5Toolbar = true,
            )
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(
                        OverflowAction(
                            container.t("hardware.copy"),
                            enabled = HardwareWiringPresentation.copyReportActionEnabled(
                                session.loading,
                                groups.isNotEmpty(),
                            ),
                        ) {
                        if (snapshot == null) return@OverflowAction
                        val text = HardwareEngine.plainText(snapshot, session.tab, session.revealSensitive) { container.t(it) }
                        if (!container.copyText(text)) {
                            val message = container.t("common.copyFailed")
                            notifyHardwareFailure(
                                container,
                                session,
                                message,
                                IllegalStateException(message),
                                local = true,
                            )
                            persist()
                        }
                    })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Hardware) })
                }
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .mooToolShell(p5 = true, endBorder = false)
        ) {
        when {
            session.error.isNotEmpty() -> Text(session.error, color = colors.danger, modifier = Modifier.padding(16.dp))
            session.loading && snapshot == null -> Text(container.t("hardware.loading"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            groups.isEmpty() -> Text(container.t("hardware.empty"), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp)) {
                groups.forEachIndexed { groupIndex, group ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                        Text(
                            title(container, group.titleKey),
                            color = colors.textStrong,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        group.items.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth()) {
                                pair.forEach { item ->
                                    Row(
                                        Modifier.weight(1f).mooHardwareStatRow().padding(end = 24.dp, top = 7.dp, bottom = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            container.t(item.labelKey),
                                            color = colors.textMuted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(0.45f)
                                        )
                                        Text(
                                            HardwareEngine.displayValue(item, session.revealSensitive),
                                            color = colors.textBody,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
                        }
                    }
                    if (groupIndex != groups.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
                    }
                }
            }
        }
        }
    }
    }
}

private fun notifyHardwareFailure(
    container: AppContainer,
    session: HardwareSession,
    message: String,
    error: Throwable,
    local: Boolean = false,
) {
    session.error = message
    val shouldToast = if (local) {
        HardwareWiringPresentation.shouldToastLocalFailure()
    } else {
        HardwareWiringPresentation.shouldToastCollectFailure(error)
    }
    if (shouldToast) {
        container.toastError(message)
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
