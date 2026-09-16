# DIFF-329：JSONPath 弹层/内联树预览对齐 Electron `formatPreview`

## 问题

Electron `JsonPathPicker` 右侧预览对标量返回 JSON 字符串、对对象/数组 `JSON.stringify(value, null, 2)`。Compose `listPaths` 的 `preview` 对对象/数组仅 `{n}` / `[n]` 缩写，弹层与内联树单击预览信息不足。

## 行为

- 新增 `jsonPathNodePreview`：经 `JsonEngine.queryPath`（与 `formatPathResult` 一致）生成预览，失败时回退树行 `preview`。
- `PathPickerDialog` 预览列、检查器内联路径树单击/双击前的预览均使用该 helper。

## 验证

- `JsonPathNodePreviewTest`
- `./gradlew :composeApp:desktopTest --offline`
