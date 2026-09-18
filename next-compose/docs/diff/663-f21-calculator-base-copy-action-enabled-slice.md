# DIFF-663：F21 计算器进制转换/复制 `*ActionEnabled`

基线：DIFF-662（工作区）。

## 范围

- **F21**：`convertBaseActionEnabled`（委托 `CalculatorEngine.convertBase`）接线 HEX/DEC/BIN 四向转换钮；溢出「复制」`canCopyResult` 接线。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
