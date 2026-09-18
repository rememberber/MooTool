# DIFF-629：F05 运行台 displayName / cancel 接线

基线：DIFF-628（A03 commit 冲突守卫）。

## 范围

- **F05**：`CodeRunWiringPresentation.displayName` / `cancelRun` 委托 `CodeRunEngine`；`CodeRunScreen` 横幅、Tab、检测行、历史摘要与停止按钮不再直调引擎。
- **单测**：`CodeRunWiringPresentationTest.displayNameMatchesElectronRuntimeTools`（四 runtime）、`cancelRunDelegatesToEngine`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
