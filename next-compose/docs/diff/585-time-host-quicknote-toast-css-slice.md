# DIFF-585：F18/F10/F01 Vault 失败 toast + 时间转换行 CSS + 帧 180

基线：DIFF-584（工作区）。

## 范围

- **F18**：转换失败 `TimeWiringPresentation.shouldToastConvertFailure` + error toast；`mooTimeConvertActions`。
- **F10**：`notifyHostFailure` — 保存/重命名/读系统 Host/应用预览/恢复/应用失败 toast。
- **F01**：`quickNoteSaveCurrent` 在 `showToast` 时保存失败 toast；`quickNoteVaultSaveIfNeeded` 失败 toast。
- **证据**：`TimeConvertActionsCaptureTest` → `180-compose-time-to-local-tab-focus.png`。

**不重复** 584：F20 翻译 / F12 密钥 / 帧 179。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
