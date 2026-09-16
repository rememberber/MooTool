# DIFF-091：P5 工具 local-tool-shell 工作区

- 编号：DIFF-091
- 影响：F08 环境变量；F09 HTTP；F10 Host；F11 网络/IP；F25 系统信息
- 日期：2026-09-15

## 原行为（Electron）

- `.p5-tool .local-tool-shell`：`box-shadow: 0 8px 24px var(--shadow-soft)`，包住主工作区
- `variables-workspace` / `net-workspace` / `http-workspace` / `host-editor` 均在 `local-tool-shell` 内
- `hardware-workspace` 同理

## 本产品行为

- `mooToolShell(p5=true)` 已用于：
  - **Net**：整页双栏（输出 + 功能区）单壳
  - **环境变量**：Tab/搜索/表/脚栏单壳（页标题仍在外）
  - **HTTP**：集合右侧请求/响应列单壳
  - **Host**：方案列表右侧编辑列单壳
  - **系统信息**：采集结果滚动区单壳（Tab 仍在页标题）

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

翻译 P5 壳与环境作用域见 [DIFF-092](092-style-translation-shell-env-scope.md)。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
