# DIFF-319：JSONPath 弹层选择对齐 Electron

## 问题

Electron `JsonPathPicker` 双击与「使用」仅 `onChoose(path)`：写入 `jsonPath`、关闭弹层、`notice = json.notice.pathApplied`，**不**执行 `queryJsonPath`、**不**把预览写入检查器 `pathResult`。

Compose 在 [DIFF-285](285-json-path-picker-double-query.md) 曾为弹层双击自动查询；「使用」还会把 `entry.preview` 写入 `pathResult`，与 Electron 不一致。

## 行为

- 新增 `JsonSession.applyPathPickerChoice`（`JsonInspectorResult.kt`）：弹层「使用」与双击共用；单测见 `JsonInspectorResultTest.applyPathPickerChoice_sets_path_and_notice_without_query_result`。
- 检查器**内联路径树**仍保持单击预览、双击查询（`feature-parity` / 内联树语义）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
