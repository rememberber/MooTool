# DIFF-652：F13 编码转换 `*ActionEnabled`

基线：DIFF-651（工作区）。

## 范围

- **F13**：`EncodeWiringPresentation.forwardConvertActionEnabled` / `reverseConvertActionEnabled`；`EncodeScreen` 正/反向转换钮 `enabled` 接线。
- **单测**：`EncodeWiringPresentationTest.convertActionEnabledMatchesGuards`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
