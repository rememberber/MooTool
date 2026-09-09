package com.rememberber.mootool.next.compose.features.config

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ConfigEngine
import com.rememberber.mootool.next.compose.domain.ConfigException
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ConfigSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.charset.StandardCharsets

@Composable
fun ConfigConvertScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.configSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistConfig()
    }

    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.YmlProperties.id)
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("config.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            MooButton(container.t("common.action.clear"), onClick = {
                if (session.tab == "validate") {
                    session.validateSource = ""
                    session.validation = ""
                    session.valid = null
                } else {
                    session.properties = ""
                    session.yaml = ""
                }
                session.error = ""
                session.notice = container.t("json.notice.cleared")
                refresh()
            })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.YmlProperties) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MooButton(container.t("config.tab.convert"), primary = session.tab == "convert", onClick = {
                session.tab = "convert"
                session.error = ""
                refresh()
            })
            MooButton(container.t("config.tab.validate"), primary = session.tab == "validate", onClick = {
                session.tab = "validate"
                session.error = ""
                refresh()
            })
        }
        if (session.tab == "convert") {
            Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.properties"), color = colors.textSecondary, fontSize = 12.sp)
                    MooTextField(
                        session.properties,
                        { session.properties = it; session.error = ""; refresh() },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        singleLine = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MooButton(container.t("config.importProperties"), onClick = {
                            importText(container.t("config.importProperties"))?.let {
                                session.properties = it
                                session.notice = container.t("json.notice.imported")
                                session.error = ""
                                refresh()
                            }
                        })
                        MooButton(container.t("config.exportProperties"), onClick = {
                            exportText(container, session, session.properties, "config.properties")
                            refresh()
                        })
                    }
                }
                Column(
                    modifier = Modifier.width(176.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MooButton(container.t("config.toYaml"), primary = true, onClick = {
                        convert(container, session, toYaml = true)
                        refresh()
                    })
                    MooButton(container.t("config.toProperties"), onClick = {
                        convert(container, session, toYaml = false)
                        refresh()
                    })
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.yaml"), color = colors.textSecondary, fontSize = 12.sp)
                    MooTextField(
                        session.yaml,
                        { session.yaml = it; session.error = ""; refresh() },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        singleLine = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MooButton(container.t("config.importYaml"), onClick = {
                            importText(container.t("config.importYaml"))?.let {
                                session.yaml = it
                                session.notice = container.t("json.notice.imported")
                                session.error = ""
                                refresh()
                            }
                        })
                        MooButton(container.t("config.exportYaml"), onClick = {
                            exportText(container, session, session.yaml, "config.yaml")
                            refresh()
                        })
                    }
                }
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.yaml"), color = colors.textSecondary, fontSize = 12.sp)
                    MooTextField(
                        session.validateSource,
                        { session.validateSource = it; session.error = ""; session.valid = null; refresh() },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        singleLine = false
                    )
                }
                Column(
                    modifier = Modifier.width(140.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MooButton(container.t("config.validate"), primary = true, onClick = {
                        validate(container, session)
                        refresh()
                    })
                    MooButton(container.t("config.format"), onClick = {
                        format(container, session)
                        refresh()
                    })
                }
                Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.result"), color = colors.textSecondary, fontSize = 12.sp)
                    Column(
                        Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceSubtle)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            session.validation.ifEmpty { container.t("config.validate") },
                            color = if (session.valid == false) colors.danger else colors.textPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
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
        ConfigHistoryDialog(container, session, historyItems) { refresh() }
    }
}

private fun convert(container: AppContainer, session: ConfigSession, toYaml: Boolean) {
    val input = if (toYaml) session.properties else session.yaml
    val summary = container.t(if (toYaml) "config.toYaml" else "config.toProperties")
    runCatching {
        if (toYaml) ConfigEngine.propertiesToYaml(session.properties) else ConfigEngine.yamlToProperties(session.yaml)
    }.onSuccess { output ->
        if (toYaml) session.yaml = output else session.properties = output
        session.error = ""
        session.notice = summary
        container.history.save(ToolId.YmlProperties.id, summary, summary, input, output, if (toYaml) "toYaml" else "toProperties")
    }.onFailure { error ->
        session.notice = ""
        session.error = messageFor(container, error)
    }
}

private fun validate(container: AppContainer, session: ConfigSession) {
    val result = ConfigEngine.validateYaml(session.validateSource)
    session.valid = result.valid
    session.validation = if (result.valid) container.t("config.valid") else container.t("config.invalid", mapOf("message" to result.message))
    session.error = if (result.valid) "" else session.validation
    session.notice = if (result.valid) container.t("config.valid") else ""
    container.history.save(ToolId.YmlProperties.id, container.t("config.validate"), container.t("config.validate"), session.validateSource, session.validation, "validate")
}

private fun format(container: AppContainer, session: ConfigSession) {
    val input = session.validateSource
    runCatching { ConfigEngine.formatYaml(input) }
        .onSuccess { output ->
            session.validateSource = output
            session.valid = true
            session.validation = container.t("config.valid")
            session.error = ""
            session.notice = container.t("config.format")
            container.history.save(ToolId.YmlProperties.id, container.t("config.format"), container.t("config.format"), input, output, "format")
        }
        .onFailure { error ->
            session.valid = false
            session.notice = ""
            session.error = messageFor(container, error)
        }
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val config = error as? ConfigException
    return when (config?.code) {
        "conflict" -> container.t("config.error.conflict", mapOf("message" to (config.message ?: "")))
        "root" -> container.t("config.error.root")
        else -> container.t("config.error.generic", mapOf("message" to (error.message ?: "")))
    }
}

private fun importText(title: String): String? {
    val file = chooseFile(save = false, title = title) ?: return null
    return runCatching { file.readText(StandardCharsets.UTF_8) }.getOrNull()
}

private fun exportText(container: AppContainer, session: ConfigSession, content: String, kindKey: String) {
    if (content.isEmpty()) {
        session.notice = container.t("config.nothingToSave")
        return
    }
    val extension = if (kindKey == "config.yaml") "yml" else "properties"
    val file = chooseFile(save = true, title = container.t(kindKey), defaultName = "config.$extension") ?: return
    runCatching { file.writeText(content, StandardCharsets.UTF_8) }
        .onSuccess {
            session.error = ""
            session.notice = container.t("json.notice.exported")
        }
        .onFailure { error ->
            session.error = container.t("config.error.write", mapOf("message" to (error.message ?: file.path)))
        }
}

private fun chooseFile(save: Boolean, title: String, defaultName: String = ""): File? {
    val dialog = FileDialog(null as Frame?, title, if (save) FileDialog.SAVE else FileDialog.LOAD)
    if (defaultName.isNotEmpty()) dialog.file = defaultName
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val directory = dialog.directory ?: return null
    return File(directory, file)
}

@Composable
private fun ConfigHistoryDialog(
    container: AppContainer,
    session: ConfigSession,
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
                            applyHistory(session, item)
                            session.notice = container.t("json.notice.restored")
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.createdAt, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.YmlProperties.id)
                    session.historyOpen = false
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

private fun applyHistory(session: ConfigSession, item: HistoryRecord) {
    when (item.options) {
        "toProperties" -> {
            session.tab = "convert"
            session.yaml = item.input
            session.properties = item.output
        }
        "validate", "format" -> {
            session.tab = "validate"
            session.validateSource = item.input
            session.validation = item.output
            session.valid = item.options == "format" || item.output.isNotEmpty()
        }
        else -> {
            session.tab = "convert"
            session.properties = item.input
            session.yaml = item.output
        }
    }
    session.error = ""
}
