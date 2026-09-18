package com.rememberber.mootool.next.compose.features.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.rememberber.mootool.next.compose.editor.FindHighlightTransformation
import com.rememberber.mootool.next.compose.editor.onFindBarRowKeys
import com.rememberber.mootool.next.compose.editor.onFindQueryEnterKey
import com.rememberber.mootool.next.compose.editor.selectedTextForFind
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.HostApplyConfig
import com.rememberber.mootool.next.compose.domain.HostEngine
import com.rememberber.mootool.next.compose.domain.HostErrorCode
import com.rememberber.mootool.next.compose.domain.HostException
import com.rememberber.mootool.next.compose.domain.HostHistoryMetadata
import com.rememberber.mootool.next.compose.domain.HostHistoryRestore
import com.rememberber.mootool.next.compose.domain.HostWiringPresentation
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.HostSession
import com.rememberber.mootool.next.compose.storage.HostProfile
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooCompactSearch
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooMenuSeparator
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooHostApplyButton
import com.rememberber.mootool.next.compose.ui.components.mooHostEditBar
import com.rememberber.mootool.next.compose.ui.components.mooHostProfilesPane
import com.rememberber.mootool.next.compose.ui.components.mooHostProfileSearch
import com.rememberber.mootool.next.compose.ui.components.mooHttpSavedItem
import com.rememberber.mootool.next.compose.ui.components.mooHttpSavedList
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooFindBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.mooEditorFrame
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.rememberber.mootool.next.compose.domain.ToolsExportWiringPresentation
import com.rememberber.mootool.next.compose.ui.chooseFileWithExportDirectory
import com.rememberber.mootool.next.compose.ui.persistToolsExportDirectory
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HostScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.hostSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Host) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    val config = remember(settings.data.directory) { HostApplyConfig.production(container.dataDirectories()) }
    var profiles by remember { mutableStateOf(emptyList<HostProfile>()) }

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistHost()
    }

    fun reloadProfiles() {
        profiles = container.hostProfiles.list(session.query, session.includeContent)
    }

    fun saveCurrent(showNotice: Boolean = true): Boolean {
        val nextName = session.name.trim()
        if (nextName.isEmpty()) {
            notifyHostFailure(container, session, container.t("host.error.name")) { persist() }
            return false
        }
        return runCatching {
            val saved = container.hostProfiles.save(session.selectedId.ifBlank { null }, nextName, session.content)
            session.markSaved(saved.id, saved.name, saved.content)
            if (showNotice) {
                session.notice = container.t("common.save")
                container.toastSuccess(container.t("common.save"))
            }
            session.error = ""
            reloadProfiles()
            container.notifyHostProfileMenuChanged()
            persist()
            true
        }.getOrElse {
            notifyHostFailure(container, session, it.message ?: container.t("host.error.generic")) { persist() }
            false
        }
    }

    fun selectProfile(profile: HostProfile) {
        if (session.dirty && session.name.isNotBlank() && !saveCurrent(showNotice = false)) return
        val loaded = container.hostProfiles.get(profile.id) ?: profile
        session.markSaved(loaded.id, loaded.name, loaded.content)
        session.error = ""
        persist()
    }

    var contentField by remember { mutableStateOf(TextFieldValue(session.content)) }
    fun openHostFindReplace() {
        contentField.selectedTextForFind()?.let { session.findQuery = it }
        session.findOpen = true
        session.findReplacedCount = 0
        persist()
    }

    fun closeHostFindReplace() {
        session.findOpen = false
        session.findReplacedCount = 0
        persist()
    }

    LaunchedEffect(session.selectedId, session.content, sessionGeneration) {
        if (contentField.text != session.content) {
            contentField = TextFieldValue(session.content, contentField.selection)
        }
    }

    LaunchedEffect(session.query, session.includeContent, revision, sessionGeneration) { reloadProfiles() }
    LaunchedEffect(settings.data.directory, sessionGeneration) {
        reloadProfiles()
        if (session.selectedId.isNotBlank()) {
            val loaded = container.hostProfiles.get(session.selectedId)
            when {
                loaded == null -> {
                    session.selectedId = ""
                    session.name = ""
                    session.content = ""
                    session.savedName = ""
                    session.savedContent = ""
                }
                !session.dirty -> session.markSaved(loaded.id, loaded.name, loaded.content)
            }
        }
        persist()
    }
    LaunchedEffect(Unit) {
        if (session.selectedId.isBlank() && session.name.isBlank() && session.content.isBlank()) {
            val first = container.hostProfiles.list().firstOrNull()
            if (first != null) session.markSaved(first.id, first.name, first.content)
        } else if (session.selectedId.isNotBlank()) {
            container.hostProfiles.get(session.selectedId)?.let { loaded ->
                if (!session.dirty) session.markSaved(loaded.id, loaded.name, loaded.content)
            }
        }
        persist()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
    Column(
        Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return@onPreviewKeyEvent false
            val meta = event.isMetaPressed || event.isCtrlPressed
            when {
                event.key == Key.Escape && session.findOpen -> {
                    closeHostFindReplace()
                    true
                }
                com.rememberber.mootool.next.compose.editor.EditorFindShortcutPolicy.opensShellFind(
                    ToolId.Host,
                    event.key,
                    meta = meta,
                    shift = event.isShiftPressed,
                    alt = event.isAltPressed,
                ) -> {
                    openHostFindReplace()
                    true
                }
                else -> false
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("host.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            if (session.error.isNotEmpty()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            else if (session.notice.isNotEmpty()) Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; persist() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Host) })
                }
            )
        }
        Row(Modifier.fillMaxSize()) {
            val listWidth = settings.layout.pane(ToolId.Host.id, 0, 220f, 180f, 320f)
            ProfileList(
                container = container,
                session = session,
                profiles = profiles,
                onSelect = ::selectProfile,
                onNew = {
                    if (session.dirty && session.name.isNotBlank() && !saveCurrent(showNotice = false)) return@ProfileList
                    session.selectedId = ""
                    session.name = container.t("host.untitled")
                    session.content = HostEngine.DEFAULT_TEMPLATE
                    session.savedName = ""
                    session.savedContent = ""
                    persist()
                },
                onCopy = {
                    if (session.selectedId.isBlank() && !saveCurrent(showNotice = false)) return@ProfileList
                    val sourceId = session.selectedId
                    if (sourceId.isBlank()) return@ProfileList
                    val copy = container.hostProfiles.duplicate(sourceId, container.t("host.copySuffix"))
                    session.markSaved(copy.id, copy.name, copy.content)
                    reloadProfiles()
                    container.notifyHostProfileMenuChanged()
                    persist()
                },
                onImport = {
                    if (session.dirty && session.name.isNotBlank() && !saveCurrent(showNotice = false)) return@ProfileList
                    val file = pickHostFile(container, false) ?: return@ProfileList
                    when (val outcome = HostWiringPresentation.runReadImportProfile(file)) {
                        is HostWiringPresentation.ImportProfileOutcome.Success -> {
                            session.selectedId = ""
                            session.name = HostWiringPresentation.inferImportProfileName(
                                file,
                                container.t("host.untitled"),
                            )
                            session.content = outcome.content
                            session.savedName = ""
                            session.savedContent = ""
                            session.error = ""
                            persist()
                        }
                        is HostWiringPresentation.ImportProfileOutcome.Failure -> {
                            val message = container.t(
                                "reformat.error.read",
                                mapOf("message" to (outcome.error.message ?: file.path)),
                            )
                            notifyHostFailure(
                                container,
                                session,
                                message,
                                outcome.error,
                                io = true,
                            ) { persist() }
                        }
                    }
                },
                onExport = {
                    val file = pickHostFile(
                        container,
                        true,
                        ToolsExportWiringPresentation.defaultHostExportFileName(session.name),
                    ) ?: return@ProfileList
                    when (val outcome = HostWiringPresentation.runWriteExportProfile(file, session.content)) {
                        HostWiringPresentation.ExportProfileOutcome.Success -> {
                            session.notice = container.t("host.exported")
                            container.toastSuccess(container.t("host.exported"))
                            persist()
                        }
                        is HostWiringPresentation.ExportProfileOutcome.Failure -> {
                            val message = container.t(
                                "reformat.error.write",
                                mapOf("message" to (outcome.error.message ?: file.path)),
                            )
                            notifyHostFailure(
                                container,
                                session,
                                message,
                                outcome.error,
                                io = true,
                            ) { persist() }
                        }
                    }
                },
                onDelete = { if (session.selectedId.isNotBlank()) { session.deleteConfirm = true; persist() } },
                onChanged = { persist(); reloadProfiles() },
                width = listWidth
            )
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Host.id, 0, listWidth + it, 1) },
                onReset = { container.setPaneSize(ToolId.Host.id, 0, 220f, 1) }
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .mooToolShell(p5 = true, endBorder = false)
            ) {
                Row(
                    Modifier.fillMaxWidth().mooHostEditBar().mooToolbarBackground()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MooTextField(
                        session.name,
                        {
                            applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                                session.name = it
                            }
                            persist()
                        },
                        modifier = Modifier.widthIn(min = 130.dp, max = 300.dp),
                        placeholder = container.t("host.profileName"),
                        dense = true
                    )
                    OverflowActionCluster(
                        overflow = overflow,
                        moreLabel = container.t("json.action.overflow"),
                        actions = listOf(
                            OverflowAction(container.t("host.current")) {
                                scope.launch(Dispatchers.IO) {
                                    val result = HostWiringPresentation.runReadSystem(config)
                                    withContext(Dispatchers.Main) {
                                        when (result) {
                                            is HostWiringPresentation.SystemReadOutcome.Success -> {
                                                val it = result.system
                                                session.systemPath = it.path
                                                session.systemContent = it.content
                                                session.systemWritable = it.writable
                                                session.systemFingerprint = it.fingerprint
                                                session.systemOpen = true
                                                session.error = ""
                                            }
                                            is HostWiringPresentation.SystemReadOutcome.Failure ->
                                                notifyHostFailure(
                                                    container,
                                                    session,
                                                    messageFor(container, result.error),
                                                ) { persist() }
                                        }
                                        persist()
                                    }
                                }
                            },
                            OverflowAction(container.t("host.find")) {
                                if (session.findOpen) {
                                    closeHostFindReplace()
                                } else {
                                    openHostFindReplace()
                                }
                            }
                        )
                    )
                    MooButton(
                        container.t("common.save"),
                        onClick = { saveCurrent() },
                        enabled = session.dirty,
                        p5Toolbar = true
                    )
                    MooButton(
                        if (session.applying) container.t("host.applying") else container.t("host.apply"),
                        prominent = true,
                        enabled = HostWiringPresentation.canOpenApplyConfirm(session.content, session.applying),
                        p5Toolbar = true,
                        modifier = Modifier.mooHostApplyButton(),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val preview = HostWiringPresentation.runBuildApplyPreview(config, session.content)
                                withContext(Dispatchers.Main) {
                                    when (preview) {
                                        is HostWiringPresentation.ApplyPreviewOutcome.Success -> {
                                            val current = preview.preview.system
                                            session.systemPath = current.path
                                            session.systemContent = current.content
                                            session.systemWritable = current.writable
                                            session.systemFingerprint = current.fingerprint
                                            session.applyDiff = preview.preview.diff
                                            session.applyConfirm = true
                                            session.error = ""
                                        }
                                        is HostWiringPresentation.ApplyPreviewOutcome.Failure ->
                                            notifyHostFailure(
                                                container,
                                                session,
                                                messageFor(container, preview.error),
                                            ) { persist() }
                                    }
                                    persist()
                                }
                            }
                        }
                    )
                    MooButton(
                        container.t("host.restore"),
                        enabled = HostWiringPresentation.canRestoreBackup(session.lastBackup, session.applying),
                        p5Toolbar = true,
                        onClick = {
                            val backup = session.lastBackup
                            session.applying = true
                            persist()
                            scope.launch(Dispatchers.IO) {
                                val result = HostWiringPresentation.runRestoreBackup(config, backup)
                                withContext(Dispatchers.Main) {
                                    session.applying = false
                                    when (result) {
                                        is HostWiringPresentation.RestoreOutcome.Success -> {
                                            val it = result.result
                                            session.systemPath = it.system.path
                                            session.systemContent = it.system.content
                                            session.systemWritable = it.system.writable
                                            session.systemFingerprint = it.system.fingerprint
                                            session.applyDiff = it.diff
                                            session.notice = container.t("host.restored")
                                            session.error = ""
                                        }
                                        is HostWiringPresentation.RestoreOutcome.Failure ->
                                            notifyHostFailure(
                                                container,
                                                session,
                                                messageFor(container, result.error),
                                            ) { persist() }
                                    }
                                    persist()
                                }
                            }
                        }
                    )
                }
                if (session.findOpen) {
                    FindBar(
                        container,
                        session,
                        contentField,
                        onContentFieldChange = { contentField = it },
                        onChanged = { persist() }
                    )
                }
                val findMatches = remember(session.content, session.findQuery, session.findOptions, session.findOpen) {
                    if (session.findOpen && session.findQuery.isNotBlank()) {
                        FindReplace.findAll(session.content, session.findQuery, session.findOptions)
                    } else {
                        emptyList()
                    }
                }
                val findHighlight = remember(findMatches, contentField.selection, colors) {
                    if (findMatches.isEmpty()) {
                        VisualTransformation.None
                    } else {
                        FindHighlightTransformation(
                            findMatches,
                            HostFindNavigation.currentMatch(findMatches, contentField),
                            colors.accent.copy(alpha = 0.24f),
                            colors.accent.copy(alpha = 0.52f),
                        )
                    }
                }
                MooTextField(
                    contentField,
                    { next ->
                        contentField = next
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            session.content = next.text
                        }
                        persist()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth().mooEditorFrame(flatten = true),
                    placeholder = container.t("host.placeholder"),
                    singleLine = false,
                    hostsContent = true,
                    visualTransformation = findHighlight,
                )
                Text(container.t("host.saveHint"), color = colors.textSecondary, fontSize = 11.sp)
            }
        }
    }
    }
    if (session.systemOpen) {
        MooOverlay(onDismiss = { session.systemOpen = false; persist() }) {
            Column(
                Modifier.width(720.dp).height(520.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("host.current"), color = colors.textPrimary)
                Text(session.systemPath, color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    if (session.systemWritable) container.t("host.writable") else container.t("host.requiresPrivilege"),
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
                SelectionContainer(Modifier.weight(1f).fillMaxWidth().border(1.dp, colors.borderControl, RoundedCornerShape(6.dp)).padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text(session.systemContent, color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.action.copy"), onClick = {
                        if (container.copyText(session.systemContent)) {
                            session.notice = container.t("common.copied")
                        } else {
                            session.notice = container.t("json.notice.copyFailed")
                        }
                        persist()
                    })
                    MooButton(container.t("common.close"), onClick = { session.systemOpen = false; persist() })
                }
            }
        }
    }
    if (session.applyConfirm) {
        MooOverlay(onDismiss = { session.applyConfirm = false; persist() }) {
            Column(
                Modifier.width(640.dp).height(480.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("host.confirmApply"), color = colors.textPrimary)
                Text(session.systemPath, color = colors.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(container.t("host.diff"), color = colors.textSecondary, fontSize = 12.sp)
                SelectionContainer(Modifier.weight(1f).fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(8.dp).verticalScroll(rememberScrollState())) {
                    Text(session.applyDiff, color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(
                        container.t("host.apply"),
                        prominent = true,
                        enabled = HostWiringPresentation.canConfirmApply(session.applying),
                        onClick = {
                        session.applying = true
                        persist()
                        scope.launch(Dispatchers.IO) {
                            val result = HostWiringPresentation.runApply(config, session.content, session.systemFingerprint)
                            withContext(Dispatchers.Main) {
                                session.applying = false
                                session.applyConfirm = false
                                when (result) {
                                    is HostWiringPresentation.ApplyOutcome.Success -> {
                                        val it = result.result
                                    session.systemPath = it.system.path
                                    session.systemContent = it.system.content
                                    session.systemWritable = it.system.writable
                                    session.systemFingerprint = it.system.fingerprint
                                    session.lastBackup = it.backupPath.orEmpty()
                                    session.applyDiff = it.diff
                                    val appliedNotice = if (it.dnsFlushed) container.t("host.applied") else container.t("host.appliedNoDns")
                                    session.notice = appliedNotice
                                    container.toastSuccess(appliedNotice)
                                    session.error = ""
                                    container.history.save(
                                        ToolId.Host.id,
                                        HostHistoryMetadata.OPERATION_APPLY,
                                        session.name.ifBlank { it.system.path },
                                        session.content.take(4_000),
                                        it.system.path,
                                        HostHistoryMetadata.encodeApplyBackup(it.backupPath.orEmpty()),
                                    )
                                    }
                                    is HostWiringPresentation.ApplyOutcome.Failure ->
                                        notifyHostFailure(
                                            container,
                                            session,
                                            messageFor(container, result.error),
                                        ) { persist() }
                                }
                                persist()
                            }
                        }
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.applyConfirm = false; persist() })
                }
            }
        }
    }
    if (session.renameOpen) {
        MooOverlay(onDismiss = { session.renameOpen = false; persist() }) {
            Column(
                Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(container.t("quickNote.rename"), color = colors.textPrimary)
                MooTextField(session.renameValue, { session.renameValue = it; persist() })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.save"), prominent = true, onClick = {
                        val nextName = session.renameValue.trim()
                        if (nextName.isEmpty() || session.selectedId.isBlank()) {
                            notifyHostFailure(container, session, container.t("host.error.name")) { persist() }
                            return@MooButton
                        }
                        runCatching { container.hostProfiles.save(session.selectedId, nextName, session.content) }
                            .onSuccess { saved ->
                                session.markSaved(saved.id, saved.name, saved.content)
                                session.renameOpen = false
                                session.notice = container.t("common.save")
                                container.toastSuccess(container.t("common.save"))
                                session.error = ""
                                reloadProfiles()
                                container.notifyHostProfileMenuChanged()
                            }
                            .onFailure {
                                notifyHostFailure(
                                    container,
                                    session,
                                    it.message ?: container.t("host.error.generic"),
                                ) { persist() }
                            }
                        persist()
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.renameOpen = false; persist() })
                }
            }
        }
    }
    if (session.deleteConfirm) {
        MooOverlay(onDismiss = { session.deleteConfirm = false; persist() }) {
            Column(
                Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(container.t("host.confirmDelete"), color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.delete"), danger = true, onClick = {
                        val id = session.selectedId
                        if (id.isNotBlank()) container.hostProfiles.delete(id)
                        session.selectedId = ""
                        session.name = ""
                        session.content = ""
                        session.savedName = ""
                        session.savedContent = ""
                        session.deleteConfirm = false
                        reloadProfiles()
                        container.notifyHostProfileMenuChanged()
                        persist()
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.deleteConfirm = false; persist() })
                }
            }
        }
    }
    if (session.historyOpen) HistoryBrowser(
        container = container,
        toolId = ToolId.Host.id,
        title = container.t("common.action.history"),
        onRestore = { item ->
            HostHistoryRestore.apply(session, item)
            session.historyOpen = false
            persist()
        },
        onDismiss = { session.historyOpen = false; persist() }
    )
}

