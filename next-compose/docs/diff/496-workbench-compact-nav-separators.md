# DIFF-496：工作台紧凑导航密度与分组分隔线

## 背景

Electron `Workbench` 在 `layout.compactNavigation` 时为壳层加上 `app-shell--compact-nav`：侧栏 `.tool-button` / `.recent-item` 最小高度 30px、字号 `calc(var(--app-font-size) - 1px)`，分组 `margin-top: 12px`。`layout.showSeparators` 对应 `app-shell--nav-separators`，为 `.tool-group` 绘制底部分隔线并 `padding-bottom: 12px`。Compose 此前仅缩小 `NavItem` 纵向 padding，开关不改变行高/字号，分组也无分隔线，与设置页「紧凑导航」「显示分组分隔线」不一致。

## 行为

- `LayoutPolicy.navigationItemFontSp` / `navigationItemMinHeightDp` / `navigationGroupTopPaddingDp` / `navigationItemVerticalPaddingDp` 集中对照 Electron 数值。
- `Sidebar`：`NavItem` 应用最小高度与 12/13sp；紧凑模式下分组顶距 12dp；`showSeparators` 时分组底 `borderSoft` 线 + 12dp 间距；侧栏底部「设置」按钮 `dense` 随紧凑导航。
- `NavigationToolGroup` 包裹最近使用、自定义分组与内置分组块。

## 验证

- `LayoutPolicyTest.compactNavigationDensityMatchesElectronShell`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

工具页 `p5Toolbar` 全局随紧凑导航收缩、六套 CSS 逐选择器皮肤、产品窗走查帧。
