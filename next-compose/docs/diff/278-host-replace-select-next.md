# DIFF-278：Host 单次替换后选中下一处匹配

## 对照 Electron

`HostTool.replaceCurrent(false)` 在替换成功后用 `findNextMatch(..., result.nextFrom, true)` 选中下一处。

## 行为

- `HostFindNavigation.afterReplaceAndSelectNext`：写入新正文后从本次替换区间末尾继续 `findNext` 并更新 `TextFieldValue` 选区。
- Host 查找条「替换」按钮走该逻辑。

## 验证

- `HostFindNavigationTest`
- `./gradlew :composeApp:desktopTest --offline`
