# DIFF-491：命令盘 Tab 在搜索框与结果行间切换焦点

## 背景

Electron `CommandPalette` 的结果行是可聚焦 `<button>`，完整键盘走查可用 Tab 从搜索框移到当前结果并 Enter 打开（DIFF-056/434 已补方向键与结果行焦点环）。Compose 结果行虽已 `focusable`，但 Tab 会离开 overlay，无法在搜索与结果间闭环。

## 行为

- 搜索框按 **Tab**（无 Shift）且存在结果时，焦点落到**当前选中**结果行。
- 结果行按 **Shift+Tab** 回到搜索框。
- 焦点在结果行时仍支持 ↑/↓ 换行、Enter 打开、Esc 关闭（与搜索框内行为一致）。

## 验证

- `CommandPaletteKeysTest.tabFocusTransition_movesBetweenSearchAndSelectedResult`
- `./gradlew :composeApp:desktopTest --offline`

## 未做

产品窗命令盘 Tab 走查帧、关闭钮插入 Tab 序（Electron 为搜索行内按钮）。
