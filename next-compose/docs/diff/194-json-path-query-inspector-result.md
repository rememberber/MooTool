# DIFF-194：JSONPath 查询结果写入检查器「结果」区

## 背景

[DIFF-173](173-json-inspector-section-order.md) 规定检查器 **结果** 卡片展示 JSONPath 查询输出；此前 `showResult` 仅打开结果对话框，未同步 `session.pathResult`，侧栏结果区在点击「查询」后仍可能只有树节点预览。

## 行为

- **F04**：检查器「查询」与路径树双击查询在成功时除结果对话框外，将 `JsonEngine.queryPath` 输出写入 `pathResult`；失败时清空 `pathResult` 并保留错误 `notice`/toast。
- 对话框标题与 Electron 一致使用 `json.panel.jsonPath`（不再误用 `json.notice.pathApplied`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- 对照 `next/src/features/json/JsonTool.tsx` `onQueryPath` + `JsonInspector` 结果区
