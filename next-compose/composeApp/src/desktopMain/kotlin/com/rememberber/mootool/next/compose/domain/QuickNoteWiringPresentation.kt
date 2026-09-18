package com.rememberber.mootool.next.compose.domain

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** F01 随手记：工具栏导出与 UI 守卫（对齐 F04 `JsonWiringPresentation` 文件 IO）。 */
object QuickNoteWiringPresentation {
    fun exportEnabled(currentFile: String): Boolean = currentFile.isNotBlank()

    sealed interface WriteExportOutcome {
        data object Success : WriteExportOutcome
        data class Failure(val error: Throwable) : WriteExportOutcome
    }

    fun runWriteExportText(destination: Path, text: String): WriteExportOutcome =
        runCatching { Files.writeString(destination, text, StandardCharsets.UTF_8) }.fold(
            onSuccess = { WriteExportOutcome.Success },
            onFailure = { WriteExportOutcome.Failure(it) },
        )

    fun operationFailureMessage(fallbackKey: String, error: Throwable?, t: (String) -> String): String =
        error?.message?.takeIf { it.isNotBlank() } ?: t(fallbackKey)

    fun shouldToastOperationFailure(): Boolean = true

    fun shouldToastSaveFailure(): Boolean = true

    fun shouldToastIoFailure(error: Throwable): Boolean = true
}
