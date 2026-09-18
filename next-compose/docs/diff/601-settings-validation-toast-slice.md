# DIFF-601：A01 设置校验失败 toast 呈现层 + 帧 196

基线：DIFF-600（工作区）。

## 范围

- **A01**：`SettingsValidationToastPresentation.shouldToastValidationFailure` + `notifySettingsValidationFailure`；`SettingsScreen` 数值/网络/Vault/备份路径校验、迁移导入与 AI 安装/连接失败 toast 统一 helper。
- **证据**：`SettingsEditorFontSizeRowCaptureTest` → `196-compose-settings-editor-font-size-row-tab-focus.png`。

**不重复** 600：F04 检查器路径复制 / 帧 195。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
