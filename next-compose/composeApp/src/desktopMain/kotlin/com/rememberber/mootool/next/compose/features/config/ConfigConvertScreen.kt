package com.rememberber.mootool.next.compose.features.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ConfigEngine
import com.rememberber.mootool.next.compose.domain.ConfigWiringPresentation
import com.rememberber.mootool.next.compose.domain.ConfigHistoryMetadata
import com.rememberber.mootool.next.compose.domain.ConfigHistoryRestore
import com.rememberber.mootool.next.compose.domain.ConfigException
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ConfigSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.statusPillContent
import com.rememberber.mootool.next.compose.ui.components.statusPillFill
import com.rememberber.mootool.next.compose.ui.components.MooStatusKind
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooToolTabsRow
import com.rememberber.mootool.next.compose.ui.components.IoThreePaneRow
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooConfigConvertPane
import com.rememberber.mootool.next.compose.ui.components.mooConfigTabsRow
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.charset.StandardCharsets

@Composable
fun ConfigConvertScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.configSession() }
    DismissModalOverlaysOnDispose(container, ToolId.YmlProperties) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistConfig()
    }


    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("config.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    add(OverflowAction(container.t("common.action.clear")) {
                        if (session.tab == "validate") {
                            session.validateSource = ""
                            session.validation = ""
                            session.valid = null
                        } else {
                            session.properties = ""
                            session.yaml = ""
                        }
                        session.error = ""
                        refresh()
                    })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.YmlProperties) })
                }
            )
        }
        MooToolTabsRow(modifier = Modifier.mooConfigTabsRow()) {
            MooToolTab(container.t("config.tab.convert"), selected = session.tab == "convert", onClick = {
                session.tab = "convert"
                session.error = ""
                refresh()
            })
            MooToolTab(container.t("config.tab.validate"), selected = session.tab == "validate", onClick = {
                session.tab = "validate"
                session.error = ""
                refresh()
            })
        }
        if (session.tab == "convert") {
            ConfigIoPanes(
                container = container,
                settings = settings,
                paneKey = "config-convert",
                middleRatio = 0.28f,
                minRight = 240f,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                left = {
                Column(Modifier.fillMaxSize().mooToolShell().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.properties"), color = colors.textSecondary, fontSize = 12.sp)
                    MooTextField(
                        session.properties,
                        {
                            applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                                session.properties = it
                                session.error = ""
                            }
                            refresh()
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        singleLine = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MooButton(container.t("config.importProperties"), p5Toolbar = true, onClick = {
                            importText(container.t("config.importProperties"))?.let {
                                session.properties = it
                                session.notice = container.t("json.notice.imported")
                                container.toastSuccess(container.t("json.notice.imported"))
                                session.error = ""
                                refresh()
                            }
                        })
                        MooButton(container.t("config.exportProperties"), p5Toolbar = true, onClick = {
                            exportText(container, session, session.properties, "config.properties")
                            refresh()
                        })
                    }
                }
                },
                middle = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MooButton(
                        container.t("config.toYaml"),
                        prominent = true,
                        enabled = ConfigWiringPresentation.canToYaml(session.properties),
                        p5Toolbar = true,
                        onClick = {
                            convert(container, session, toYaml = true)
                            refresh()
                        },
                    )
                    MooButton(
                        container.t("config.toProperties"),
                        enabled = ConfigWiringPresentation.canToProperties(session.yaml),
                        p5Toolbar = true,
                        onClick = {
                            convert(container, session, toYaml = false)
                            refresh()
                        },
                    )
                }
                },
                right = {
                Column(Modifier.fillMaxSize().mooToolShell().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.yaml"), color = colors.textSecondary, fontSize = 12.sp)
                    MooTextField(
                        session.yaml,
                        {
                            applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                                session.yaml = it
                                session.error = ""
                            }
                            refresh()
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        singleLine = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MooButton(container.t("config.importYaml"), p5Toolbar = true, onClick = {
                            importText(container.t("config.importYaml"))?.let {
                                session.yaml = it
                                session.notice = container.t("json.notice.imported")
                                container.toastSuccess(container.t("json.notice.imported"))
                                session.error = ""
                                refresh()
                            }
                        })
                        MooButton(container.t("config.exportYaml"), p5Toolbar = true, onClick = {
                            exportText(container, session, session.yaml, "config.yaml")
                            refresh()
                        })
                    }
                }
                }
            )
        } else {
            ConfigIoPanes(
                container = container,
                settings = settings,
                paneKey = "config-validate",
                middleRatio = 0.32f,
                minRight = 220f,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                left = {
                Column(Modifier.fillMaxSize().mooToolShell().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.yaml"), color = colors.textSecondary, fontSize = 12.sp)
                    MooTextField(
                        session.validateSource,
                        {
                            applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                                session.validateSource = it
                                session.error = ""
                                session.valid = null
                            }
                            refresh()
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        singleLine = false
                    )
                }
                },
                middle = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MooButton(
                        container.t("config.validate"),
                        prominent = true,
                        enabled = ConfigWiringPresentation.canValidateSource(session.validateSource),
                        p5Toolbar = true,
                        onClick = {
                            validate(container, session)
                            refresh()
                        },
                    )
                    MooButton(
                        container.t("config.format"),
                        enabled = ConfigWiringPresentation.canValidateSource(session.validateSource),
                        p5Toolbar = true,
                        onClick = {
                        format(container, session)
                        refresh()
                    })
                }
                },
                right = {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(container.t("config.result"), color = colors.textSecondary, fontSize = 12.sp)
                    val kind = when (session.valid) {
                        true -> MooStatusKind.Valid
                        false -> MooStatusKind.Error
                        null -> MooStatusKind.Neutral
                    }
                    val dark = MooTheme.dark
                    Column(
                        Modifier.weight(1f).fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (kind == MooStatusKind.Neutral) colors.surfaceSubtle
                                else statusPillFill(kind, dark)
                            )
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            session.validation.ifEmpty { container.t("config.validate") },
                            color = statusPillContent(kind, dark, colors.textPrimary),
                            fontSize = 11.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooConfigConvertPane().mooStatusBarBackground().padding(horizontal = 12.dp),
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
            toolId = ToolId.YmlProperties.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                ConfigHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

@Composable
private fun ConfigIoPanes(
    container: AppContainer,
    settings: AppSettings,
    paneKey: String,
    middleRatio: Float,
    minRight: Float,
    modifier: Modifier,
    left: @Composable () -> Unit,
    middle: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    IoThreePaneRow(container, settings, paneKey, middleRatio, minRight, modifier, left, middle, right)
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
        container.toastSuccess(summary)
        container.history.save(
            ToolId.YmlProperties.id,
            summary,
            summary,
            input,
            output,
            ConfigHistoryMetadata.encodeConvert(toYaml = toYaml),
        )
    }.onFailure { error ->
        session.notice = ""
        val message = messageFor(container, error)
        session.error = message
        container.toastError(message)
    }
}

private fun validate(container: AppContainer, session: ConfigSession) {
    val result = ConfigEngine.validateYaml(session.validateSource)
    session.valid = result.valid
    session.validation = if (result.valid) container.t("config.valid") else container.t("config.invalid", mapOf("message" to result.message))
    session.error = if (result.valid) "" else session.validation
    val notice = if (result.valid) container.t("config.valid") else ""
    session.notice = notice
    if (result.valid) container.toastSuccess(notice)
    container.history.save(
        ToolId.YmlProperties.id,
        container.t("config.validate"),
        container.t("config.validate"),
        session.validateSource,
        session.validation,
        ConfigHistoryMetadata.VALIDATE,
    )
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
            container.toastSuccess(session.notice)
            container.history.save(
                ToolId.YmlProperties.id,
                container.t("config.format"),
                container.t("config.format"),
                input,
                output,
                ConfigHistoryMetadata.FORMAT,
            )
        }
        .onFailure { error ->
            session.valid = false
            session.notice = ""
            val message = messageFor(container, error)
            session.error = message
            container.toastError(message)
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
            container.toastSuccess(container.t("json.notice.exported"))
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
