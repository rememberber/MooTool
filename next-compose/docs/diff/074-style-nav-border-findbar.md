# DIFF-074：导航选中/悬停边、图标色与查找条

- 编号：DIFF-074
- 影响：A01 外观（侧栏、设置导航、命令盘结果、JSON/随手记/HTTP/Host 查找条）
- 日期：2026-09-15

## 原行为（Electron）

- modern `.tool-button` 选中无边；smartisan 选中 `border-control` + 左红条；miui 选中白底 + `border` + 左橙条，悬停 `surface-card`；claude 选中暖底 + accent 16% 边
- 选中图标：smartisan 红、miui 橙、claude accent
- `.json-findbar` 底为 `--surface`；通用 `.find-replace-bar` 为 `--surface-soft` 并有底边

## 本产品行为

- `navItemBorder` / `navSelectedIcon` 接到侧栏、设置导航、命令盘结果
- miui/claude 悬停填色不再一律 `control`
- modern 选中/悬停不再画通用 `border`
- JSON 查找条用 workspace + 底边；其余查找条 `surfaceSubtle` + 底边

## 理由

此前所有风格导航悬停都画 `border`，modern 多出一圈；选中图标走正文色而不是风格强调色。

## 证据

`ThemeContrastTest.navItemChromeFollowsElectronStyleBlocks`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、三平台安装仍未测。
