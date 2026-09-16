# DIFF-279：Host 正文查找匹配高亮

## 对照 Electron

`codeEditorSearchHighlight` / `.cm-searchMatch`：查找打开且查询非空时，在编辑器内标出全部命中，当前命中更强对比。

## 行为

- `FindHighlightTransformation` + `findMatchAtSelection`：对 `BasicTextField` 施加 `VisualTransformation` 背景色。
- `MooTextField(TextFieldValue)` 增加可选 `visualTransformation`。
- Host 正文在 `findOpen` 且 `findQuery` 非空时高亮全部匹配，当前匹配（选区或 caret 所在命中）用更高 alpha 的 accent 背景。

## 验证

- `FindHighlightTransformationTest`
- `./gradlew :composeApp:desktopTest --offline`
