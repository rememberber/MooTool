# DIFF-641：F04 JSON 检查器转换九动作 `*ActionEnabled`

基线：DIFF-640（工作区）。

## 范围

- **F04**：`JsonInspectorPresentation.jsonStructureConvertActionEnabled` / `editorTextConvertActionEnabled` / `conversionDialogActionEnabled`；检查器转换两列网格各钮 `enabled` 接线（结构类需可解析 JSON；转义类需编辑器非空；XML/Bean 对话框入口常开）。
- **实现**：`InspectorGridAction` + `InspectorActionGrid` 支持 `enabled`。
- **单测**：`JsonInspectorPresentationTest.inspectorConvertActionEnabled`。

**不重复** 639：639 为应用格式/选路径；本切片为转换九动作网格。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
