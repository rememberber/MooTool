# DIFF-098：网络工具右侧功能区 P5 操作按钮

- 编号：DIFF-098
- 影响：F11 网络/IP
- 日期：2026-09-15

## 原行为（Electron）

- `.p5-tool .toolbar-button` / `panel-command`：网络分区内 PING、扫描、转换等约 30px 高操作按钮（输出区 ifconfig 已在 DIFF-094）

## 本产品行为

- 右侧功能区：`CommandSection` 运行钮、IPv4↔Long 转换、端口扫描、刷新 DNS、本机地址刷新均 `MooButton(p5Toolbar = true)`

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

产品窗 Tab 焦点帧、系统 IME、三平台安装仍未测。
