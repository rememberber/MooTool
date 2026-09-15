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
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DiffEngine
import com.rememberber.mootool.next.compose.domain.GitCommitInfo
import com.rememberber.mootool.next.compose.domain.GitDiffPreview
import com.rememberber.mootool.next.compose.domain.GitDiffSelection
import com.rememberber.mootool.next.compose.domain.GitEngine
import com.rememberber.mootool.next.compose.domain.GitFileDiff
import com.rememberber.mootool.next.compose.domain.GitStatus
import com.rememberber.mootool.next.compose.features.diff.annotateSide
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
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
    onFlush: () -> String?
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
    var notice by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(true) }

    fun identity() = GitEngine.identityFrom(settings.vault.gitUsername)
    fun token() = settings.vault.gitToken.trim()

    fun load() {
        scope.launch {
            busy = true
            error = ""
            val flushed = onFlush()
            if (flushed != null) {
                error = flushed
                busy = false
                return@launch
            }
            val nextStatus = withContext(Dispatchers.IO) { GitEngine.status(root) }
            val nextHistory = withContext(Dispatchers.IO) {
                if (nextStatus.repository) GitEngine.history(root) else emptyList()
            }
            status = nextStatus
            history = nextHistory
            if (nextStatus.remote.isNotBlank()) remote = nextStatus.remote
            else if (remote.isBlank()) remote = settings.vault.gitRemote
            busy = false
        }
    }

    fun runAction(afterSuccess: (() -> Unit)? = null, block: () -> com.rememberber.mootool.next.compose.domain.GitActionResult) {
        scope.launch {
            busy = true
            error = ""
            notice = ""
            val flushed = onFlush()
            if (flushed != null) {
                error = flushed
                busy = false
                return@launch
            }
            val result = withContext(Dispatchers.IO) { block() }
            if (result.success) {
                notice = result.message.ifBlank { container.t("git.done") }
                afterSuccess?.invoke()
            } else {
                error = result.message
            }
            val nextStatus = withContext(Dispatchers.IO) { GitEngine.status(root) }
            val nextHistory = withContext(Dispatchers.IO) {
                if (nextStatus.repository) GitEngine.history(root) else emptyList()
            }
            status = nextStatus
            history = nextHistory
            if (nextStatus.remote.isNotBlank()) remote = nextStatus.remote
            busy = false
        }
    }

    fun loadFileDiffs(path: String? = null, commit: String? = null) {
        scope.launch {
            busy = true
            val next = withContext(Dispatchers.IO) {
                GitEngine.fileDiffs(root, path = path, commit = commit)
            }
            fileDiffs = next
            selectedDiffPath = GitDiffSelection.selected(next, "")?.path.orEmpty()
            busy = false
        }
    }

    LaunchedEffect(root) { load() }

    MooOverlay(onDismiss = onDismiss) {
        Column(
            Modifier.width(1040.dp).height(620.dp)
                .background(colors.workspace, RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = colors.textPrimary, fontSize = 16.sp)
            Text(root.toAbsolutePath().normalize().toString(), color = colors.textSecondary, fontSize = 11.sp)
            val summary = when {
                !status.available -> container.t("git.unavailable")
                !status.repository -> container.t("git.noRepo")
                else -> container.t("git.branch", mapOf("branch" to status.branch.ifBlank { "HEAD" }))
            }
            Text(summary, color = if (!status.available) colors.danger else colors.textPrimary, fontSize = 13.sp)
            if (status.repository) {
                Text(
                    container.t("git.counts", mapOf("changes" to status.changes.size.toString(), "conflicts" to status.conflicts.toString())),
                    color = if (status.conflicts > 0 || status.merging) colors.warning else colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            if (status.version.isNotBlank()) {
                Text(status.version, color = colors.textSecondary, fontSize = 11.sp)
            }
            Text(container.t("git.authHint"), color = colors.textSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MooButton(container.t("git.refresh"), enabled = !busy, onClick = { load() })
                if (!status.repository) {
                    MooButton(container.t("git.init"), primary = true, enabled = !busy && status.available, onClick = {
                        runAction { GitEngine.init(root, identity()) }
                    })
                }
                if (status.repository) {
                    MooButton(
                        container.t("git.fetch"),
                        enabled = !busy && status.remote.isNotBlank(),
                        onClick = { runAction { GitEngine.fetch(root, token = token()) } }
                    )
                    MooButton(
                        container.t("git.pull"),
                        enabled = !busy && status.remote.isNotBlank() && !status.merging,
                        onClick = { runAction { GitEngine.pull(root, token = token()) } }
                    )
                    MooButton(
                        container.t("git.push"),
                        enabled = !busy && status.remote.isNotBlank() && !status.merging,
                        onClick = { runAction { GitEngine.push(root, token = token()) } }
                    )
                }
                if (status.merging) {
                    MooButton(container.t("git.abort"), enabled = !busy, onClick = { runAction { GitEngine.abortMerge(root) } })
                    MooButton(
                        container.t("git.continue"),
                        enabled = !busy && status.conflicts == 0,
                        onClick = { runAction { GitEngine.continueOperation(root, identity()) } }
                    )
                }
                MooButton(container.t("common.close"), onClick = onDismiss)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MooTextField(remote, { remote = it }, placeholder = container.t("git.remotePlaceholder"), modifier = Modifier.weight(1f))
                MooButton(container.t("git.saveRemote"), enabled = !busy && status.repository, onClick = {
                    val nextRemote = remote.trim()
                    runAction(
                        afterSuccess = {
                            container.updateSettings { current -> current.copy(vault = current.vault.copy(gitRemote = nextRemote)) }
                        }
                    ) { GitEngine.setRemote(root, nextRemote) }
                })
            }
            if (status.available) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.width(280.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MooButton(
                                container.t("git.changes") + " ${status.changes.size}",
                                primary = tab == "changes",
                                onClick = { tab = "changes"; selected = ""; fileDiffs = emptyList(); selectedDiffPath = "" }
                            )
                            MooButton(
                                container.t("git.history"),
                                primary = tab == "history",
                                onClick = { tab = "history"; selected = ""; fileDiffs = emptyList(); selectedDiffPath = "" }
                            )
                        }
                        if (tab == "changes") {
                            if (status.changes.isEmpty()) {
                                Text(container.t("git.emptyChanges"), color = colors.textSecondary, fontSize = 12.sp)
                            } else {
                                LazyColumn(Modifier.weight(1f)) {
                                    items(status.changes, key = { "${it.status}-${it.path}" }) { change ->
                                        Text(
                                            "${change.status} ${change.path}" + if (change.conflict) " · ${container.t("git.conflict")}" else "",
                                            color = if (change.path == selected) colors.accent else if (change.conflict) colors.danger else colors.textPrimary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                                                selected = change.path
                                                loadFileDiffs(path = change.path)
                                            }.padding(6.dp)
                                        )
                                    }
                                }
                            }
                            if (status.repository) {
                                MooTextField(message, { message = it }, placeholder = container.t("git.commitMessage"), modifier = Modifier.fillMaxWidth())
                                MooButton(
                                    container.t("git.commit"),
                                    primary = true,
                                    enabled = !busy && status.changes.isNotEmpty() && !status.merging && status.conflicts == 0 && message.trim().isNotEmpty(),
                                    onClick = { runAction { GitEngine.commit(root, message, identity()) } }
                                )
                                val selectedChange = status.changes.find { it.path == selected }
                                if (selectedChange != null) {
                                    MooButton(
                                        container.t("git.discard") + " " + selectedChange.path,
                                        enabled = !busy,
                                        onClick = { runAction { GitEngine.discard(root, selectedChange.path) } }
                                    )
                                }
                                if (selectedChange?.conflict == true) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MooButton(container.t("git.ours"), enabled = !busy, onClick = {
                                            runAction { GitEngine.resolveConflict(root, selectedChange.path, "ours") }
                                        })
                                        MooButton(container.t("git.theirs"), enabled = !busy, onClick = {
                                            runAction { GitEngine.resolveConflict(root, selectedChange.path, "theirs") }
                                        })
                                    }
                                }
                            }
                        } else {
                            if (history.isEmpty()) {
                                Text(container.t("git.emptyHistory"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            } else {
                                LazyColumn(Modifier.weight(1f)) {
                                    items(history, key = { it.hash }) { commit ->
                                        Column(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                                                selected = commit.hash
                                                loadFileDiffs(commit = commit.hash)
                                            }.padding(6.dp)
                                        ) {
                                            Text(
                                                "${commit.shortHash}  ${commit.message}",
                                                color = if (commit.hash == selected) colors.accent else colors.textPrimary,
                                                fontSize = 12.sp
                                            )
                                            Text("${commit.author} · ${commit.date}", color = colors.textSecondary, fontSize = 11.sp)
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
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (error.isNotBlank()) Text(error, color = colors.danger, fontSize = 12.sp)
            else if (notice.isNotBlank()) Text(notice, color = colors.success, fontSize = 12.sp)
            else if (busy) Text(container.t("git.busy"), color = colors.textSecondary, fontSize = 12.sp)
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
        modifier.background(colors.surfaceSubtle, RoundedCornerShape(8.dp)).padding(8.dp)
    ) {
        if (fileDiff == null) {
            Text(container.t("git.diffEmpty"), color = colors.textSecondary, fontSize = 12.sp)
            return@Column
        }
        if (files.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(container.t("git.diffFile"), color = colors.textSecondary, fontSize = 11.sp)
                Box {
                    MooButton(GitDiffSelection.fileLabel(fileDiff), onClick = { fileMenuOpen = true })
                    DropdownMenu(expanded = fileMenuOpen, onDismissRequest = { fileMenuOpen = false }) {
                        files.forEach { item ->
                            DropdownMenuItem(onClick = {
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
