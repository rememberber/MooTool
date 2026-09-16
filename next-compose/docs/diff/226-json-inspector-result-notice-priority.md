# DIFF-226：JSON 检查器结果区 notice 与 pathResult 优先级

## 背景

[DIFF-225](225-json-inspector-result-primary.md) 在非空时始终优先 `pathResult`，导致 Vault 保存、格式化等写入 `notice` 后，侧栏仍显示上一次 JSONPath 查询结果。

## 行为

- 抽取 `jsonInspectorResultDisplay`：当 `notice` 为 JSONPath 面板标题或 `json.notice.pathApplied` 时仍优先 `pathResult`；否则优先 `notice`。
- 路径树单击与双击选中路径时同步 `json.notice.pathApplied`，与路径选择器一致。

## 验证

- `JsonInspectorResultTest`
- `./gradlew :composeApp:desktopTest --offline`
