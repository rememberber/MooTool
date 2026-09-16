# DIFF-266：打开查找条预填编辑器选区

## 对照 Electron

`JsonTool.openFindReplace` / `QuickNoteTool.openFindReplace`：打开查找时若有非空选区，将选中文本写入查找框。

## 行为

- `EditorBuffer.selectedTextForFind()` + `openFindBarSeedingSelection`（须在 EDT 读取 RSTA 选区）
- JSON / 随手记均写入会话 `findQuery`
- 覆盖 Cmd/Ctrl+F、编辑器内快捷键、工具栏「查找」打开路径（关闭查找仍直接关条，不预填）

## 验证

- `FindBarOpenTest`
- `./gradlew :composeApp:desktopTest --offline`
