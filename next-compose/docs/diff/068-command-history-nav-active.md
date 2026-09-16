# DIFF-068：命令盘/历史/字体/自定义分组可 Tab；选中导航对齐 tool-button--active

- 编号：DIFF-068
- 影响：A01/A02 键盘与外观
- 日期：2026-09-15

## 原行为（Electron）

命令盘结果、历史项、字体列表、自定义分组可键盘到达。选中 `.tool-button--active` / `.settings-nav__item--active` 用 `--text-strong` 与字重 600；分组标题用 `--text-muted`。

## 本产品行为

- 命令盘关闭/结果、历史恢复、字体条目、自定义分组行使用 `mooFocusClickable`
- 侧栏选中项与设置导航选中项用 `textStrong` + SemiBold
- 设置分组标题用 `textMuted`

Vault 树仍走容器焦点 + 方向键，避免每个节点进入 Tab 环。

## 理由

这些 overlay/列表此前只能鼠标点。选中导航色阶与 Electron active 规则不一致。

## 证据

实现与 `desktopTest`。产品窗命令盘 Tab 帧待拍。仍不是逐选择器 CSS 移植。

## 未做

- 系统 IME、托盘 TCC、三平台安装仍未测
