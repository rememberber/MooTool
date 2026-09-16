# DIFF-272：HTTP 超时写入全局设置与 Enter 发送

## 对照 Electron

`HttpTool`：`commitTimeout` 在超时输入 `blur`、按 Enter 以及 `sendRequest` 时将 `timeoutMs` 夹紧到 1–120s 并写入 `settings.network.requestTimeoutMs`；URL 输入 Enter（非修饰键）在未发送时触发请求。

## 行为

- `HttpTimeoutSettings.commit`：夹紧会话超时并与全局设置比较，必要时 `AppContainer.updateSettings`。
- `HttpScreen`：设置页变更时 `LaunchedEffect` 同步会话超时；超时框失焦/Enter 提交；发送前同样提交；URL 框 Enter 发送。
- 单测：`HttpTimeoutSettingsTest`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
