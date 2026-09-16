# DIFF-431：设置导航 Tab 聚焦 + 语言下拉焦点证据

## 背景

`acceptance.md` 已记录产品窗 `85-settings-appearance.png` 导航焦点环，但 `SettingsNavItem` 仅有 `mooFocusOutline`、**未** `focusable`，键盘 Tab 无法落到侧栏项（与 DIFF-064 首页链接不一致）。parity-gap 要求设置语言/外观 Tab 走查 Compose 回归帧。

## 行为

- `SettingsNavItem`：增加 `focusable`、可选 `modifier`。
- `MooSelect`：可选 `modifier` / `triggerModifier`（测试注入 `focusRequester`）。
- `ToolbarFocusCaptureTest`：`134-compose-settings-appearance-nav-tab-focus.png`、`135-compose-settings-language-select-tab-focus.png`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（541/541）
