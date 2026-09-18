# DIFF-626：F05 运行台 `runtimeTools` vitest 登记 + 显示名单测

基线：DIFF-625（工作区）。

## 范围

- **F05**：`CodeRunEngine.displayName` 对齐 Electron `runtimeDisplayName`（`Node.js` / `Groovy`）；`CodeRunEngine.formatSource(Node)` 走 `CodeEditorSurfaceFormatEngine.formatJavascript`，对齐 Electron `formatRuntimeSource('node')` 常见样本（如 `const x = { a: 1 }`）。
- **fixture**：`docs/fixtures/electron-next-runtimeTools-vitest.md`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
