# DIFF-434：命令盘结果行 Tab 焦点环

## 背景

命令盘（Cmd/Ctrl+K）结果行用方向键高亮，但 Tab 聚焦时外描边曾被 `clip` 裁切；`mooFocusClickable` 的 `focusable` 在 `clickable` 之后，与 `SettingsNavItem` 不一致，部分控件 Tab 时焦点态不稳定。

## 行为

- `Workbench.kt` `CommandSearch` 结果行：`mooFocusOutline` 置于 `clip` 之前；Tab 聚焦时 `focusRing` 描边与侧栏导航一致。
- `Controls.kt` `mooFocusClickable`：`focusable` 置于 `clickable` 之前。
- `ToolbarFocusCaptureTest`：`140-compose-command-palette-result-tab-focus.png`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（546/546）
