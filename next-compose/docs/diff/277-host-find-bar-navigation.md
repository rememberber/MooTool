# DIFF-277：Host 查找条上/下条与选项

## 对照 Electron

`HostTool` 使用 `FindReplaceBar`：`findAround` 在正文中选中下一处/上一处；支持 matchCase/wholeWord/regex；替换使用当前选区/光标。

## 行为

- `HostFindNavigation.jump`：根据 `TextFieldValue` 选区调用 `FindReplace.findNext` 并更新选区。
- Host `FindBar` 补齐上一处/下一处、三项选项开关；`replaceCurrent` 使用正文光标而非固定 `0`；无匹配时状态栏 `json.notice.noMatches`。

## 验证

- `HostFindNavigationTest`
- `./gradlew :composeApp:desktopTest --offline`
