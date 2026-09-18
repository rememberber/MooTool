# DIFF-653：F08 环境变量导出 `tools.exportDirectory`

基线：DIFF-652（工作区）。

## 范围

- **F08**：`EnvWiringPresentation.exportActionEnabled`（委托 `exportEnabled`）；`VariablesScreen` 溢出菜单导出改用 `chooseFileWithExportDirectory` + 成功写盘后 `persistToolsExportDirectory`。
- **单测**：`EnvWiringPresentationTest.exportRequiresSnapshot` 增补 `exportActionEnabled` 断言。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
