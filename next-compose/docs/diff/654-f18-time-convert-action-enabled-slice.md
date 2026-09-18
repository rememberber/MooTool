# DIFF-654：F18 时间转换 `*ActionEnabled`

基线：DIFF-653（工作区）。

## 范围

- **F18**：`TimeWiringPresentation.timestampToLocalActionEnabled` / `localToTimestampActionEnabled` / `copyFieldActionEnabled`；`TimeConvertScreen` 转换与复制钮 `enabled` 接线（含本地时间复制钮此前未禁用）。
- **单测**：`TimeWiringPresentationTest.toolbarActionEnabledMatchesGuards`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
