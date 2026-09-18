# DIFF-593：F14/F15/F16/F19 失败 toast 呈现层 + 帧 188

基线：DIFF-592（工作区）。

## 范围

- **F14**：`CryptoWiringPresentation.shouldToastOperationFailure` / `shouldToastVerifyFailure` + `notifyCryptoFailure`（密钥生成/还原/运算/摘要/随机失败；验签失败仍 toast）。
- **F15**：`RegexWiringPresentation.shouldToastWorkerError`（跳过 `cancelled`）+ `notifyRegexFailure`。
- **F16**：`CronWiringPresentation.shouldToastPreviewFailure` + `notifyCronFailure`。
- **F19**：`MessageBoardWiringPresentation.shouldToastWakeFailure` + `notifyMessageBoardWakeFailure`（显示器唤醒失败）。
- **证据**：`CryptoGenerateKeyTabCaptureTest` → `188-compose-crypto-generate-key-tab-focus.png`。

**不重复** 592：F05/F13/F12 / 帧 187。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
