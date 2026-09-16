# DIFF-293：JSON 检查器转义文案与高级格式化历史摘要

## 问题

Electron `JsonTool`：

- `escapeJavaString` / `unescapeJsonText` 的 toast/notice 使用 `json.action.escapeText` / `json.action.unescapeText`（不是 `json.notice.escaped`）。
- `formatJsonAdvanced` 的 `runTransform`：界面提示为 `json.notice.formatted`，写入历史的 `summary` 为 `json.format.apply`。

Compose 误将「字符串转义/反转义」写成 escaped/unescaped 通用 notice；高级格式化历史摘要也用了 formatted 文案。

## 行为

- 检查器「字符串转义」「字符串反转义」：`transform` 的 notice/toast 对齐动作标题键。
- 检查器「应用格式」：`notice` = `json.notice.formatted`，`history.save` 摘要 = `json.format.apply`。
- `transform(..., historySummary = notice)` 默认保持 notice 与历史摘要相同的动作（如交换、字符串转义）；格式化/压缩/JSON 转义的历史摘要见 [DIFF-294](294-json-transform-history-summary.md)。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
