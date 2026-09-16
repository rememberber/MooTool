# DIFF-276：Host 打开查找预填正文选区

## 对照 Electron

`HostTool.openFindReplace` 读取正文编辑器选区写入查找框。

## 行为

- `TextFieldValue.selectedTextForFind()`（与 RSTA `selectedTextForFind` 语义一致）。
- Host 正文改用 `MooTextField(TextFieldValue)` 保留选区；`Cmd/Ctrl+F/R` 与「查找」按钮打开时预填。
- `MooTextField` 增加 `TextFieldValue` 重载供 Host 使用。

## 验证

- `TextFieldFindSeedTest`
- `./gradlew :composeApp:desktopTest --offline`
