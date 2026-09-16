# DIFF-064：IME 预编辑让路快捷键，首页链接可 Tab 聚焦

- 编号：DIFF-064
- 影响：A01 快捷键路由；F00 首页键盘；HTTP/运行台/JSON/随手记等 Enter 与 Meta 快捷键
- 日期：2026-09-15

## 原行为（Electron）

输入法预编辑时 Enter 提交候选，不发送 HTTP、不运行代码、不关闭弹层。Escape 先取消预编辑。首页链接可键盘到达。

## 本产品行为

- `ImeShortcutGate` 监听 AWT `InputMethodEvent`：预编辑中以及提交后 120ms 内，工具/窗口快捷键不消费按键。
- HTTP Cmd/Ctrl+Enter、运行台运行、JSON/随手记保存与格式化、命令盘、Vault 树、弹层 Escape、留言板退出等均让路。
- 首页 logo、站点、贡献者与外链使用与工具栏一致的 2dp 外描边焦点环，可 Tab 到达。

## 理由

规格要求「IME 预编辑 > 当前编辑器 > 当前工具」。页面级 `onPreviewKeyEvent` 会在预编辑提交的 Enter 之前先发请求。首页此前只有鼠标 clickable，没有可见焦点环。

## 证据

`ImeShortcutGateTest`：预编辑阻断、提交后短暂抑制。`desktopTest` **248/248**。系统 IME 产品窗口手势仍未手工验收。

## 未做

- 产品窗系统 IME 预编辑截图仍缺。
- 首页以外其余页面 Tab 走查仍缺。
