# DIFF-656：F15 正则测试 `*ActionEnabled`

基线：DIFF-655（工作区）。

## 范围

- **F15**：`RegexWiringPresentation.runTestActionEnabled` / `cancelTestActionEnabled`；`RegexScreen` 测试/取消钮接线。
- **单测**：`RegexWiringPresentationTest.runTestActionEnabledMatchesGuards`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
