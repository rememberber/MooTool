package com.rememberber.mootool.next.compose.domain

/** JSON 检查器侧栏按钮/分区显隐（对齐 Electron `JsonInspector`）。 */
object JsonInspectorPresentation {
    fun inferSchemaEnabled(analysis: JsonAnalysis?): Boolean = analysis != null

    fun showDuplicatePaths(duplicateCount: Int): Boolean = duplicateCount > 0

    fun showDuplicatePathList(duplicateCount: Int): Boolean = showDuplicatePaths(duplicateCount)

    fun structurePanelVisible(analysis: JsonAnalysis?): Boolean = analysis != null

    fun duplicatePathClickEnabled(path: String): Boolean = path.isNotBlank()

    fun pathCopyEnabled(selectedPath: String): Boolean = selectedPath.isNotBlank()
}
