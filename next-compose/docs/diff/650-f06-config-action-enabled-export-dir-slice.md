# DIFF-650：F06 配置转换 `*ActionEnabled` + `tools.exportDirectory`

基线：DIFF-649（工作区）。

## 范围

- **F06**：`ConfigWiringPresentation.propertiesToYamlActionEnabled` / `yamlToPropertiesActionEnabled` / `validateYamlActionEnabled` / `formatYamlActionEnabled` / `exportTextActionEnabled`；`ConfigConvertScreen` 转换/校验/导出钮 `enabled` 接线。
- **F06**：导入/导出改用 `chooseFileWithExportDirectory` + 成功 IO 后 `persistToolsExportDirectory`（对齐 F03/F04 `tools.exportDirectory` 链）。
- **单测**：`ConfigWiringPresentationTest.toolbarActionEnabledMatchesGuards`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
