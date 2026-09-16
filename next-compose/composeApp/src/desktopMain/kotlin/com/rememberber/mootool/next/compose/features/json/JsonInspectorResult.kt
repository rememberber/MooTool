package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.JsonStatus
import com.rememberber.mootool.next.compose.sessions.JsonSession

data class JsonInspectorResultDisplay(
    val text: String,
    val isError: Boolean,
)

/**
 * 检查器结果区文案：JSONPath 查询/路径预览优先展示 [pathResult]；
 * Vault、格式化等操作的 [notice] 在非常规 JSONPath 标题时优先，避免 DIFF-225 后旧查询结果盖住新提示。
 */
/** 编辑器经 [com.rememberber.mootool.next.compose.editor.EditorBuffer.setText] 批量替换后调用，避免残留 JSONPath 结果。 */
internal fun JsonSession.clearJsonPathQueryResult() {
    pathResult = ""
}

/** 用户手改 JSONPath 输入时丢弃上次查询/路径预览，避免表达式已变仍展示旧 [pathResult]。 */
internal fun JsonSession.applyInspectorJsonPathInput(value: String, pathAppliedNotice: String) {
    jsonPath = value
    clearJsonPathQueryResult()
    if (notice == pathAppliedNotice) notice = ""
}

/** 路径选择弹层「使用」/ 双击：对齐 Electron `onChoosePath`，只写入路径与提示，不自动查询。 */
internal fun JsonSession.applyPathPickerChoice(path: String, pathAppliedNotice: String) {
    jsonPath = path
    clearJsonPathQueryResult()
    notice = pathAppliedNotice
    pathPickerOpen = false
}

/** 检查器内联路径树单击：写入路径并在结果区展示节点预览（不执行 JSONPath 查询）。 */
internal fun JsonSession.applyInlinePathTreePreview(path: String, preview: String, pathAppliedNotice: String) {
    jsonPath = path
    pathResult = preview
    notice = pathAppliedNotice
}

/**
 * 检查器内联路径树双击：先单击预览，再执行 JSONPath 查询并写入 [pathResult]（对齐 Electron 内联树双击查询；
 * 弹层双击仅选路径见 DIFF-319）。
 */
internal fun JsonSession.performInlinePathTreeDoubleTapQuery(
    input: String,
    path: String,
    previewFallback: String,
    pathAppliedNotice: String,
    queryPanelTitle: String,
    translator: com.rememberber.mootool.next.compose.domain.JsonTranslator,
) {
    val preview = jsonPathNodePreview(input, path, translator, previewFallback)
    applyInlinePathTreePreview(path, preview, pathAppliedNotice)
    runCatching { com.rememberber.mootool.next.compose.domain.JsonEngine.queryPath(input, path, translator) }
        .onSuccess { result ->
            pathResult = result
            notice = queryPanelTitle
        }
        .onFailure { error ->
            clearJsonPathQueryResult()
            notice = error.message ?: ""
        }
}

internal fun jsonPathPickerInitialSelection(jsonPath: String, entryPaths: List<String>): String =
    entryPaths.find { it == jsonPath } ?: entryPaths.firstOrNull() ?: jsonPath

/** 对齐 Electron `JsonPathPicker` `formatPreview`：字符串原文，对象/数组 pretty-print。 */
internal fun jsonPathNodePreview(
    input: String,
    path: String,
    translator: com.rememberber.mootool.next.compose.domain.JsonTranslator,
    fallback: String = "",
): String = runCatching { com.rememberber.mootool.next.compose.domain.JsonEngine.pathPickerPreview(input, path, translator) }
    .getOrElse { fallback }

internal fun jsonInspectorResultDisplay(
    pathResult: String,
    notice: String,
    status: JsonStatus,
    pathAppliedNotice: String,
    jsonPathPanelTitle: String,
): JsonInspectorResultDisplay {
    val pathPinned = pathResult.isNotBlank() &&
        (notice.isBlank() || notice == pathAppliedNotice || notice == jsonPathPanelTitle)
    val text = when {
        pathPinned -> pathResult
        notice.isNotBlank() -> notice
        pathResult.isNotBlank() -> pathResult
        else -> status.message
    }
    val isError = pathResult.isBlank() && status.kind == JsonStatus.Kind.Error
    return JsonInspectorResultDisplay(text, isError)
}
