package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DocumentFormatEngine
import com.rememberber.mootool.next.compose.domain.NoteAttachmentEngine
import com.rememberber.mootool.next.compose.domain.NoteFrontmatter
import com.rememberber.mootool.next.compose.domain.NoteMetadata
import com.rememberber.mootool.next.compose.domain.VaultGitCheckpointMessages
import com.rememberber.mootool.next.compose.domain.VaultChangeKind
import com.rememberber.mootool.next.compose.domain.VaultConflictEngine
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.domain.noteOwnWriteQuickNoteFile
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.VaultEntry
import javax.swing.SwingUtilities

internal fun prepareQuickNoteVaultContext(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    entry: VaultEntry,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    if (entry.directory) {
        val previousSelected = session.currentFile.ifBlank { session.vaultSelectedPath }
        if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor, onConflict)) {
            session.vaultSelectedPath = previousSelected
            return false
        }
        session.vaultSelectedPath = entry.relativePath
        quickNoteOnEdt { session.editor.setText("", recordUndo = false) }
        session.currentFile = ""
        session.savedText = ""
        session.metadata = NoteMetadata.defaults("Untitled")
        session.savedMetadata = session.metadata
        return true
    }
    if (entry.relativePath == session.currentFile) {
        session.vaultSelectedPath = entry.relativePath
        return true
    }
    val previousSelected = session.currentFile.ifBlank { session.vaultSelectedPath }
    if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor, onConflict)) {
        session.vaultSelectedPath = previousSelected
        return false
    }
    session.vaultSelectedPath = entry.relativePath
    quickNoteOpenVaultFile(session, vault, entry.relativePath)
    return true
}

internal fun quickNoteVaultSaveIfNeeded(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    val ok = quickNoteSaveIfNeeded(quickNoteDirty(session)) {
        quickNoteSaveCurrent(container, session, vault, monitor, onConflict = onConflict)
    }
    if (!ok) {
        session.error = session.error.ifBlank { container.t("quickNote.saveFailed") }
    }
    return ok
}

/**
 * 工具栏/Ctrl+S：无 `currentFile` 时打开「新建笔记」对话框（对齐 Electron `saveCurrent` 需已有 `note`）。
 * @return `true` 表示已打开新建对话框。
 */
/** 250ms 空闲自动保存；失败时 `quickNoteSaveCurrent` 已写入 `session.error`（对齐 JSON `jsonVaultIdleAutosaveAttempt`）。 */
internal fun quickNoteIdleAutosaveAttempt(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): Boolean =
    quickNoteSaveCurrent(container, session, vault, monitor, showToast = false, onConflict = onConflict)

internal fun quickNoteSaveFromUserAction(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    if (session.currentFile.isBlank()) {
        openQuickNoteNewNoteDialog(session)
        return true
    }
    quickNoteSaveCurrent(container, session, vault, monitor, onConflict = onConflict)
    return false
}

internal fun quickNoteSaveCurrent(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    showToast: Boolean = true,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    val name = session.currentFile
    if (name.isBlank()) {
        session.error = session.error.ifBlank { container.t("quickNote.saveNoSelection") }
        return false
    }
    val disk = runCatching { vault.readOrNull(name) }.getOrNull()
    val diskBody = disk?.let { NoteFrontmatter.parse(it, name.substringAfterLast('/').substringBeforeLast('.')).content }
    if (!VaultConflictEngine.canOverwrite(session.savedText, diskBody, session.editor.text)) {
        onConflict(VaultConflictState(name, session.editor.text, diskBody, disk == null))
        session.error = container.t("vault.conflict.blocked")
        return false
    }
    val previous = runCatching { vault.readNote(session.currentFile).content }.getOrNull()
    val metadata = session.metadata.copy(
        title = session.metadata.title.ifBlank { name.substringAfterLast('/').substringBeforeLast('.') },
        syntax = session.metadata.syntax.ifBlank { NoteFrontmatter.syntaxForExtension(name.substringAfterLast('.')) },
        lineWrap = session.wrap,
        fontName = session.metadata.fontName.ifBlank { container.settings.value.editor.quickNoteFontName },
        fontSize = session.metadata.fontSize.takeIf { it > 0 } ?: container.settings.value.editor.quickNoteFontSize
    )
    return runCatching { vault.saveNote(name, session.editor.text, metadata) }
        .onSuccess { document ->
            monitor.noteOwnWriteQuickNoteFile(vault, document.relativePath)
            session.currentFile = document.relativePath
            session.savedText = document.content
            session.metadata = document.metadata
            session.savedMetadata = document.metadata
            session.error = ""
            if (showToast) {
                session.notice = container.t("quickNote.saved")
                container.toastSuccess(container.t("quickNote.saved"))
                container.history.save(ToolId.QuickNote.id, document.relativePath, document.relativePath, document.content.take(8_000), "")
            }
            container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_QUICK_NOTE)
            previous?.let { old ->
                val removed = NoteAttachmentEngine.extractPaths(old) - NoteAttachmentEngine.extractPaths(document.content)
                removed.forEach { path -> runCatching { NoteAttachmentEngine.deleteIfUnreferenced(vault, path) } }
            }
        }
        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
        .isSuccess
}

