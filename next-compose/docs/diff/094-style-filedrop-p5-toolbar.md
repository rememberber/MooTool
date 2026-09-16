# DIFF-094：file-drop 行组件与 P5 工具栏按钮

- 编号：DIFF-094
- 影响：F03 格式化；F20 翻译；F11 网络；`MooButton`；`EditorBufferColumnEditTest`
- 日期：2026-09-15

## 原行为（Electron）

- `.file-drop-row`：`flex`、10px 缝、11px muted 文件名省略
- `.p5-tool .toolbar-button`：30px 高、9px 水平内边距、6px 圆角、12px 字号（翻译/网络内嵌工具栏）

## 本产品行为

- 新增 `FileDropRow`；格式化文件 Tab 改用该组件
- `MooButton(p5Toolbar = true)`：30dp 高、9/4 内边距、6dp 圆角、12sp
- 翻译语言/交换/提供商/自动/立即、Net 输出区 ifconfig/netstat/停止 使用 `p5Toolbar`
- 列编辑键入单测在派发 `KEY_TYPED` 前重试 `requestFocusInWindow`，去掉对 `requestFocusInWindow()` 返回值的断言

`primary = expr` / `prominent` 语义未改。

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

file-drop 拖放区、翻译工具栏 30px 图标按钮；Host `p5Toolbar` 见 [DIFF-095](095-style-host-ua-pane-qrcode-filedrop.md)。系统 IME、产品窗 Tab 帧、三平台安装仍未测。
