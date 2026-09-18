# DIFF-649：F03 格式化 `*ActionEnabled`

基线：DIFF-648（工作区）。

## 范围

- **F03**：`ReformatWiringPresentation.formatActionEnabled` / `copyResultActionEnabled` / `saveResultActionEnabled`；`ReformatScreen` 格式化、复制、另存（工具栏与溢出菜单）`enabled` 接线。
- **单测**：`ReformatWiringPresentationTest.toolbarActionEnabledMatchesGuards`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
