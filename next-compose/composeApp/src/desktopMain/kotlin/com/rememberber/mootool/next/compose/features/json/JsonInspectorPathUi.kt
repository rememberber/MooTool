package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.JsonInspectorPresentation
import com.rememberber.mootool.next.compose.domain.JsonPathEntry
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.sessions.JsonSession

/** 打开路径弹层时始终跟当前 [jsonPath] 对齐（补充 DIFF-228，修复旧 `pathPickerSelection` 残留）。 */
internal fun jsonPathPickerSelectionForOpen(jsonPath: String, entryPaths: List<String>): String =
    jsonPathPickerInitialSelection(jsonPath, entryPaths)

internal fun jsonPathQuickPickerMenuLabel(entry: JsonPathEntry): String =
    buildString {
        repeat(entry.depth) { append("· ") }
        append(entry.label)
    }

internal fun jsonPathQuickPickerButtonLabel(jsonPath: String, entries: List<JsonPathEntry>): String {
    val match = entries.find { it.path == jsonPath }
    return when {
        match != null -> jsonPathQuickPickerMenuLabel(match)
        jsonPath.isNotBlank() -> jsonPath
        else -> "—"
    }
}

/**
 * 结构面板重复键路径：同步 JSONPath + 结果区预览（同内联路径树单击），并复制路径（DIFF-489 / macOS 树复制路径）。
 */
internal fun jsonInspectorDuplicatePathClick(
    path: String,
    inspectorInput: String,
    session: JsonSession,
    container: AppContainer,
    pathAppliedNotice: String,
    translator: JsonTranslator,
    onChanged: () -> Unit,
): Boolean {
    val preview = jsonPathNodePreview(inspectorInput, path, translator, "")
    session.applyInlinePathTreePreview(path, preview, pathAppliedNotice)
    onChanged()
    return jsonInspectorCopyJsonPath(path, container)
}

internal fun jsonInspectorCopyJsonPath(path: String, container: AppContainer): Boolean =
    when (val outcome = JsonInspectorPresentation.runCopyJsonPath(path, container::copyText)) {
        JsonInspectorPresentation.CopyJsonPathOutcome.Empty -> false
        JsonInspectorPresentation.CopyJsonPathOutcome.Success -> {
            notifyJsonInspectorPathCopySuccess(container)
            true
        }
        JsonInspectorPresentation.CopyJsonPathOutcome.Failure -> {
            notifyJsonInspectorPathCopyFailure(container)
            false
        }
    }

internal fun notifyJsonInspectorPathCopySuccess(container: AppContainer) {
    if (JsonInspectorPresentation.shouldToastPathCopySuccess()) {
        container.toastSuccess(container.t("json.notice.pathCopied"))
    }
}

internal fun notifyJsonInspectorPathCopyFailure(container: AppContainer) {
    if (JsonInspectorPresentation.shouldToastPathCopyFailure()) {
        container.toastError(container.t("json.notice.copyFailed"))
    }
}

internal fun notifyJsonInspectorPathQueryFailure(
    container: AppContainer,
    session: com.rememberber.mootool.next.compose.sessions.JsonSession,
    message: String,
) {
    session.notice = message
    if (JsonInspectorPresentation.shouldToastPathQueryFailure()) {
        container.toastError(message)
    }
}

internal fun jsonInspectorCopyResult(displayText: String, container: AppContainer): Boolean =
    when (val outcome = JsonInspectorPresentation.runCopyResultText(displayText, container::copyText)) {
        JsonInspectorPresentation.CopyJsonPathOutcome.Empty -> false
        JsonInspectorPresentation.CopyJsonPathOutcome.Success -> {
            notifyJsonInspectorResultCopySuccess(container)
            true
        }
        JsonInspectorPresentation.CopyJsonPathOutcome.Failure -> {
            notifyJsonInspectorResultCopyFailure(container)
            false
        }
    }

internal fun notifyJsonInspectorResultCopySuccess(container: AppContainer) {
    if (JsonInspectorPresentation.shouldToastResultCopySuccess()) {
        container.toastSuccess(container.t("json.notice.copied"))
    }
}

internal fun notifyJsonInspectorResultCopyFailure(container: AppContainer) {
    if (JsonInspectorPresentation.shouldToastResultCopyFailure()) {
        container.toastError(container.t("json.notice.copyFailed"))
    }
}
