package com.rememberber.mootool.next.compose.domain

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
}
