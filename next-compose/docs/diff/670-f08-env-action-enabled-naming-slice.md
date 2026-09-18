# DIFF-670：F08 环境变量 `*ActionEnabled` 命名与 UI 接线

基线：DIFF-669。

## 范围

- **F08**：`refreshActionEnabled` / `addVariableActionEnabled` / `saveEditorActionEnabled` / `deleteRowActionEnabled` / `confirmDeleteActionEnabled`（委托既有守卫）+ `VariablesScreen` 接线。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
