# DIFF-089：本机地址编辑器、Net/UA 最小列、HTTP Body 控制条

- 编号：DIFF-089
- 影响：F11 网络/IP；F08 环境变量编辑；F12 UA；F10 Host 名称/系统 hosts；F09 HTTP Body
- 日期：2026-09-15

## 原行为（Electron）

- `.local-address-editor`：最小 72、10 字号、7 垫、等宽
- `.net-workspace`：`minmax(360px, 1.15fr) minmax(320px, 0.85fr)`
- `.environment-editor textarea`：最小 120、11 等宽、9/10 垫
- `.ua-workspace`：两列均 `minmax(360px, …)`；结果区 22 垫
- `.host-name`：`min(300px, 36%)`、最小 130
- `.system-hosts-meta` 10sp；`.system-hosts-view` 11 等宽、12 垫
- `.http-body-editor`：37 控制条 + 8 垫；MIME/格式化在控制条

## 本产品行为

- `MooTextField(code=true)`：10 字号、7 垫、圆角 6、不强制 120 最小高；`mono=true` 等宽
- 本机 IPv4/IPv6 使用 `code`；环境值使用 `dense`+`mono` 多行
- Net 输出/功能列 `widthIn(min=360/320)`；UA 两列 `min=360`，结果 1.1 权重与 22 垫
- Host 名称 130–300；系统 hosts 元信息 10sp、正文 11 等宽
- HTTP Body MIME+格式化改到 37 高控制条，编辑器 8 垫；格式化仍真实执行

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

`.p5-tool .local-tool-shell` 边影、Host 方案正文 12/16–18 编辑垫、响应预览 11 等宽仍待。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
