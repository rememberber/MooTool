# DIFF-057：命令盘/分组弹层改为应用内遮罩

- 编号：DIFF-057
- 影响：A02 命令搜索；A01 分组管理器；对照 Electron `command-palette-backdrop` / `.dialog`
- 日期：2026-09-15

## 原行为（Electron）

搜索与分组管理是应用壳内的 fixed 遮罩：`rgba(0,0,0,.2)`（深色/hero 更深），面板 `surface-elevated` 不透明，点遮罩关闭。不是独立透明系统窗。

## 本产品行为

- `MooOverlay` 画在 Workbench 内：`overlayScrim()` 浅色 0.20 / 深色 0.46，点遮罩关闭。
- 命令盘靠上（约 96dp），分组管理器居中；面板用 `workspace` 实底。
- 删除确认仍是第二层遮罩，不关管理器。
- 不再使用 Compose `Dialog` 弹出独立半透明窗（主窗截图曾透出首页）。

## 理由

Electron 弹层属于壳，不离开主窗口。系统 `Dialog` 在 macOS 上半透明，截到的命令盘/分组与首页叠在一起，也不符合点遮罩关闭。

## 证据

`ThemeContrastTest` 校验各风格 scrim alpha。`desktopTest` **240/240**。主窗重拍见 `72-command-palette.png` / `73-group-manager.png`。

## 受影响范围

- 仍非六套 CSS 逐选择器皮肤。
- 工具内 `Dialog` 已由 [DIFF-059](059-tool-dialogs-to-overlay.md) 改为 overlay。
- 产品主窗 Tab/IME 手工、托盘 TCC、三平台安装未测。
