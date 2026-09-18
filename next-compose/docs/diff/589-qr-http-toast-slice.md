# DIFF-589：F17 QR + F09 HTTP 失败 toast + 帧 184

基线：DIFF-588（工作区）。

## 范围

- **F17**：`QrWiringPresentation.shouldToastOperationFailure` + `notifyQrFailure`；生成/识别/Logo/剪贴板空 error toast。
- **F09**：`HttpRequestPresentation.shouldToastResponseError` + `notifyHttpFailure`；空 URL、空响应另存、复制失败、curl 导入失败、发送网络错误 toast。
- **证据**：`HttpSendButtonCaptureTest` → `184-compose-http-send-tab-focus.png`。

**不重复** 588：F22 调色板 / 帧 183。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
