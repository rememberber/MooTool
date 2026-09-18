# DIFF-598：F18/F11/A03 失败 toast 呈现层收尾 + 帧 193

基线：DIFF-597（工作区）。

## 范围

- **F18**：`notifyTimeConvertFailure`（沿用 `TimeWiringPresentation.shouldToastConvertFailure`）。
- **F11**：`notifyNetActionResult` 网络任务完成失败 toast 经 `shouldToastNetworkError`（跳过 `ABORTED`）。
- **A03**：`VaultGitToastPresentation` + `notifyVaultGitPanelFailure` / `notifyVaultGitActionFailure` / `notifyVaultGitFlushBlocked` / `notifyVaultGitInvalidRemote`。
- **证据**：`TimeToTimestampTabCaptureTest` → `193-compose-time-to-timestamp-tab-focus.png`。

**不重复** 597：F01/F09 / 帧 192。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
