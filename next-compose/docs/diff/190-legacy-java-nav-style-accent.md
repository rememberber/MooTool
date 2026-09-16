# DIFF-190：Java 侧栏样式与强调色迁入

## 背景

Java `setting.custom` 用 `funcTabGrouped` / `tabCard` 控制功能 Tab 样式（分组 / 卡片 / 经典）。Compose 对应 `layout.navigationStyle`（`grouped` / `card` / `classic`）。Java `setting.quickNote.accentColor` 存 `Moo.accent.*` 键名，Compose 使用 `appearance.accentColor`（`yellow`/`coral`/`blue`/`green`/`red`/`purple`）。

## 行为

- `applyPatch`：与 Java `isFuncTabGrouped` / `isTabCard` 一致——`funcTabGrouped=true` 优先为 `grouped`；否则 `tabCard=true` 为 `card`；否则 `classic`（仅在配置中存在对应键时写入）。
- `accentColor` 经 `legacyAccentColor` 映射到 Compose 预设 id。

## 未覆盖

- `menuBarPosition` / `funcTabPosition`（Java 顶栏/侧栏 Tab 位）；Compose 壳固定侧栏布局，无等价设置项。

## 证据

- `LegacyJavaSettingsTest.applyPatchMapsNavigationStyleAndAccentColor`
