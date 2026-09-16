# DIFF-073：菜单主题色、触觉按钮与命令盘搜索条

- 编号：DIFF-073
- 影响：A01 外观（下拉/右键菜单、smartisan/miui 按钮、命令盘、弹层底）
- 日期：2026-09-15

## 原行为（Electron）

- `.quick-note-tree-menu` / `.vault-more-menu` 使用 `--surface-elevated` 与 10px 圆角
- smartisan/miui `.toolbar-button` / `.icon-button` 为 raised 渐变，按下反转或 inset，悬停不改成扁平 hover 底
- `.command-palette__search` 在 smartisan/miui 走 toolbar 渐变
- `.dialog` 底为 `--surface` / elevated，边为 `--border-control`（smartisan/miui）

## 本产品行为

- `MooTheme` 包一层 Material `colors`，surface/onSurface 接到 workspace/textPrimary，所有 `DropdownMenu` 跟随当前风格
- smartisan/miui `MooButton`/`MooIconButton` 悬停仍用 `controlBrush`；按下 smartisan 反转渐变、miui 改 inset
- 命令盘搜索行在 smartisan/miui 铺 `toolbarBrush`
- `mooDialogSurface` 底改为 workspace，smartisan/miui 边用 `borderControl`

## 理由

此前菜单走 Material 默认紫/灰；触觉风格一悬停就被扁平 `hoveredControlFill` 盖掉；弹层底偏 `surfaceCard` 灰块。

## 证据

`ThemeContrastTest.tactileStylesExposeRaisedChromeForButtons`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、三平台安装仍未测。