@Composable
private fun ProfileList(
    container: AppContainer,
    session: HostSession,
    profiles: List<HostProfile>,
    onSelect: (HostProfile) -> Unit,
    onNew: () -> Unit,
    onCopy: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onChanged: () -> Unit,
    width: Float
) {
    val colors = MooTheme.colors
    val stamp = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()) }
    val contextMenuFirstFocus = remember { FocusRequester() }
    Column(
        Modifier.width(width.dp).fillMaxHeight().mooHostProfilesPane().padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooCompactSearch(
            session.query,
            { session.query = it; onChanged() },
            placeholder = container.t("common.search"),
            modifier = Modifier.fillMaxWidth().mooHostProfileSearch(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.clip(RoundedCornerShape(4.dp)).background(if (session.includeContent) colors.accent else colors.workspace)
                    .mooFocusClickable(shape = RoundedCornerShape(4.dp), enabled = HostWiringPresentation.canToggleContentSearch(session.applying)) {
                        session.includeContent = !session.includeContent
                        onChanged()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    container.t("host.searchContent"),
                    color = if (session.includeContent) colors.onAccent else colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.new"), onClick = onNew)
        }
        if (profiles.isEmpty()) {
            val emptyKey =
                if (HostWiringPresentation.showFilteredEmpty(profiles.size, session.query)) {
                    "json.notice.noMatches"
                } else {
                    "host.empty"
                }
            Text(container.t(emptyKey), color = colors.textSecondary, fontSize = 12.sp)
        } else {
            LazyColumn(Modifier.weight(1f).mooHttpSavedList()) {
                items(profiles, key = { it.id }) { profile ->
                    val menuOpen = session.profileContextMenuId == profile.id
                    LaunchedEffect(menuOpen) {
                        if (menuOpen) contextMenuFirstFocus.requestFocus()
                    }
                    Box {
                    val interaction = remember(profile.id) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val active = profile.id == session.selectedId
                    val shape = RoundedCornerShape(5.dp)
                    Column(
                        Modifier.fillMaxWidth()
                            .mooHttpSavedItem(active = active, hovered = hovered)
                            .hoverable(interaction)
                            .pointerInput(profile.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                            event.changes.forEach { it.consume() }
                                            onSelect(profile)
                                            session.profileContextMenuId = profile.id
                                            onChanged()
                                        }
                                    }
                                }
                            }
                            .mooFocusClickable(shape = shape) { onSelect(profile) },
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            profile.name,
                            color = colors.textBody,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stamp.format(Instant.ofEpochMilli(profile.modifiedAt)),
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    MooMenu(expanded = menuOpen, onDismissRequest = { session.profileContextMenuId = ""; onChanged() }) {
                        MooMenuItem(
                            container.t("common.rename"),
                            modifier = Modifier.focusRequester(contextMenuFirstFocus),
                        ) {
                            session.profileContextMenuId = ""
                            onSelect(profile)
                            session.renameValue = profile.name
                            session.renameOpen = true
                            onChanged()
                        }
                        MooMenuItem(container.t("host.export")) {
                            session.profileContextMenuId = ""
                            onSelect(profile)
                            onExport()
                        }
                        MooMenuSeparator()
                        MooMenuItem(container.t("common.delete")) {
                            session.profileContextMenuId = ""
                            onSelect(profile)
                            onDelete()
                        }
                    }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("host.import"), onClick = onImport)
            MooButton(container.t("host.export"), onClick = onExport, enabled = session.content.isNotBlank())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("host.copy"), onClick = onCopy, enabled = session.selectedId.isNotBlank() || session.content.isNotBlank())
            MooButton(container.t("common.delete"), onClick = onDelete, enabled = session.selectedId.isNotBlank())
        }
    }
}

