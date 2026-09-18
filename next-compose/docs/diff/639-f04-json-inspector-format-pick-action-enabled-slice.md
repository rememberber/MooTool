# DIFF-639：F04 JSON 检查器「应用格式」/「选择路径」`*ActionEnabled`

基线：DIFF-638（工作区）。

## 范围

- **F04**：`JsonInspectorPresentation.formatAdvancedActionEnabled`（委托 `inferSchemaEnabled`）、`pathPickerOpenActionEnabled`（无 `listPaths` 条目时禁用）；`JsonScreen` 检查器「应用自定义格式」与「选择 JSONPath」钮 `enabled` 接线。
- **单测**：`JsonInspectorPresentationTest.formatAdvancedAndPathPickerOpenActionEnabled`。

**不重复** 638：638 为 JSONPath 复制/查询；本切片为高级格式化与路径弹层入口。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
