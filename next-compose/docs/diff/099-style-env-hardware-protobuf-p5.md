# DIFF-099：环境/硬件/Protobuf P5 操作按钮

- 编号：DIFF-099
- 影响：F08 环境变量；F25 系统信息；F07 Protobuf
- 日期：2026-09-15

## 原行为（Electron）

- P5 工具区内 `panel-command` / `.p5-tool .toolbar-button` 约 30px 高（环境新增/刷新、硬件刷新、Protobuf 格式化/转换/解码等）

## 本产品行为

- 环境：标题栏「新增」「刷新」`p5Toolbar`（作用域仍 `dense`）
- 硬件：标题栏「刷新」`p5Toolbar`
- Protobuf：JSON/Wire/Convert 各 Tab 内全部 `MooButton` 加 `p5Toolbar`

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

产品窗 Tab 焦点帧、系统 IME、三平台安装仍未测。
