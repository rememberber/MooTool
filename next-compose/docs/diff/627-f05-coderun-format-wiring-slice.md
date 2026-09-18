# DIFF-627：F05 运行台格式化 `CodeRunWiringPresentation` 接线

基线：DIFF-626（Node `CodeRunEngine.formatSource` 对齐 Electron）。

## 范围

- **F05**：`CodeRunWiringPresentation.formatSource` 委托 `CodeRunEngine.formatSource`；`CodeRunScreen` 格式化按钮与编辑器快捷键走 Presentation（与 `runCode`/`parseRunArguments` 同层）。
- **单测**：`CodeRunWiringPresentationTest.formatSourceDelegatesToEngineForNodeSample`（Node + Python 样本，延续 `runtimeTools.test.ts` fixture）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
