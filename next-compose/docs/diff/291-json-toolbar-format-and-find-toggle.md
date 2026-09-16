# DIFF-291：JSON 工具栏快速格式化 2 空格 + 查找折叠保留已替换计数

## 问题

- Electron `JsonToolbar` / `JsonTool` 主工具栏「格式化」与 `Cmd/Ctrl+Shift+F` 固定 `formatJson(..., 2)`；检查器「应用格式」才使用 `formatOptions`（含 2/4 与排序等）。Compose 主工具栏误用 `session.formatOptions.spaces`。
- Electron 工具栏再次点「查找」仅 `findOpen: false`；`replacedCount` 仅在 `FindReplaceBar` 关闭时清零。Compose 工具栏折叠查找时误清零 `findReplacedCount`（JSON/随手记）。

## 行为

- `JsonToolbarFormatPolicy.QUICK_FORMAT_SPACES = 2`：主工具栏按钮、壳层与 RSTA `Cmd/Ctrl+Shift+F`、编辑器快捷键格式化均使用该常量。
- 检查器「应用格式」仍调用 `JsonEngine.formatAdvanced(..., session.formatOptions)`。
- JSON/随手记工具栏关闭查找条：只设 `findOpen = false`；Esc / 查找条「关闭」/ 重新打开查找仍按 DIFF-287 归零。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- `JsonToolbarFormatPolicyTest`
