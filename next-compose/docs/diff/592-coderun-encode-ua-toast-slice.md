# DIFF-592：F05/F13/F12 失败 toast 呈现层 + 帧 187

基线：DIFF-591（工作区）。

## 范围

- **F05**：`CodeRunWiringPresentation.shouldToastRunFailure`（跳过 `ABORTED`）/ `shouldToastValidationFailure` + `notifyCodeRunFailure`（未配置运行时、参数解析、运行失败）。
- **F13**：`EncodeWiringPresentation.shouldToastConvertFailure` + `notifyEncodeFailure`。
- **F12**：`UaWiringPresentation.shouldToastParseFailure` + `notifyUaFailure`。
- **证据**：`RuntimeRunTabCaptureTest` → `187-compose-runtime-run-tab-focus.png`。

**不重复** 591：F06/F07/F25 / 帧 186。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
