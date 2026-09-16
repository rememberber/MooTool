package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.VaultGitCheckpointMessages
import com.rememberber.mootool.next.compose.domain.VaultChangeKind
import com.rememberber.mootool.next.compose.domain.VaultConflictEngine
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.domain.noteOwnWriteJsonVaultFile
import com.rememberber.mootool.next.compose.domain.rebaselineAfterLocalCrud
import com.rememberber.mootool.next.compose.features.vault.vaultExportText
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.VaultEntry
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

/** Vault 树打开文件：先 flush；失败回滚 `vaultSelectedPath` 到仍打开的 `currentFile`（对齐 DIFF-378 / Electron `openFile` 失败不切换）。 */
internal fun openJsonVaultTreeFile(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    relativePath: String,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    if (relativePath == session.currentFile) {
        if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) {
            session.vaultSelectedPath = session.currentFile.ifBlank { session.vaultSelectedPath }
            return false
        }
        loadJsonVaultSnippet(session, relativePath, container.jsonVault.read(relativePath))
        return true
    }
    val previousSelected = session.currentFile.ifBlank { session.vaultSelectedPath }
    if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) {
        session.vaultSelectedPath = previousSelected
        return false
    }
    loadJsonVaultSnippet(session, relativePath, container.jsonVault.read(relativePath))
    return true
}

internal enum class JsonVaultReloadOpenResult {
    Unchanged,
    Reloaded,
    ClearedMissing,
}

/** 编辑器干净时从磁盘重载 `currentFile`（Electron `reloadSelectedFromDisk` / Vault 刷新）。 */
internal fun jsonVaultReloadOpenFileIfClean(
    container: AppContainer,
    session: JsonSession,
): JsonVaultReloadOpenResult {
    val path = session.currentFile
    if (path.isBlank()) return JsonVaultReloadOpenResult.Unchanged
    if (session.editor.text != session.savedText) return JsonVaultReloadOpenResult.Unchanged
    val disk = container.jsonVault.readOrNull(path)
    if (disk == null) {
        clearJsonVaultOpenSession(session)
        return JsonVaultReloadOpenResult.ClearedMissing
    }
    if (disk == session.savedText) return JsonVaultReloadOpenResult.Unchanged
    loadJsonVaultSnippet(session, path, disk)
    return JsonVaultReloadOpenResult.Reloaded
}

/** 磁盘监听 / Git 刷新：干净时总重载 `currentFile`（对齐 Electron `onJsonVaultChange`）；脏时仅当路径在 `paths` 内走冲突决策。 */
internal fun jsonVaultApplyExternalChange(
    container: AppContainer,
    session: JsonSession,
    paths: List<String>,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    val current = session.currentFile
    if (current.isBlank()) return false
    if (session.editor.text == session.savedText) {
        return when (jsonVaultReloadOpenFileIfClean(container, session)) {
            JsonVaultReloadOpenResult.Reloaded -> {
                session.notice = container.t("vault.conflict.reloaded")
                true
            }
            JsonVaultReloadOpenResult.ClearedMissing -> {
                session.notice = container.t("vault.conflict.deleted")
                true
            }
            JsonVaultReloadOpenResult.Unchanged -> false
        }
    }
    if (current !in paths) return false
    val disk = container.jsonVault.readOrNull(current)
    return when (VaultConflictEngine.decide(current, current, session.editor.text, session.savedText, disk)) {
        VaultChangeKind.Reload -> {
            loadJsonVaultSnippet(session, current, disk.orEmpty())
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
            onConflict(VaultConflictState(current, session.editor.text, disk, disk == null))
            false
        }
        VaultChangeKind.Ignored, VaultChangeKind.TreeChanged -> false
    }
}

internal fun clearJsonVaultOpenSession(session: JsonSession) {
    val apply = {
        session.editor.setText("", recordUndo = false)
        session.currentFile = ""
        session.vaultSelectedPath = ""
        session.savedText = ""
        session.clearJsonPathQueryResult()
    }
    if (SwingUtilities.isEventDispatchThread()) apply() else SwingUtilities.invokeLater(apply)
}

internal fun loadJsonVaultSnippet(session: JsonSession, path: String, content: String) {
    val apply = {
        session.editor.setText(content, recordUndo = false)
        session.currentFile = path
        session.vaultSelectedPath = path
        session.savedText = content
        session.clearJsonPathQueryResult()
        session.notice = ""
    }
    if (SwingUtilities.isEventDispatchThread()) apply() else SwingUtilities.invokeLater(apply)
}

internal fun flushJsonVaultEditorIfDirty(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): Result<Unit> {
    if (session.currentFile.isBlank() || session.editor.text == session.savedText) return Result.success(Unit)
    return saveJsonVault(container, session, monitor, showToast = false, onConflict = onConflict)
}

/** flush 失败时写入 `session.notice` 并返回 `false`，供历史/导入/Vault 树等路径共用。 */
internal fun jsonVaultFlushDirtyOrNotice(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): Boolean =
    flushJsonVaultEditorIfDirty(container, session, monitor, onConflict)
        .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
        .isSuccess

/** 供 `runCatching` 内使用：flush 失败时 `notice` 已写入并抛出，由外层 `onFailure` 展示。 */
internal fun jsonVaultRequireFlushDirty(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
) {
    if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) {
        throw IllegalStateException(session.notice.ifBlank { container.t("json.notice.failed") })
    }
}

