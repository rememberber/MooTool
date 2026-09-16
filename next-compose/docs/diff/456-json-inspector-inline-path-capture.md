# DIFF-456：JSON 检查器内联路径查询 Compose 证据帧 `147`

## 背景

DIFF-455 落地内联路径树双击查询逻辑；需要可回归的 Compose 场景图展示结果区与选中路径行（仍非产品主窗验收）。

## 行为

- `JsonInspectorCaptureTest.captureInlinePathDoubleTapQueryResult`：模拟 `performInlinePathTreeDoubleTapQuery` 后渲染结果区 + `JsonPathListRow`，写出 `147-compose-json-inspector-inline-path-query.png`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（613/613）
- 证据：`docs/evidence/2026-09-15-inspector-screencapture/windows/147-compose-json-inspector-inline-path-query.png`
