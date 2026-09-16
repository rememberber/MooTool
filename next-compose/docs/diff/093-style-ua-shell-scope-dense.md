# DIFF-093：UA 工作区壳、列最小 300、环境作用域 dense 按钮

- 编号：DIFF-093
- 影响：F12 UA；F08 环境变量；桌面控件
- 日期：2026-09-15

## 原行为（Electron）

- `.ua-workspace` 在 `local-tool-shell` 内，双列 `minPaneWidths` 300
- `.environment-scope select`：高 26、11 字号量级

## 本产品行为

- UA 输入/结果双列外包 `mooToolShell(p5=true)`；列 `widthIn(min = 300.dp)`
- `MooButton(dense=true)`：26 高、8/3 垫、11sp；环境作用域触发器使用

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

UA 分栏拖拽持久化仍用 Compose 权重非 ResizableColumns。六套 CSS 逐选择器、系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
