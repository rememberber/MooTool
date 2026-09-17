# DIFF-498：分离窗主题与查找范围对照

## 背景

DIFF-497 在主窗与 `Themed` 分离窗传入 `compactNavigation`，但两处 `MooTheme` 参数重复，存在拆窗漏传风险；497「未做」亦提到分离窗主题对齐。JSON/随手记/Host/HTTP 查找在更早切片已接好，需与 Electron 范围对照并锁定 F05 为 Compose 扩展。

## 行为

- **`AppMooTheme` / `AppMooThemeInputs`**：主窗 `Main.kt` 与 `DetachedToolWindow` 共用同一套外观 + `layout.compactNavigation`，保证分离工具窗 p5 工具栏 dense 与主窗一致。
- **`EditorFindToolScope`**：Electron 壳层查找覆盖 JSON、随手记、Host、HTTP 响应；Compose 另含 F05 代码运行 RSTA 只读查找条（497 已接 `EditorFindOnlyBar` + 快捷键）。

## 验证

- `AppMooThemeInputsTest.mainAndDetachedWindowsShareCompactNavigationAndAppearance`
- `EditorFindToolScopeTest`
- `DetachedCompactP5CaptureTest` → `docs/evidence/2026-09-15-tray-density/captures/detached-compact-p5-toolbar.png`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、文本对比/格式化等 Electron 无查找的工具、产品窗分离窗走查帧、系统 IME 查找手工验收。
