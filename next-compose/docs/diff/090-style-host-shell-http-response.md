# DIFF-090：P5 输出壳边影、Host 正文、HTTP 响应 11sp

- 编号：DIFF-090
- 影响：F11 网络/IP；F10 Host；F09 HTTP 响应；F08 环境编辑
- 日期：2026-09-15

## 原行为（Electron）

- `.p5-tool .local-tool-shell`：`box-shadow: 0 8px 24px var(--shadow-soft)`
- `.host-editor > .host-content-editor`：12 字号、1.65 行高、16/18 垫、无边框
- `.http-response-pane pre` / `.http-body-editor textarea`：11 等宽、11/13 垫
- `.environment-editor textarea`：最小 120、11 等宽

## 本产品行为

- `mooToolShell(p5=true)`：8dp 边影，Net 输出列使用
- `MooTextField(hostsContent=true)`：12 等宽、16/18 垫、无边框；Host 方案正文 `mooEditorFrame(flatten)`
- HTTP 响应 `EditorHost` 固定 11 字号；空态占位 11sp
- 环境编辑值多行 `dense`+`mono`，最小高 120

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

其余 P5 面板未全部套 `p5` 壳；Host 未换 RSTA。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
