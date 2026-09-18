# DIFF-669：F12/F21/F25 `*ActionEnabled` 命名与 UI 接线

基线：DIFF-668。

## 范围

- **F12**：`parseActionEnabled` / `copyResultActionEnabled`（委托既有 `canParse`/`canCopyResult`）+ `UaParseScreen`。
- **F21**：`evaluateActionEnabled` / `copyResultActionEnabled` + `CalculatorScreen`。
- **F25**：`refreshActionEnabled` / `copyReportActionEnabled` + `HardwareScreen`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
