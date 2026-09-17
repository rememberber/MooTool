package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.JsonSession

/** F04 历史恢复（对齐 Electron `onApplyHistory`：写回格式化/转换结果，不写 restored notice）。 */
object JsonHistoryRestore {
    fun apply(session: JsonSession, item: HistoryRecord) {
        if (JsonHistoryMetadata.isPathQuery(item.options)) {
            session.pathResult = item.output
            session.dialogTitle = item.summary
            session.dialogBody = item.output
        } else {
            session.pathResult = ""
            session.dialogTitle = ""
            session.dialogBody = ""
        }
    }

    /** JSONPath 历史只恢复结果区，不改编辑器正文。 */
    fun editorText(item: HistoryRecord): String? =
        if (JsonHistoryMetadata.isPathQuery(item.options)) null
        else item.output.ifBlank { item.input }
}
