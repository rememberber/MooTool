# DIFF-270：HTTP 请求 Body 聚焦时不打开响应查找

## 对照 Electron

`HttpTool` 全局 `keydown` 在 `event.target.closest('.http-request-pane')` 时直接返回，不在请求区触发响应 `openFind`。

## 行为

- `HttpFindShortcutPolicy.shouldOpenResponseFindFromShell`：当 `bodyEditor`（RSTA）持有 Swing 焦点时，壳层 `Cmd/Ctrl+F` 不调用 `openHttpResponseFind`。
- 响应编辑器快捷键与「查找」按钮行为不变。
- Params/Headers/Cookies Compose 焦点见 [DIFF-271](271-http-find-ignore-request-pane-compose.md)。

## 验证

- `HttpFindShortcutPolicyTest`
- `./gradlew :composeApp:desktopTest --offline`
