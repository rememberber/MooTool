# DIFF-331：内联路径树预览仅对选中行求值

## 问题

[DIFF-330](330-json-path-list-row-inline-preview.md) 后，内联路径树在每次重组时对最多 80 条节点各调用 `jsonPathNodePreview`（内部 `queryPath`），大 JSON 下侧栏重组成本过高。

## 行为

- 选中行预览用 `remember(editor, jsonPath)` 缓存一条 `selectedPathPreview`。
- `inlinePreviewText` 仅选中行传入；点击/双击仍对目标路径单次求值。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（**452/452**）
