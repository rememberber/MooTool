# DIFF-599：F08/F17/F22 失败 toast 呈现层收尾 + 帧 194

基线：DIFF-598（工作区）。

## 范围

- **F08**：`notifyEnvIoFailure` + `EnvWiringPresentation.shouldToastIoFailure`（导出写盘失败）。
- **F17**：`notifyQrIoFailure`；`notifyQrFailure` 文案路径经 `shouldToastErrorMessage`。
- **F22**：`notifyColorFailure` 文案路径经 `shouldToastErrorMessage`（Throwable 路径仍 `shouldToastOperationFailure`）。
- **证据**：`EnvExportTabCaptureTest` → `194-compose-env-export-tab-focus.png`。

**不重复** 598：F18/F11/A03 / 帧 193。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
