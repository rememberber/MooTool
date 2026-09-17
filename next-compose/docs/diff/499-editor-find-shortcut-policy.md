# DIFF-499：壳层查找快捷键与 EditorFindToolScope 接线

## 背景

DIFF-498 新增 `EditorFindToolScope` 对照 Electron 查找覆盖范围，但各工具页仍各自调用 `FindReplaceShortcutPolicy` 或硬编码 `Key.F`，HTTP 与 JSON/Host 的差异未集中表达。

## 行为

- **`EditorFindShortcutPolicy.opensShellFind`**：先判定 `EditorFindToolScope.supportsComposeFind`；HTTP 对齐 Electron 仅 `Cmd/Ctrl+F`（不含 `R`）；JSON/随手记/Host/F05 仍接受 `F`/`R`。
- JSON、随手记、Host、HTTP、代码运行壳层 `onPreviewKeyEvent` 改走该策略（HTTP 仍叠加 `HttpFindShortcutPolicy` 请求区守卫）。

## 验证

- `EditorFindShortcutPolicyTest`
- 既有 `EditorFindToolScopeTest` / `HttpFindShortcutPolicyTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

文本对比/格式化等无 Electron 查找的工具、六套 CSS 皮肤、产品窗走查帧、P7 三平台安装验收。
