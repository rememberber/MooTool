package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DiffEngine
import com.rememberber.mootool.next.compose.domain.GitCommitInfo
import com.rememberber.mootool.next.compose.domain.GitDiffPreview
import com.rememberber.mootool.next.compose.domain.GitDiffSelection
import com.rememberber.mootool.next.compose.domain.GitEditorFlushPolicy
import com.rememberber.mootool.next.compose.domain.GitVaultFlushAction
import com.rememberber.mootool.next.compose.domain.GitEngine
import com.rememberber.mootool.next.compose.domain.GitOperationPresentation
import com.rememberber.mootool.next.compose.domain.GitRemoteCommitResult
import com.rememberber.mootool.next.compose.domain.SettingsVaultGitNormalize
import com.rememberber.mootool.next.compose.domain.GitFileDiff
import com.rememberber.mootool.next.compose.domain.GitStatus
import com.rememberber.mootool.next.compose.features.diff.annotateSide
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.MooStatusKind
import com.rememberber.mootool.next.compose.ui.components.MooStatusPill
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.rememberPairedScrollStates
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VaultGitDialog(
    container: AppContainer,
    title: String,
    defaultMessage: String,
    root: Path,
    onDismiss: () -> Unit,
    onFlush: () -> String?,
    onVaultRefresh: () -> Unit = {},
    /** commit/push/pull 等刷新面板 status 后同步工具栏 Git 角标（对齐 Electron `refreshGitChangeCount`）。 */
    onGitStatusChanged: () -> Unit = {},
) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(GitStatus(available = false, repository = false)) }
    var history by remember { mutableStateOf(emptyList<GitCommitInfo>()) }
    var tab by remember { mutableStateOf("changes") }
    var selected by remember { mutableStateOf("") }
    var fileDiffs by remember { mutableStateOf<List<GitFileDiff>>(emptyList()) }
    var selectedDiffPath by remember { mutableStateOf("") }
    var remote by remember { mutableStateOf(settings.vault.gitRemote) }
    var message by remember { mutableStateOf(defaultMessage) }
    var busy by remember { mutableStateOf(true) }
    var confirmAbort by remember { mutableStateOf(false) }
    var confirmDiscardPath by remember { mutableStateOf<String?>(null) }

    fun clearDiffSelection() {
        selected = ""
        fileDiffs = emptyList()
        selectedDiffPath = ""
    }

    fun identity() = GitEngine.identityFrom(settings.vault.gitUsername)
    fun token() = settings.vault.gitToken.trim()

    fun syncRemoteField(gitStatus: GitStatus) {
        remote = gitStatus.remote.ifBlank { settings.vault.gitRemote }
    }

    fun load() {
        scope.launch {
            busy = true
            try {
                val nextStatus = withContext(Dispatchers.IO) { GitEngine.status(root) }
                val nextHistory = withContext(Dispatchers.IO) {
                    if (nextStatus.repository) GitEngine.history(root) else emptyList()
                }
                status = nextStatus
                history = nextHistory
                syncRemoteField(nextStatus)
            } catch (error: Exception) {
                container.toastError(error.message?.ifBlank { null } ?: container.t("json.notice.failed"))
            } finally {
                busy = false
            }
        }
    }

    fun runAction(
        workingTree: GitVaultFlushAction? = null,
        afterSuccess: (() -> Unit)? = null,
        remoteOverrideAfterReload: String? = null,
        block: () -> com.rememberber.mootool.next.compose.domain.GitActionResult,
    ) {
        scope.launch {
            busy = true
            try {
                if (GitEditorFlushPolicy.shouldFlushEditor(workingTree)) {
                    val flushed = onFlush()
                    if (flushed != null) {
                        container.toastError(flushed)
                        return@launch
                    }
                }
                val result = withContext(Dispatchers.IO) { block() }
                if (result.success) {
                    container.toastSuccess(container.t("git.done"))
                    afterSuccess?.invoke()
                    if (GitEditorFlushPolicy.refreshesVaultAfterSuccess(workingTree)) {
                        onVaultRefresh()
                    }
                } else {
                    container.toastError(result.message.ifBlank { container.t("json.notice.failed") })
                    if (workingTree == GitVaultFlushAction.Pull) {
                        onVaultRefresh()
                    } else {
                        return@launch
                    }
                }
                val nextStatus = withContext(Dispatchers.IO) { GitEngine.status(root) }
                val nextHistory = withContext(Dispatchers.IO) {
                    if (nextStatus.repository) GitEngine.history(root) else emptyList()
                }
                status = nextStatus
                history = nextHistory
                syncRemoteField(nextStatus)
                if (remoteOverrideAfterReload != null) {
                    remote = remoteOverrideAfterReload
                }
                onGitStatusChanged()
            } catch (error: Exception) {
                container.toastError(error.message?.ifBlank { null } ?: container.t("json.notice.failed"))
            } finally {
                busy = false
            }
        }
    }

    fun loadFileDiffs(path: String? = null, commit: String? = null) {
        scope.launch {
            busy = true
            fileDiffs = emptyList()
            selectedDiffPath = ""
            try {
                val next = withContext(Dispatchers.IO) {
                    GitEngine.fileDiffs(root, path = path, commit = commit)
                }
                fileDiffs = next
                selectedDiffPath = GitDiffSelection.selected(next, "")?.path.orEmpty()
            } catch (error: Exception) {
                container.toastError(error.message?.ifBlank { null } ?: container.t("json.notice.failed"))
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(root) { load() }

    confirmDiscardPath?.let { discardPath ->
        MooOverlay(onDismiss = { confirmDiscardPath = null }) {
            Column(
                Modifier.width(480.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    container.t("git.confirmDiscard", mapOf("path" to discardPath)),
                    color = colors.textPrimary,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(
                        container.t("git.discard"),
                        prominent = true,
                        danger = true,
                        p5Toolbar = true,
                        enabled = !busy,
                        onClick = {
                            confirmDiscardPath = null
                            runAction(
                                workingTree = GitVaultFlushAction.Discard,
                                afterSuccess = { clearDiffSelection() }
                            ) { GitEngine.discard(root, discardPath) }
                        }
                    )
                    MooButton(container.t("common.cancel"), p5Toolbar = true, onClick = { confirmDiscardPath = null })
                }
            }
        }
    }

    if (confirmAbort) {
        MooOverlay(onDismiss = { confirmAbort = false }) {
            Column(
                Modifier.width(480.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(container.t(GitOperationPresentation.confirmAbortKey(status.operation)), color = colors.textPrimary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(
                        container.t(GitOperationPresentation.abortButtonKey(status.operation)),
                        prominent = true,
                        danger = true,
                        p5Toolbar = true,
                        enabled = !busy,
                        onClick = {
                            confirmAbort = false
                            runAction(
                                workingTree = GitVaultFlushAction.AbortMerge,
                                afterSuccess = { clearDiffSelection() }
                            ) { GitEngine.abortMerge(root) }
                        }
                    )
                    MooButton(container.t("common.cancel"), p5Toolbar = true, onClick = { confirmAbort = false })
                }
            }
        }
    }

    MooOverlay(onDismiss = onDismiss) {
        Column(
            Modifier.width(920.dp).height(620.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MooPageTitle(title)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    when {
                        !status.repository -> Text(container.t("git.noRepo"), color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        else -> {
                            Text(
                                container.t("git.branch", mapOf("branch" to status.branch.ifBlank { "HEAD" })),
                                color = colors.textStrong,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                container.t("git.sync", mapOf("ahead" to status.ahead.toString(), "behind" to status.behind.toString())),
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            GitOperationPresentation.inProgressMessageKey(status.operation, status.merging)?.let { key ->
                                MooStatusPill(
                                    container.t(key),
                                    kind = if (status.conflicts > 0) MooStatusKind.Error else MooStatusKind.Valid,
                                )
                            }
                            if (status.merging || status.conflicts > 0) {
                                Text(
                                    container.t(
                                        "git.counts",
                                        mapOf(
                                            "changes" to status.changes.size.toString(),
                                            "conflicts" to status.conflicts.toString(),
                                        ),
                                    ),
                                    color = if (status.conflicts > 0) colors.danger else colors.textMuted,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MooButton(container.t("git.refresh"), p5Toolbar = true, enabled = !busy, onClick = { load() })
                    if (!status.repository) {
                        MooButton(
                            container.t("git.init"),
                            prominent = true,
                            p5Toolbar = true,
                            enabled = !busy && status.available,
                            onClick = { runAction { GitEngine.init(root, identity()) } }
                        )
                    }
                    if (status.repository) {
                        MooButton(
                            container.t("git.fetch"),
                            p5Toolbar = true,
                            enabled = !busy && status.remote.isNotBlank(),
                            onClick = { runAction { GitEngine.fetch(root, token = token()) } }
                        )
                        MooButton(
                            container.t("git.pull"),
                            p5Toolbar = true,
                            enabled = !busy && GitOperationPresentation.pullEnabled(
                                remotePresent = status.remote.isNotBlank(),
                                merging = status.merging,
                                conflicts = status.conflicts,
                            ),
                            onClick = { runAction(workingTree = GitVaultFlushAction.Pull) { GitEngine.pull(root, token = token()) } }
                        )
                        MooButton(
                            container.t("git.push"),
                            p5Toolbar = true,
                            enabled = !busy && GitOperationPresentation.pushEnabled(
                                remotePresent = status.remote.isNotBlank(),
                                merging = status.merging,
                            ),
                            onClick = { runAction(workingTree = GitVaultFlushAction.Push) { GitEngine.push(root, token = token()) } }
                        )
                    }
                    if (status.merging || status.conflicts > 0) {
                        MooButton(
                            container.t(GitOperationPresentation.abortButtonKey(status.operation)),
                            danger = true,
                            p5Toolbar = true,
                            enabled = !busy,
                            onClick = { confirmAbort = true }
                        )
                    }
                    if (status.merging && status.conflicts == 0) {
                        MooButton(
                            container.t("git.continue"),
                            p5Toolbar = true,
                            enabled = !busy,
                            onClick = {
                                runAction(workingTree = GitVaultFlushAction.ContinueOperation) {
                                    GitEngine.continueOperation(root, identity())
                                }
                            }
                        )
                    }
                }
            }
            if (status.available) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(container.t("git.remote"), color = colors.textMuted, fontSize = 11.sp)
                    MooTextField(remote, { remote = it }, placeholder = container.t("git.remotePlaceholder"), modifier = Modifier.weight(1f))
                    val remoteTrimmed = remote.trim()
                    val remoteLabel = if (remoteTrimmed.isNotEmpty()) container.t("git.saveRemote") else container.t("git.removeRemote")
                    MooButton(
                        remoteLabel,
                        p5Toolbar = true,
                        enabled = !busy && status.repository && (remoteTrimmed.isNotEmpty() || status.remote.isNotBlank()),
                        onClick = {
                            val committed = when (val outcome = SettingsVaultGitNormalize.commitGitRemote(remote)) {
                                GitRemoteCommitResult.Cleared -> ""
                                is GitRemoteCommitResult.Accepted -> outcome.remote
                                GitRemoteCommitResult.Rejected -> {
                                    container.toastError(container.t("settings.vault.gitRemoteInvalid"))
                                    return@MooButton
                                }
                            }
                            remote = committed
                            runAction(
                                remoteOverrideAfterReload = committed,
                                afterSuccess = {
                                    container.updateSettings { current ->
                                        current.copy(vault = current.vault.copy(gitRemote = committed))
                                    }
                                },
                            ) { GitEngine.setRemote(root, committed) }
                        }
                    )
                }
            }
            if (!status.available) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(container.t("git.unavailable"), color = colors.textMuted, fontSize = 12.sp)
                }
            } else if (status.available) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(430.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, colors.borderSoft, RoundedCornerShape(6.dp))
                ) {
                    Column(
                        Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .border(1.dp, colors.borderSoft, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    ) {
                        GitPanelTabs(
                            changesLabel = container.t("git.changes") + " ${status.changes.size}",
                            historyLabel = container.t("git.history"),
                            tab = tab,
                            onTab = { next ->
                                tab = next
                                selected = ""
                                fileDiffs = emptyList()
                                selectedDiffPath = ""
                            }
                        )
                        Column(
                            Modifier.weight(1f).fillMaxWidth().padding(5.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (tab == "changes") {
                                if (status.changes.isEmpty()) {
                                    Text(container.t("git.emptyChanges"), color = colors.textMuted, fontSize = 11.sp)
                                } else {
                                    LazyColumn(Modifier.weight(1f)) {
                                        items(status.changes, key = { "${it.status}-${it.path}" }) { change ->
                                            val selectedRow = change.path == selected
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .then(
                                                        if (selectedRow) Modifier.background(colors.control) else Modifier
                                                    )
                                                    .mooFocusClickable {
                                                        selected = change.path
                                                        loadFileDiffs(path = change.path)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    change.status,
                                                    color = colors.accent,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    change.path,
                                                    color = if (change.conflict) colors.danger else colors.textBody,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (change.conflict) {
                                                    Text(container.t("git.conflict"), color = colors.danger, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                val selectedChange = status.changes.find { it.path == selected }
                                if (selectedChange != null && status.repository) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MooButton(
                                            container.t("git.discard"),
                                            danger = true,
                                            p5Toolbar = true,
                                            enabled = !busy,
                                            onClick = { confirmDiscardPath = selectedChange.path }
                                        )
                                        if (selectedChange.conflict) {
                                            MooButton(container.t("git.ours"), p5Toolbar = true, enabled = !busy, onClick = {
                                                runAction(
                                                    workingTree = GitVaultFlushAction.ResolveConflict,
                                                    afterSuccess = { clearDiffSelection() }
                                                ) { GitEngine.resolveConflict(root, selectedChange.path, "ours") }
                                            })
                                            MooButton(container.t("git.theirs"), p5Toolbar = true, enabled = !busy, onClick = {
                                                runAction(
                                                    workingTree = GitVaultFlushAction.ResolveConflict,
                                                    afterSuccess = { clearDiffSelection() }
                                                ) { GitEngine.resolveConflict(root, selectedChange.path, "theirs") }
                                            })
                                        }
                                    }
                                }
                                if (status.repository) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            container.t("git.commitMessage"),
                                            color = colors.textMuted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(64.dp)
                                        )
                                        MooTextField(
                                            message,
                                            { message = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                        MooButton(
                                            container.t("git.commit"),
                                            prominent = true,
                                            p5Toolbar = true,
                                            enabled = !busy && status.changes.isNotEmpty() && !status.merging && status.conflicts == 0 && message.trim().isNotEmpty(),
                                            onClick = {
                                                runAction(
                                                    workingTree = GitVaultFlushAction.Commit,
                                                    afterSuccess = { onVaultRefresh() },
                                                ) { GitEngine.commit(root, message, identity()) }
                                            }
                                        )
                                    }
                                }
                            } else {
                                if (history.isEmpty()) {
                                    Text(container.t("git.emptyHistory"), color = colors.textMuted, fontSize = 11.sp)
                                } else {
                                    LazyColumn(Modifier.weight(1f)) {
                                        items(history, key = { it.hash }) { commit ->
                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .then(
                                                        if (commit.hash == selected) Modifier.background(colors.control) else Modifier
                                                    )
                                                    .mooFocusClickable {
                                                        selected = commit.hash
                                                        loadFileDiffs(commit = commit.hash)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(commit.shortHash, color = colors.accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                    Text(
                                                        commit.message,
                                                        color = colors.textBody,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Text("${commit.author} · ${commit.date}", color = colors.textMuted, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    GitFileDiffPane(
                        container = container,
                        files = fileDiffs,
                        selectedPath = selectedDiffPath,
                        onSelectPath = { selectedDiffPath = it },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                MooButton(container.t("common.close"), p5Toolbar = true, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun GitPanelTabs(
    changesLabel: String,
    historyLabel: String,
    tab: String,
    onTab: (String) -> Unit,
) {
    val colors = MooTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.toolbar)
            .border(1.dp, colors.borderSoft)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("changes" to changesLabel, "history" to historyLabel).forEach { (id, label) ->
            val active = tab == id
            Box(
                Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .then(if (active) Modifier.background(colors.control) else Modifier)
                    .mooFocusClickable { onTab(id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) colors.textStrong else colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun GitFileDiffPane(
    container: AppContainer,
    files: List<GitFileDiff>,
    selectedPath: String,
    onSelectPath: (String) -> Unit,
    modifier: Modifier
) {
    val colors = MooTheme.colors
    val (leftScroll, rightScroll) = rememberPairedScrollStates()
    val fileDiff = GitDiffSelection.selected(files, selectedPath)
    var fileMenuOpen by remember { mutableStateOf(false) }
    Column(
        modifier.background(colors.surfaceSubtle, RoundedCornerShape(8.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            container.t("git.diff"),
            color = colors.textStrong,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (fileDiff == null) {
            Text(container.t("git.diffEmpty"), color = colors.textSecondary, fontSize = 12.sp)
            return@Column
        }
        if (files.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(container.t("git.diffFile"), color = colors.textSecondary, fontSize = 11.sp)
                Box {
                    MooButton(GitDiffSelection.fileLabel(fileDiff), onClick = { fileMenuOpen = true })
                    MooMenu(expanded = fileMenuOpen, onDismissRequest = { fileMenuOpen = false }) {
                        files.forEach { item ->
                            MooMenuItem(onClick = {
                                fileMenuOpen = false
                                onSelectPath(item.path)
                            }) {
                                Text(GitDiffSelection.fileLabel(item))
                            }
                        }
                    }
                }
            }
        } else {
            Text(GitDiffSelection.fileLabel(fileDiff), color = colors.textSecondary, fontSize = 11.sp)
        }
        when (fileDiff.preview) {
            GitDiffPreview.Binary -> Text(container.t("git.diffBinary"), color = colors.warning, fontSize = 12.sp)
            GitDiffPreview.TooLarge -> Text(container.t("git.diffTooLarge"), color = colors.warning, fontSize = 12.sp)
            GitDiffPreview.Text -> {
                val comparison = remember(fileDiff.before, fileDiff.after) {
                    DiffEngine.compare(fileDiff.before, fileDiff.after, ignoreWhitespace = false)
                }
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Text(container.t("git.diffBefore"), color = colors.textSecondary, fontSize = 11.sp)
                        Text(
                            annotateSide(fileDiff.before, comparison.segments, "left", "both", colors.success, colors.danger, colors.accent),
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(leftScroll)
                        )
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Text(container.t("git.diffAfter"), color = colors.textSecondary, fontSize = 11.sp)
                        Text(
                            annotateSide(fileDiff.after, comparison.segments, "right", "both", colors.success, colors.danger, colors.accent),
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rightScroll)
                        )
                    }
                }
            }
        }
    }
}
