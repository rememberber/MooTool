# DIFF-225：JSON 检查器结果区优先展示 pathResult

## 背景

[DIFF-194](194-json-path-query-inspector-result.md) 将 JSONPath 查询写入 `pathResult`，但结果卡片同时展示 `notice`（常为操作标题）与 `pathResult`，用户需在两行中辨认真实查询输出。

## 行为

- 结果区主文案：`pathResult` 非空时用其内容，否则 `notice`，再否则校验 `status.message`。
- 错误着色仍在校验失败且未写入 `pathResult` 时生效。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
