package com.rememberber.mootool.next.compose.domain

/** JSON 检查器侧栏按钮/分区显隐（对齐 Electron `JsonInspector`）。 */
object JsonInspectorPresentation {
    fun inferSchemaEnabled(analysis: JsonAnalysis?): Boolean = analysis != null

    /** 对齐 `JsonScreen`「生成 JSON Schema」：同 [inferSchemaEnabled]。 */
    fun inferSchemaActionEnabled(analysis: JsonAnalysis?): Boolean = inferSchemaEnabled(analysis)

    /** 对齐检查器「应用自定义格式」：仅 JSON 结构解析成功时可用（同 [inferSchemaEnabled]）。 */
    fun formatAdvancedActionEnabled(analysis: JsonAnalysis?): Boolean = inferSchemaEnabled(analysis)

    /** 对齐检查器「选择 JSONPath」：无路径树条目时禁用。 */
    fun pathPickerOpenActionEnabled(pathEntryCount: Int): Boolean = pathEntryCount > 0

    /** 检查器转换区依赖可解析 JSON 结构的动作（JSON→XML/Bean、KV 互换等）。 */
    fun jsonStructureConvertActionEnabled(analysis: JsonAnalysis?): Boolean = inferSchemaEnabled(analysis)

    /** 检查器转换区纯文本动作（转义/反转义等）：编辑器非空即可。 */
    fun editorTextConvertActionEnabled(editorText: String): Boolean = editorText.isNotBlank()

    /** XML/Bean 对话框入口不依赖当前编辑器 JSON 有效性。 */
    fun conversionDialogActionEnabled(): Boolean = true

    fun showDuplicatePaths(duplicateCount: Int): Boolean = duplicateCount > 0

    fun showDuplicatePathList(duplicateCount: Int): Boolean = showDuplicatePaths(duplicateCount)

    fun structurePanelVisible(analysis: JsonAnalysis?): Boolean = analysis != null

    fun duplicatePathClickEnabled(path: String): Boolean = path.isNotBlank()

    /** 对齐结构面板重复键路径行：同 [duplicatePathClickEnabled]。 */
    fun duplicatePathClickActionEnabled(path: String): Boolean = duplicatePathClickEnabled(path)

    fun pathCopyEnabled(selectedPath: String): Boolean = selectedPath.isNotBlank()

    /** 对齐 `JsonScreen` JSONPath 复制钮：同 [pathCopyEnabled]。 */
    fun pathCopyActionEnabled(selectedPath: String): Boolean = pathCopyEnabled(selectedPath)

    /** 对齐 `JsonScreen` JSONPath 查询钮：空路径时禁用（`queryPath` 仍由 Enter/守卫 toast）。 */
    fun pathQueryActionEnabled(selectedPath: String): Boolean = selectedPath.trim().isNotEmpty()

    sealed interface CopyJsonPathOutcome {
        data object Empty : CopyJsonPathOutcome
        data object Success : CopyJsonPathOutcome
        data object Failure : CopyJsonPathOutcome
    }

    fun runCopyJsonPath(path: String, copyText: (String) -> Boolean): CopyJsonPathOutcome =
        runCopyResultText(path, copyText)

    fun resultCopyEnabled(displayText: String): Boolean = displayText.isNotBlank()

    /** 对齐检查器结果区「复制」：同 [resultCopyEnabled]。 */
    fun resultCopyActionEnabled(displayText: String): Boolean = resultCopyEnabled(displayText)

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
