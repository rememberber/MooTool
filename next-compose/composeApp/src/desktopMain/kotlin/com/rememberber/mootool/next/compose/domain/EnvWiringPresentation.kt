package com.rememberber.mootool.next.compose.domain

import java.io.File
import java.nio.charset.StandardCharsets

/** F08 环境变量工具栏/对话框启用守卫与引擎路径（可单测）。 */
object EnvWiringPresentation {
    fun refreshEnabled(loading: Boolean, saving: Boolean): Boolean = !loading && !saving

    fun exportEnabled(hasSnapshot: Boolean): Boolean = hasSnapshot

    fun addVariableEnabled(canEdit: Boolean, saving: Boolean): Boolean = canEdit && !saving

    fun saveEditorEnabled(trimmedKey: String, saving: Boolean): Boolean =
        trimmedKey.isNotEmpty() && !saving

    fun deleteRowEnabled(canDelete: Boolean, saving: Boolean): Boolean = canDelete && !saving

    fun confirmDeleteEnabled(saving: Boolean): Boolean = !saving

    sealed interface SnapshotOutcome {
        data class Success(val snapshot: EnvSnapshot) : SnapshotOutcome
        data class Failure(val error: Throwable) : SnapshotOutcome
    }

    fun runSnapshot(config: EnvStoreConfig): SnapshotOutcome =
        runCatching { EnvEngine.snapshot(config) }.fold(
            onSuccess = { SnapshotOutcome.Success(it) },
            onFailure = { SnapshotOutcome.Failure(it) },
        )

    fun runPreviewDiff(
        snapshot: EnvSnapshot,
        scope: EnvPersistScope,
        key: String,
        value: String?,
    ): String = EnvEngine.previewDiff(snapshot, scope, key, value)

    sealed interface ExportOutcome {
        data object Success : ExportOutcome
        data class Failure(val error: Throwable) : ExportOutcome
    }

    fun runWriteExport(file: File, snapshot: EnvSnapshot): ExportOutcome =
        runCatching { file.writeText(EnvEngine.formatExport(snapshot), StandardCharsets.UTF_8) }.fold(
            onSuccess = { ExportOutcome.Success },
            onFailure = { ExportOutcome.Failure(it) },
        )

    fun shouldToastOperationFailure(error: Throwable): Boolean = true

    fun shouldToastIoFailure(error: Throwable): Boolean = true
}
