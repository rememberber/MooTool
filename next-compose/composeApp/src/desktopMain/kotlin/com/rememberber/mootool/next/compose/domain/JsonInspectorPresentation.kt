package com.rememberber.mootool.next.compose.domain

/** JSON 检查器侧栏按钮/分区显隐（对齐 Electron `JsonInspector`）。 */
object JsonInspectorPresentation {
    fun inferSchemaEnabled(analysis: JsonAnalysis?): Boolean = analysis != null

    fun showDuplicatePaths(duplicateCount: Int): Boolean = duplicateCount > 0

    fun showDuplicatePathList(duplicateCount: Int): Boolean = showDuplicatePaths(duplicateCount)

    fun structurePanelVisible(analysis: JsonAnalysis?): Boolean = analysis != null

    fun duplicatePathClickEnabled(path: String): Boolean = path.isNotBlank()

    fun pathCopyEnabled(selectedPath: String): Boolean = selectedPath.isNotBlank()

    sealed interface CopyJsonPathOutcome {
        data object Empty : CopyJsonPathOutcome
        data object Success : CopyJsonPathOutcome
        data object Failure : CopyJsonPathOutcome
    }

    fun runCopyJsonPath(path: String, copyText: (String) -> Boolean): CopyJsonPathOutcome =
        runCopyResultText(path, copyText)

    fun resultCopyEnabled(displayText: String): Boolean = displayText.isNotBlank()

    fun runCopyResultText(text: String, copyText: (String) -> Boolean): CopyJsonPathOutcome {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return CopyJsonPathOutcome.Empty
        return if (copyText(trimmed)) CopyJsonPathOutcome.Success else CopyJsonPathOutcome.Failure
    }

    fun shouldToastPathCopySuccess(): Boolean = true

    fun shouldToastPathCopyFailure(): Boolean = true

    fun shouldToastResultCopySuccess(): Boolean = true

    fun shouldToastResultCopyFailure(): Boolean = true

    /** 对齐 Electron `showError`：JSONPath 查询失败写 notice 并 error toast。 */
    fun shouldToastPathQueryFailure(): Boolean = true
}
