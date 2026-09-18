# DIFF-590：F11 网络失败 toast + 帧 185

基线：DIFF-589（工作区）。

## 范围

- **F11**：`NetWiringPresentation.shouldToastNetworkError` / `shouldToastLocalFailure` + `notifyNetFailure`；刷新本机地址、命令启动校验失败、IPv4↔Long 转换失败 error toast。
- **证据**：`NetPingCommandCaptureTest` → `185-compose-net-ping-command-tab-focus.png`。

**不重复** 589：F17/F09 / 帧 184。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
