# DIFF-281：随手记完整查找替换条 + RSTA 导航

## 对照 Electron

`QuickNoteTool` 使用完整 `FindReplaceBar`（选项、上/下条、替换/全部替换）；`replaceCurrentMatch` 在选区整段命中时优先替换选区。

## 行为

- `QuickNoteFindBar`：与 JSON 查找条同套控件与 [DIFF-280](280-find-bar-keyboard.md) 键盘。
- `RstaFindNavigation`：RSTA 编辑器上/下条与「替换并选中下一处」。
- `FindReplace.replaceCurrent`：选区为单一完整命中时先替换选区（对齐 Electron `replaceCurrentMatch`）；Host 替换传入选区。
- JSON「替换」改用 `RstaFindNavigation.replaceAndSelectNext` 与按选区跳转。

## 验证

- `FindReplaceTest.replaceCurrent_prefers_exact_selection_match`
- `RstaFindNavigationTest`
- `./gradlew :composeApp:desktopTest --offline`
