# DIFF-430：JSONPath 选择器预览对齐 Electron `formatPreview`

## 背景

Electron `JsonPathPicker` 预览区对标量 **字符串** 显示原文（无 JSON 引号），对象/数组 `JSON.stringify` 缩进；路径不存在则为空。Compose 此前经 `queryPath` 预览，字符串会显示为 `"..."`，与弹层/内联树不一致。

## 行为

- `JsonEngine.pathPickerPreview` + `formatPickerPreview`：对齐 `formatPreview`。
- `jsonPathNodePreview` 改走 `pathPickerPreview`（检查器内联树、路径弹层右侧预览）。
- JSONPath **查询**仍用 `queryPath` / `formatPathResult`（字符串带 JSON 引号，与 Electron `formatJsonPathValue` 一致）。

## 验证

- `JsonPathNodePreviewTest`（含 `preview_string_scalar_matches_electron_formatPreview`）
- `./gradlew :composeApp:desktopTest --offline`
