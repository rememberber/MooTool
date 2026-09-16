# DIFF-088：Net/环境/硬件 P5 输入密度与标题栏

- 编号：DIFF-088
- 影响：F11 网络/IP；F08 环境变量；F25 系统信息；F12 UA；F10 Host 查找
- 日期：2026-09-15

## 原行为（Electron）

- `.p5-tool input/select`：高 30、11 字号、圆角 6
- `.net-command-row` / `.net-port-scan-row`：dense 输入；端口列 `0.8fr` / `1.2fr`
- `.net-section label`：9sp
- `.variables-workspace > header`：Tab 进工具栏；`.compact-search` 宽 `min(260px, 38%)`
- `.variables-workspace > footer`：最小 30、8/11 垫、10sp
- `.hardware-workspace > header`：Tab 与刷新同条；时间戳 10sp
- `.ua-input-panel`：0.9 列、11/600 标签、中缝 `border-soft`
- `.host-find` 查找/替换走 p5 输入

## 本产品行为

- Net `CommandSection` / `LabeledField` / 端口扫描使用 `MooTextField(dense = true)`
- 环境 Tab/作用域与搜索同条；搜索 `widthIn(140–260)`；表在 `Box(weight=1)`；脚 30/10sp
- 环境编辑键 dense；作用域仍 `primary = session.scope == item` / `primary = session.targetScope == …`
- 硬件 Tab 并入标题工具栏；时间戳与序列号勾选 10sp
- UA 输入 `weight(0.9)` + `borderless`；结果列 1dp 中缝
- Host 查找/替换 dense

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

`.local-address-editor` 10sp/7 垫、Net 分栏 `minmax(360/320)`、环境值等宽 textarea、HTTP Body 37 控制条仍待。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
