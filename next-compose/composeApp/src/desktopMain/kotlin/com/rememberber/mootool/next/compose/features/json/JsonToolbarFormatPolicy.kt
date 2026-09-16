package com.rememberber.mootool.next.compose.features.json

/**
 * Electron `JsonToolbar` / `JsonTool` 主工具栏「格式化」与 `Cmd/Ctrl+Shift+F` 固定 2 空格；
 * 检查器「应用格式」走 [com.rememberber.mootool.next.compose.domain.JsonEngine.formatAdvanced] 与会话 [formatOptions]。
 */
object JsonToolbarFormatPolicy {
    const val QUICK_FORMAT_SPACES: Int = 2
}
