# DIFF-290：HTTP 响应查找无匹配提示

## 问题

Electron `HttpTool.findAround` 在无下一处匹配时 `toast.info(findReplace.noMatches)`。Compose 响应查找条在 `matches` 为空时仍允许点「查找/上一处/下一处」，但不反馈。

## 行为

- 查找词非空且当前响应 Tab 下 `FindReplace.findAll` 结果为 0 时：点击 `find.find` / 上一处 / 下一处或 Enter 等同逻辑，写入 `session.notice`（`json.notice.noMatches`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
