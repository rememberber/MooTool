# DIFF-295：JSON 转换结果弹层复制写入状态栏 notice

## 问题

Electron `JsonTool` 结果弹层「复制」走 `copyValue`，会更新 `notice`（`json.notice.copied` / `copyFailed`）并 toast。Compose `ResultDialog` 仅调用 `container.copyText`（有 toast），未同步 `session.notice`，底部状态栏无复制反馈。

## 行为

- 结果弹层「复制」：`copyText` 的 `CopyOutcome` 写入 `session.notice`（与工具栏复制一致）。工具栏按钮 `copyState` 见 [DIFF-296](296-json-result-dialog-copy-feedback.md)。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