@Composable
private fun FindBar(
    container: AppContainer,
    session: HostSession,
    contentField: TextFieldValue,
    onContentFieldChange: (TextFieldValue) -> Unit,
    onChanged: () -> Unit,
) {
    val matches = FindReplace.findAll(session.content, session.findQuery, session.findOptions)
    val colors = MooTheme.colors
    val findFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        findFocus.requestFocus()
    }
    fun jumpFind(forward: Boolean) {
        if (session.findQuery.isBlank()) return
        HostFindNavigation.jump(session.content, session.findQuery, session.findOptions, contentField, forward = forward)
            ?.let(onContentFieldChange)
            ?: run { container.toastFindNoMatches() }
        onChanged()
    }
    fun closeFind() {
        session.findOpen = false
        session.findReplacedCount = 0
        onChanged()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .mooFindBarBackground()
            .onFindBarRowKeys(
                onPrevious = { jumpFind(false) },
                onNext = { jumpFind(true) },
                onClose = ::closeFind,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(
            session.findQuery,
            {
                session.findQuery = it
                session.findReplacedCount = 0
                onChanged()
            },
            modifier = Modifier.width(180.dp),
            placeholder = container.t("host.findPlaceholder"),
            dense = true,
            fieldModifier = Modifier
                .focusRequester(findFocus)
                .onFindQueryEnterKey { jumpFind(true) },
        )
        MooButton(
            container.t("find.find"),
            enabled = session.findQuery.isNotBlank(),
            onClick = { jumpFind(true) },
            p5Toolbar = true,
        )
        MooTextField(session.replaceText, { session.replaceText = it; onChanged() }, modifier = Modifier.width(140.dp), placeholder = container.t("host.replacePlaceholder"), dense = true)
        MooButton(container.t("find.matchCase") + ": ${session.findOptions.matchCase}", onClick = {
            session.findOptions = session.findOptions.copy(matchCase = !session.findOptions.matchCase)
            session.findReplacedCount = 0
            onChanged()
        }, p5Toolbar = true)
        MooButton(container.t("find.wholeWord") + ": ${session.findOptions.wholeWord}", onClick = {
            session.findOptions = session.findOptions.copy(wholeWord = !session.findOptions.wholeWord)
            session.findReplacedCount = 0
            onChanged()
        }, p5Toolbar = true)
        MooButton(container.t("find.regex") + ": ${session.findOptions.regex}", onClick = {
            session.findOptions = session.findOptions.copy(regex = !session.findOptions.regex)
            session.findReplacedCount = 0
            onChanged()
        }, p5Toolbar = true)
        Text(
            "${container.t("find.foundPrefix")} ${matches.size}",
            color = colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("find.previous"), onClick = { jumpFind(false) }, p5Toolbar = true)
        MooButton(container.t("find.next"), onClick = { jumpFind(true) }, p5Toolbar = true)
        MooButton(container.t("find.replace"), onClick = {
            val selection = contentField.selection
            val caret = maxOf(selection.start, selection.end)
            val (next, match) = FindReplace.replaceCurrent(
                session.content,
                session.findQuery,
                session.replaceText,
                session.findOptions,
                caret,
                selection.start,
                selection.end,
            )
            if (match == null) {
                container.toastFindNoMatches()
            } else {
                session.content = next
                session.findReplacedCount += 1
                session.notice = ""
                onContentFieldChange(
                    HostFindNavigation.afterReplaceAndSelectNext(
                        next,
                        session.findQuery,
                        session.findOptions,
                        contentField,
                        match
                    )
                )
            }
            onChanged()
        }, p5Toolbar = true)
        MooButton(container.t("find.replaceAll"), onClick = {
            val (next, count) = FindReplace.replaceAll(session.content, session.findQuery, session.replaceText, session.findOptions)
            if (count == 0) {
                container.toastFindNoMatches()
            } else {
                session.content = next
                onContentFieldChange(contentField.copy(text = next))
                session.findReplacedCount = count
                session.notice = ""
            }
            onChanged()
        }, p5Toolbar = true)
        Text(
            "${container.t("find.replacedPrefix")} ${session.findReplacedCount}",
            color = colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("common.close"), onClick = ::closeFind, p5Toolbar = true)
    }
}


