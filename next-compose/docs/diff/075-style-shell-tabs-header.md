# DIFF-075：工具壳圆角、工具 Tab 条与设置标题

- 编号：DIFF-075
- 影响：A01 外观（editor-shell/vault-panel、tool-tabs、settings-content__header）；F02/F05/F06/F14/F16/F17/F21/F24/F25 等工具 Tab
- 日期：2026-09-15

## 原行为（Electron）

- `.editor-shell` / `.vault-panel` 圆角与阴影按风格：modern 8/5、hero 14/8、smartisan 10/7、miui-v5 5/5、claude 12/6
- `.inspector-card` 圆角：modern 8、hero 12、smartisan 8、miui 4、claude 12
- `.tool-tabs` 底为 toolbar 渐变 + 底边；`.tool-tab--active` 为强调色下划线，不是填充主按钮
- `.settings-content__header h1`：modern 18/650，hero 700，smartisan 650+高光字影，miui 600，claude 560
- `.settings-group__rows` 圆角与壳一致（modern 8、hero 14、smartisan 10、miui 5、claude 12）

## 本产品行为

- `MooDimens.shellRadius` / `shellElevation` / `cardRadius` 接到 `mooToolShell`、`mooEditorFrame` 阴影（仍不 clip Swing）、`MooCard`、`SettingsGroup`
- 工具页 Tab 改 `MooToolTab` + `mooToolTabsBackground`；「更多」/闩锁/范围筛选仍走 `MooButton(primary = …)`，未改 prominent
- 设置与工具标题字重按风格；smartisan 标题加 1dp 高光字影

## 理由

此前壳圆角走控件半径，Tab 被画成填充按钮，和 Electron 工具页分层不一致。

## 证据

`ThemeContrastTest.chromeRadiiMatchElectronStyleBlocks`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、三平台安装仍未测。产品窗 Tab 焦点帧仍待拍。
