# DIFF-294：JSON `transform` 历史摘要对齐 Electron `runTransform`

## 问题

Electron `JsonTool.runTransform(success, summary)` 中 toast/notice 与写入历史的 `summary` 常不同，例如：

- 格式化：`json.notice.formatted` + 历史 `json.action.format`
- 压缩：`json.notice.compressed` + 历史 `json.action.compress`
- JSON 转义/反转义：`json.notice.escaped|unescaped` + 历史 `json.action.escape|unescape`

Compose `transform` 在 DIFF-293 前对多数动作用同一字符串；高级格式化已拆分，其余仍不一致。

## 行为

上述动作经 `transform(..., historySummary = …)` 写入历史；交换/字符串转义等 notice 与摘要相同的动作保持默认。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