private fun pickHostFile(container: AppContainer, save: Boolean, defaultName: String = "hosts.txt"): File? {
    val title = if (save) "Export Host" else "Import Host"
    val file = chooseFileWithExportDirectory(
        container,
        save = save,
        title = title,
        defaultFileName = if (save) defaultName else "",
    ) ?: return null
    if (save) persistToolsExportDirectory(container, file)
    return file
}

private fun notifyHostFailure(
    container: AppContainer,
    session: HostSession,
    message: String,
    error: Throwable = IllegalStateException(message),
    io: Boolean = false,
    onPersist: () -> Unit,
) {
    session.error = message
    val shouldToast = if (io) {
        HostWiringPresentation.shouldToastIoFailure(error)
    } else {
        HostWiringPresentation.shouldToastOperationFailure(error)
    }
    if (shouldToast) {
        container.toastError(message)
    }
    onPersist()
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? HostException)?.code
    val key = when (code) {
        HostErrorCode.INVALID -> "host.error.invalid"
        HostErrorCode.EMPTY -> "host.error.empty"
        HostErrorCode.CONFLICT -> "host.error.conflict"
        HostErrorCode.PERMISSION -> "host.error.permission"
        HostErrorCode.MISSING -> "host.error.missing"
        HostErrorCode.COMMAND_FAILED, null -> "host.error.generic"
    }
    val localized = container.t(key)
    val detail = error.message?.takeIf { it.isNotBlank() && it != "PERMISSION" && it != "Invalid hosts content" }
    return if (detail != null && localized != key) "$localized ($detail)" else localized
}