internal fun duplicateJsonVaultSnippet(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    sourcePath: String,
    onConflict: (VaultConflictState) -> Unit,
): Result<String> {
    if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) {
        return Result.failure(
            IllegalStateException(session.notice.ifBlank { container.t("json.notice.failed") }),
        )
    }
    return runCatching {
        val copy = container.jsonVault.duplicate(sourcePath)
        val text = container.jsonVault.read(copy)
        monitor.rebaselineAfterLocalCrud()
        loadJsonVaultSnippet(session, copy, text)
        container.recordVaultActivity(VaultGitCheckpointMessages.DUPLICATE_JSON_SNIPPET, json = true)
        copy
    }
}

internal fun exportJsonVaultEntryToPath(
    container: AppContainer,
    session: JsonSession,
    relativePath: String,
    destination: Path,
) {
    val disk = runCatching { container.jsonVault.read(relativePath) }.getOrElse { throw it }
    val body = vaultExportText(
        exportPath = relativePath,
        openPath = session.currentFile,
        editorText = session.editor.text,
        savedText = session.savedText,
        diskText = disk,
    )
    Files.writeString(destination, body, StandardCharsets.UTF_8)
}

internal fun prepareJsonVaultContext(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    entry: VaultEntry,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    if (entry.directory) {
        if (session.currentFile.isBlank() || session.editor.text == session.savedText) {
            session.vaultSelectedPath = entry.relativePath
            return true
        }
        val previousSelected = session.currentFile.ifBlank { session.vaultSelectedPath }
        val result = saveJsonVault(container, session, monitor, showToast = false, onConflict = onConflict)
        if (result.isSuccess) {
            session.vaultSelectedPath = entry.relativePath
        } else {
            session.notice = result.exceptionOrNull()?.message ?: container.t("json.notice.failed")
            session.vaultSelectedPath = previousSelected
        }
        return result.isSuccess
    }
    if (entry.relativePath == session.currentFile) {
        session.vaultSelectedPath = entry.relativePath
        return true
    }
    if (session.currentFile.isBlank() || session.editor.text == session.savedText) {
        session.vaultSelectedPath = entry.relativePath
        return true
    }
    val previousSelected = session.currentFile.ifBlank { session.vaultSelectedPath }
    val result = saveJsonVault(container, session, monitor, showToast = false, onConflict = onConflict)
    if (result.isSuccess) {
        session.vaultSelectedPath = entry.relativePath
    } else {
        session.notice = result.exceptionOrNull()?.message ?: container.t("json.notice.failed")
        session.vaultSelectedPath = previousSelected
    }
    return result.isSuccess
}

/** Vault Git `onFlush`：未命名/未脏时跳过写盘，对齐 Electron `prepareGitAction` 与 DIFF-332。 */
internal fun jsonGitFlushBeforeAction(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): String? {
    if (session.currentFile.isBlank()) {
        if (session.editor.text.isNotBlank() && session.editor.text != JsonSession.SAMPLE_JSON) {
            return container.t("git.flush.untitled")
        }
        return null
    }
    if (!session.isVaultEditorDirty()) return null
    return saveJsonVault(container, session, monitor, showToast = false, onConflict = onConflict)
        .fold(
            onSuccess = { null },
            onFailure = { it.message ?: container.t("json.notice.failed") },
        )
}

/**
 * 工具栏/Ctrl+S 保存：无 Vault 选中时打开「新建片段」对话框（对齐 Electron `saveSelected` → `beginCreateFile`）。
 * @return `true` 表示已打开新建对话框；`false` 表示已尝试写盘（成功或失败，`notice` 已更新）。
 */
internal fun jsonVaultSaveFromUserAction(
    container: AppContainer,
    session: JsonSession,
    vaultItems: List<VaultEntry>,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): Boolean {
    if (session.currentFile.isBlank()) {
        openJsonVaultNewFileDialog(session, vaultItems)
        return true
    }
    saveJsonVault(container, session, monitor, onConflict = onConflict)
        .onSuccess { session.notice = container.t("common.save") }
        .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
    return false
}

/** 250ms 空闲自动保存；失败写入 `session.notice`（对齐随手记 idle `error` 与 DIFF-363）。 */
internal fun jsonVaultIdleAutosaveAttempt(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
): Boolean =
    saveJsonVault(container, session, monitor, showToast = false, onConflict = onConflict)
        .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
        .isSuccess

internal fun saveJsonVault(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    showToast: Boolean = true,
    onConflict: (VaultConflictState) -> Unit,
): Result<Unit> {
    val name = session.currentFile
    if (name.isBlank()) {
        return Result.failure(IllegalStateException(container.t("json.vault.saveNoSelection")))
    }
    val disk = runCatching { container.jsonVault.readOrNull(name) }.getOrNull()
    if (!VaultConflictEngine.canOverwrite(session.savedText, disk, session.editor.text)) {
        onConflict(VaultConflictState(name, session.editor.text, disk, disk == null))
        return Result.failure(IllegalStateException(container.t("vault.conflict.blocked")))
    }
    return runCatching {
        container.jsonVault.write(name, session.editor.text)
        monitor.noteOwnWriteJsonVaultFile(container.jsonVault, name)
        session.currentFile = name
        session.savedText = session.editor.text
        container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_JSON_SNIPPET, json = true)
        if (showToast) container.toastSuccess(container.t("json.vault.saved"))
    }
}
