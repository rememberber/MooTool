# DIFF-587：F08 环境 CRUD/刷新失败 toast + F03 格式化失败 toast + 帧 182

基线：DIFF-586（工作区）。

## 范围

- **F08**：`EnvWiringPresentation.shouldToastOperationFailure` + `notifyEnvFailure`；刷新快照、删除、保存变量失败 error toast（导出失败已有 toast）。
- **F03**：格式化失败 `ReformatWiringPresentation.shouldToastFormatFailure` + error toast。
- **证据**：`EnvAddButtonCaptureTest` → `182-compose-env-add-variable-tab-focus.png`。

**不重复** 586：F01 Vault toast / 帧 181。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
