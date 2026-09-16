# DIFF-455：JSON 检查器内联路径树双击查询

## 背景

Electron 内联路径树双击应执行 JSONPath 查询并更新结果区；弹层双击仅选路径（DIFF-319）。Compose 原先在 `JsonScreen` 内联实现「预览 + `showResult`」，查询逻辑未集中单测。

## 行为

- `JsonSession.performInlinePathTreeDoubleTapQuery`：单击预览 + `JsonEngine.queryPath` 写入 `pathResult`；非法路径清空 `pathResult` 并保留错误 `notice`。
- `JsonScreen` 双击回调改为调用上述函数，成功时同步结果对话框与历史，失败时 `toastError`（不再重复查询）。
- `JsonInspectorResultTest` 覆盖成功/非法 filter 两条路径。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（612/612）
