package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.ai.AiClient
import com.rememberber.mootool.next.compose.ai.AiDataAccessRequest
import com.rememberber.mootool.next.compose.ai.AiInstallMode
import com.rememberber.mootool.next.compose.ai.AiInstallPreview
import com.rememberber.mootool.next.compose.ai.AiInstallRequest
import com.rememberber.mootool.next.compose.ai.AiIntegrationStatus
import com.rememberber.mootool.next.compose.ai.AiOperation
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooSelect
import com.rememberber.mootool.next.compose.ui.components.MooSwitch
import com.rememberber.mootool.next.compose.ui.components.SettingRow
import com.rememberber.mootool.next.compose.ui.components.SettingsGroup
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiIntegrationSettingsPanel(container: AppContainer) {
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var client by remember { mutableStateOf(AiClient.Codex) }
    var mode by remember { mutableStateOf(AiInstallMode.Both) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<AiIntegrationStatus?>(null) }
    var preview by remember { mutableStateOf<AiInstallPreview?>(null) }
    var notesAccess by remember { mutableStateOf(false) }
    var jsonAccess by remember { mutableStateOf(false) }
    var revision by remember { mutableStateOf(0) }

    fun refresh() {
        scope.launch {
            busy = true
            error = ""
            runCatching {
                withContext(Dispatchers.IO) {
                    status = container.aiIntegration.getStatus(client)
                    val access = container.aiIntegration.getDataAccess()
                    notesAccess = access.notes != null
                    jsonAccess = access.json != null
                    preview = container.aiIntegration.preview(AiInstallRequest(client, mode))
                }
            }.onFailure { error = it.message ?: it.toString() }
            busy = false
        }
    }

    LaunchedEffect(client, mode, revision) { refresh() }

    SettingsGroup(container.t("settings.ai.title")) {
        Text(container.t("settings.ai.description"), color = colors.textSecondary, fontSize = 12.sp)
        Text(container.t("settings.ai.scope"), color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        SettingRow(container.t("settings.ai.client")) {
            MooSelect(
                options = listOf(
                    AiClient.Codex.name to "Codex",
                    AiClient.ClaudeCode.name to "Claude Code",
                    AiClient.Cursor.name to "Cursor"
                ),
                value = client.name,
                onChange = { value ->
                    client = AiClient.entries.first { it.name == value }
                    if (client == AiClient.Cursor) mode = AiInstallMode.Mcp
                }
            )
        }
        SettingRow(container.t("settings.ai.mode")) {
            MooSelect(
                options = buildList {
                    if (client != AiClient.Cursor) add(AiInstallMode.Both.name to "MCP + Skill")
                    add(AiInstallMode.Mcp.name to "MCP")
                    if (client != AiClient.Cursor) add(AiInstallMode.Skill.name to "Skill")
                },
                value = mode.name,
                onChange = { value -> mode = AiInstallMode.entries.first { it.name == value } }
            )
        }
        status?.let {
            Text(
                "MCP: ${stateLabel(container, it.mcp)}" +
                    if (client != AiClient.Cursor) " · Skill: ${stateLabel(container, it.skill)}" else "",
                color = colors.textBody,
                fontSize = 11.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            MooButton(
                container.t(if (busy) "common.loading" else "settings.ai.install"),
                enabled = !busy && preview != null,
                onClick = {
                    val id = preview?.id ?: return@MooButton
                    scope.launch {
                        busy = true
                        runCatching {
                            withContext(Dispatchers.IO) { container.aiIntegration.install(id) }
                            container.toastSuccess(container.t("settings.ai.installed"))
                            revision += 1
                        }.onFailure {
                            error = it.message ?: it.toString()
                            notifySettingsValidationFailure(container, error)
                        }
                        busy = false
                    }
                },
                p5Toolbar = true
            )
            MooButton(container.t("settings.ai.test"), enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    runCatching {
                        val result = withContext(Dispatchers.IO) { container.aiIntegration.testConnection() }
                        container.toastSuccess(container.t("settings.ai.connected", mapOf("count" to result.tools.size.toString())))
                    }.onFailure {
                        error = it.message ?: it.toString()
                        notifySettingsValidationFailure(container, error)
                    }
                    busy = false
                }
            }, p5Toolbar = true)
            MooButton(container.t("settings.ai.refresh"), enabled = !busy, onClick = { revision += 1 }, p5Toolbar = true)
        }
        if (error.isNotEmpty()) {
            Text(error, color = colors.danger, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
        SettingsGroup(container.t("settings.ai.dataTitle")) {
            Text(container.t("settings.ai.dataHint"), color = colors.textMuted, fontSize = 11.sp)
            SettingRow(container.t("settings.ai.access.notes")) {
                MooSwitch(checked = notesAccess, onCheckedChange = { enabled ->
                    scope.launch {
                        busy = true
                        runCatching {
                            withContext(Dispatchers.IO) {
                                container.aiIntegration.setDataAccess(AiDataAccessRequest(notes = enabled, json = jsonAccess))
                            }
                            revision += 1
                        }.onFailure { error = it.message ?: it.toString() }
                        busy = false
                    }
                })
            }
            SettingRow(container.t("settings.ai.access.json")) {
                MooSwitch(checked = jsonAccess, onCheckedChange = { enabled ->
                    scope.launch {
                        busy = true
                        runCatching {
                            withContext(Dispatchers.IO) {
                                container.aiIntegration.setDataAccess(AiDataAccessRequest(notes = notesAccess, json = enabled))
                            }
                            revision += 1
                        }.onFailure { error = it.message ?: it.toString() }
                        busy = false
                    }
                })
            }
        }
    }
}

private fun stateLabel(container: AppContainer, state: com.rememberber.mootool.next.compose.ai.AiComponentState): String =
    when (state) {
        com.rememberber.mootool.next.compose.ai.AiComponentState.NotInstalled -> container.t("settings.ai.state.not-installed")
        com.rememberber.mootool.next.compose.ai.AiComponentState.Installed -> container.t("settings.ai.state.installed")
        com.rememberber.mootool.next.compose.ai.AiComponentState.NeedsRepair -> container.t("settings.ai.state.needs-repair")
        com.rememberber.mootool.next.compose.ai.AiComponentState.Conflict -> container.t("settings.ai.state.conflict")
    }
