# DIFF-432：侧栏语言切换 Tab 聚焦 + 搜索钮焦点证据

## 背景

侧栏 `LanguageRow`（中/EN/日）有 `mooFocusOutline` 但缺少 `focusable`，键盘 Tab 无法切换语言（与 DIFF-431 设置导航同类问题）。侧栏 ⌕ 搜索使用 `MooGhostButton`（已 `focusable`），需 Compose 回归帧补充。

## 行为

- `Sidebar.kt` `LanguageRow`：增加 `focusable(true, interaction)`。
- `ToolbarFocusCaptureTest`：`136-compose-sidebar-language-chip-tab-focus.png`、`137-compose-sidebar-search-ghost-tab-focus.png`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（543/543）
