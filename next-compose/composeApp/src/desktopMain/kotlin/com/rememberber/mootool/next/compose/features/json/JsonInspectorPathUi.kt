package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.JsonPathEntry

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

internal fun jsonInspectorCopyJsonPath(path: String, container: AppContainer): Boolean {
    val trimmed = path.trim()
    if (trimmed.isEmpty()) return false
    val success = container.copyText(trimmed)
    if (success) {
        container.toastSuccess(container.t("json.notice.pathCopied"))
    } else {
        container.toastError(container.t("json.notice.copyFailed"))
    }
    return success
}
