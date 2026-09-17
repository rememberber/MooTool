# DIFF-492：命令盘关闭钮 Tab 焦点序

## 背景

[DIFF-491](491-command-palette-tab-result-focus.md) 已让 Tab 在搜索框与选中结果行间切换。Electron `CommandPalette.tsx` 搜索行 DOM 顺序为 `input` → 关闭 `<button>` → 结果区各 `<button>`，Tab 会先落到关闭钮再进入结果。Compose 此前从搜索框 Tab 直接跳到结果，跳过关闭钮。

## 行为

- 搜索框 **Tab** → 关闭钮（`app.search.close`）。
- 关闭钮 **Shift+Tab** → 搜索框；**Tab**（有结果时）→ 当前选中结果行。
- 结果行 **Shift+Tab** → 关闭钮（不再直接回搜索框）。
- 关闭钮 **Enter** 关闭命令盘；无结果时关闭钮 **Tab** 离开 overlay。

## 验证

- `CommandPaletteKeysTest.tabFocusTransition_movesSearchCloseAndResult`
- `ToolbarFocusCaptureTest.captureCommandPaletteCloseButtonFocusRing` → `149-compose-command-palette-close-tab-focus.png`
- `./gradlew :composeApp:desktopTest --offline`

## 未做

产品窗命令盘完整 Tab 走查；结果行 Tab 继续向后遍历每条结果（Electron 为独立 button，Compose 仍只聚焦当前选中行）。
