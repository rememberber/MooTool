# DIFF-271：HTTP 请求子面板 Compose 聚焦时不打开响应查找

## 对照 Electron

`HttpTool` 在 `event.target.closest('.http-request-pane')` 时跳过全局 `Cmd/Ctrl+F`；URL/超时/发送行在 pane 外，仍可打开响应查找。

## 行为

- `HttpSession.requestPaneComposeFocused`：由请求 Tabs + Params/Headers/Cookies 区 `focusGroup` + `onFocusChanged` 维护。
- `HttpFindShortcutPolicy`：在 [DIFF-270](270-http-find-ignore-request-body-focus.md) 的 Body RSTA 判定之外，当 `requestPaneComposeFocused` 为真时壳层不调用 `openHttpResponseFind`。
- Body `EditorHost` 仍为 Swing 焦点，继续单独检测 `bodyEditor.area`。

## 验证

- `HttpFindShortcutPolicyTest`（含 JFrame 下 Body 焦点用例）
- `./gradlew :composeApp:desktopTest --offline`
