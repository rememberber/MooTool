# DIFF-330：内联路径树选中行预览与弹层一致

## 问题

[DIFF-329](329-json-path-picker-preview-format.md) 已让弹层与 `pathResult` 使用 `jsonPathNodePreview`，但 `JsonPathListRow` 在 `showInlinePreview` 时仍展示 `listPaths` 的缩写 `entry.preview`（`{n}` / `[n]`）。

## 行为

- `JsonPathListRow` 增加可选 `inlinePreviewText`；内联路径树传入 `jsonPathNodePreview(...)`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（**452/452**）
