# DIFF-240：JSON 检查器手改 JSONPath 清空旧查询结果

## 背景

用户修改检查器 JSONPath 输入框后，若仍保留上次 `pathResult`，结果区会继续展示与当前表达式不一致的预览（编辑器正文变更已在 DIFF-227 清空 `pathResult`）。

## 行为

- `JsonSession.applyInspectorJsonPathInput`：写入新路径、清空 `pathResult`；若 `notice` 仅为「路径已应用」则一并清空。
- 检查器 JSONPath `MooTextField` 接入。

## 验证

- `JsonInspectorResultTest.applyInspectorJsonPathInput_clears_stale_path_result`
- `./gradlew :composeApp:desktopTest --offline`
