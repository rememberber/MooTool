# DIFF-497：经典导航壳、p5 紧凑工具栏、设置 Tab/快捷键、代码运行查找

## 背景

DIFF-496 补齐侧栏紧凑导航与分组分隔线后，仍缺：`navigationStyle=classic` 时内置分组标题隐藏（Electron `app-shell--nav-classic`）、紧凑导航下工具页 `p5Toolbar` 密度、设置页左侧分类键盘/快捷键切换、F05 源码编辑器 `Cmd/Ctrl+F`/`R` 查找（`EditorHost` 已绑定但无 `onFind`）。

## 行为

- **P1 / A01 布局**：`LayoutPolicy.showNavigationGroupLabel` / `navigationGroupTopPaddingDp(navigationStyle, compactNavigation)`；`Sidebar` 分组顶距与 classic 仅保留自定义分组标题。
- **P1 紧凑导航 → 工具栏**：`MooTheme(compactNavigation)` + `LocalCompactNavigation`；`MooButton(p5Toolbar=true)` 在紧凑导航时走 26dp dense 尺寸（DIFF-496 未做项）。
- **A01 设置**：`Meta+,` 等设置快捷键改为切换开/关（对齐 Esc 关闭）；设置左侧导航 `↑/↓` 切换分类并写回 `settingsNavCategoryId`（`settingsNavCategoryStep`）。
- **F05 代码运行**：会话 `findOpen`/`findQuery`/`findOptions`；壳层与 RSTA `EditorAppShortcuts.onFind` 打开 `EditorFindOnlyBar` + `EditorFindHighlight`。

## 验证

- `LayoutPolicyTest.classicNavigationHidesBuiltinGroupLabels`
- `SettingsNavCategoryTest.arrowStepMovesWithinCategories`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）
- **2026-09-17**：`./scripts/check-core.sh` 全绿（约 7.5 min，commit `06fafbfe`）

## 未做

六套 CSS 逐选择器皮肤、产品窗走查帧、其他仍用 `MooTextField` 的工具内查找、分离窗 `MooTheme` 独立 compact 传参（若后续拆窗主题需对齐主窗设置）。
