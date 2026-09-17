package com.rememberber.mootool.next.compose.domain

import java.io.File
import java.nio.charset.StandardCharsets

/** F10 Host：应用/备份恢复 busy 守卫与引擎路径（可单测）。 */
object HostWiringPresentation {
    fun canConfirmApply(applying: Boolean): Boolean = !applying

    fun canOpenApplyConfirm(content: String, applying: Boolean): Boolean =
        content.isNotBlank() && !applying

    fun canRestoreBackup(lastBackup: String, applying: Boolean): Boolean =
        lastBackup.isNotBlank() && !applying

    fun canToggleContentSearch(applying: Boolean): Boolean = !applying

    fun showFilteredEmpty(profileCount: Int, query: String): Boolean =
        profileCount == 0 && JsonVaultSearchPresentation.isFiltering(query)

    sealed interface SystemReadOutcome {
        data class Success(val system: SystemHostsFile) : SystemReadOutcome
        data class Failure(val error: Throwable) : SystemReadOutcome
    }

    fun runReadSystem(config: HostApplyConfig): SystemReadOutcome =
        runCatching { HostEngine.readSystem(config) }.fold(
            onSuccess = { SystemReadOutcome.Success(it) },
            onFailure = { SystemReadOutcome.Failure(it) },
        )

    data class ApplyPreview(val system: SystemHostsFile, val diff: String)

    sealed interface ApplyPreviewOutcome {
        data class Success(val preview: ApplyPreview) : ApplyPreviewOutcome
        data class Failure(val error: Throwable) : ApplyPreviewOutcome
    }

    fun runBuildApplyPreview(config: HostApplyConfig, content: String): ApplyPreviewOutcome =
        runCatching {
            val current = HostEngine.readSystem(config)
            HostEngine.validate(content)
            val diff = HostEngine.previewDiff(current.content, content, current.path)
            ApplyPreview(current, diff)
        }.fold(
            onSuccess = { ApplyPreviewOutcome.Success(it) },
            onFailure = { ApplyPreviewOutcome.Failure(it) },
        )

    sealed interface ApplyOutcome {
        data class Success(val result: HostApplyResult) : ApplyOutcome
        data class Failure(val error: Throwable) : ApplyOutcome
    }

    fun runApply(
        config: HostApplyConfig,
        content: String,
        expectedFingerprint: String?,
    ): ApplyOutcome =
        runCatching { HostEngine.apply(config, content, expectedFingerprint) }.fold(
            onSuccess = { ApplyOutcome.Success(it) },
            onFailure = { ApplyOutcome.Failure(it) },
        )

    sealed interface RestoreOutcome {
        data class Success(val result: HostApplyResult) : RestoreOutcome
        data class Failure(val error: Throwable) : RestoreOutcome
    }

    fun runRestoreBackup(config: HostApplyConfig, backupPath: String): RestoreOutcome =
        runCatching {
            val current = HostEngine.readSystem(config)
            HostEngine.restore(config, backupPath, current.fingerprint)
        }.fold(
            onSuccess = { RestoreOutcome.Success(it) },
            onFailure = { RestoreOutcome.Failure(it) },
        )

    sealed interface ImportProfileOutcome {
        data class Success(val content: String) : ImportProfileOutcome
        data class Failure(val error: Throwable) : ImportProfileOutcome
    }

    fun inferImportProfileName(file: File, untitledKey: String): String =
        file.nameWithoutExtension.ifBlank { untitledKey }

    fun runReadImportProfile(file: File): ImportProfileOutcome =
        runCatching {
            ImportProfileOutcome.Success(file.readText(StandardCharsets.UTF_8))
        }.getOrElse { ImportProfileOutcome.Failure(it) }

    sealed interface ExportProfileOutcome {
        data object Success : ExportProfileOutcome
        data class Failure(val error: Throwable) : ExportProfileOutcome
    }

    fun runWriteExportProfile(file: File, content: String): ExportProfileOutcome =
        runCatching { file.writeText(content, StandardCharsets.UTF_8) }.fold(
            onSuccess = { ExportProfileOutcome.Success },
            onFailure = { ExportProfileOutcome.Failure(it) },
        )
}
