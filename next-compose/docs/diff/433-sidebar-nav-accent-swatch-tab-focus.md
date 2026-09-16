# DIFF-433：侧栏导航/分离钮 Tab 聚焦 + 强调色色片焦点环

## 背景

侧栏 `NavItem` 主行与 ⧉/▣ 分离钮已有 `mooFocusOutline`，需与 DIFF-431/432 一致保证 `focusable` 与键盘 Tab 可达。设置「外观」强调色 `AccentSwatches` 在 DIFF-431 已 `focusable`，但修饰符顺序导致焦点外描边被 `clip` 裁切，Tab 时几乎不可见。

## 行为

- `Sidebar.kt`：`NavItem` 主行与分离/收回钮保持 `focusable(true, interaction)`（与 Electron 侧栏可键盘操作对齐）。
- `SettingsChrome.kt` `AccentSwatches`：`mooFocusOutline` 置于 `clip` 之前；`focusable` 置于 `clickable` 之前（与 `SettingsNavItem` 一致）。
- `ToolbarFocusCaptureTest`：`138-compose-sidebar-nav-item-tab-focus.png`、`139-compose-settings-accent-swatch-tab-focus.png`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（545/545）