internal enum class QuickNoteReloadOpenResult {
    Unchanged,
    Reloaded,
    ClearedMissing,
}

/** 编辑器干净时从磁盘重载当前笔记（Electron `reloadCurrentNoteFromDisk` / `loadTree` 后刷新）。 */
internal fun quickNoteReloadOpenFileIfClean(session: QuickNoteSession, vault: NoteVault): QuickNoteReloadOpenResult {
    val path = session.currentFile
    if (path.isBlank()) return QuickNoteReloadOpenResult.Unchanged
    if (quickNoteDirty(session)) return QuickNoteReloadOpenResult.Unchanged
    if (vault.readOrNull(path) == null) {
        clearQuickNoteOpenSession(session)
        return QuickNoteReloadOpenResult.ClearedMissing
    }
    val note = vault.readNote(path)
    if (note.content == session.savedText && note.metadata == session.savedMetadata) {
        return QuickNoteReloadOpenResult.Unchanged
    }
    quickNoteOpenVaultFile(session, vault, path)
    return QuickNoteReloadOpenResult.Reloaded
}

internal fun clearQuickNoteOpenSession(session: QuickNoteSession) {
    quickNoteOnEdt { session.editor.setText("", recordUndo = false) }
    session.currentFile = ""
    session.vaultSelectedPath = ""
    session.savedText = ""
    session.metadata = NoteMetadata.defaults("Untitled")
    session.savedMetadata = session.metadata
    session.error = ""
}

internal fun quickNoteApplyExternalChange(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    paths: List<String>,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    val current = session.currentFile
    if (current.isBlank()) return false
    if (!quickNoteDirty(session)) {
        return when (quickNoteReloadOpenFileIfClean(session, vault)) {
            QuickNoteReloadOpenResult.Reloaded -> {
                session.notice = container.t("vault.conflict.reloaded")
                true
            }
            QuickNoteReloadOpenResult.ClearedMissing -> {
                session.notice = container.t("vault.conflict.deleted")
                true
            }
            QuickNoteReloadOpenResult.Unchanged -> false
        }
    }
    if (current !in paths) return false
    val disk = vault.readOrNull(current)
    val diskBody = disk?.let { NoteFrontmatter.parse(it, current.substringAfterLast('/').substringBeforeLast('.')).content }
    return when (VaultConflictEngine.decide(current, current, session.editor.text, session.savedText, diskBody)) {
        VaultChangeKind.Reload -> {
            quickNoteOpenVaultFile(session, vault, current)
            session.notice = container.t("vault.conflict.reloaded")
            true
        }
        VaultChangeKind.Deleted -> {
            session.currentFile = ""
            session.savedText = session.editor.text
            session.notice = container.t("vault.conflict.deleted")
            true
        }
        VaultChangeKind.Conflict -> {
            onConflict(VaultConflictState(current, session.editor.text, diskBody, disk == null))
            false
        }
        VaultChangeKind.Ignored, VaultChangeKind.TreeChanged -> false
    }
}

internal fun quickNoteOpenVaultFile(session: QuickNoteSession, vault: NoteVault, relativePath: String) {
    val note = vault.readNote(relativePath)
    quickNoteOnEdt { session.editor.setText(note.content, recordUndo = false) }
    session.currentFile = note.relativePath
    session.vaultSelectedPath = note.relativePath
    session.savedText = note.content
    session.metadata = note.metadata
    session.savedMetadata = note.metadata
    session.wrap = note.metadata.lineWrap
    session.editor.syntax = DocumentFormatEngine.rstaSyntax(
        note.metadata.syntax.ifBlank { NoteFrontmatter.syntaxForExtension(relativePath.substringAfterLast('.')) }
    )
    session.error = ""
    session.notice = ""
}

internal fun quickNoteOnEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}

/**
 * Vault Git `onFlush`：无命名文档、未脏（正文或 metadata）时跳过写盘，对齐 Electron `prepareGitAction` 与 JSON DIFF-332。
 * @return 非 null 时 Git 操作应中止并展示该文案
 */
internal fun quickNoteGitFlushBeforeAction(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): String? {
    if (session.currentFile.isBlank()) {
        if (session.editor.text.isNotBlank() && session.editor.text != QuickNoteSession.SAMPLE) {
            return container.t("git.flush.untitled")
        }
        return null
    }
    if (!quickNoteDirty(session)) return null
    if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor, onConflict)) {
        return session.error.ifBlank { container.t("quickNote.saveFailed") }
    }
    return null
}
