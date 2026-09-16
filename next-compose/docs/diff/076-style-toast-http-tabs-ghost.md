# DIFF-076：Toast 闭环、HTTP Tab 与 icon-ghost

- 编号：DIFF-076
- 影响：A01 外观（toast / icon-ghost）；F04 JSON 保存/复制；F01 随手记保存；F09 HTTP 复制与请求/响应 Tab；F22 调色板复制；F25 系统信息复制；A03 备份导出/恢复
- 日期：2026-09-15

## 原行为（Electron）

- `ToastProvider`：右下角最多 4 条，默认 3200ms，成功/错误/信息，可关闭
- 复制、Vault 保存、备份成功走 toast，不只写状态栏
- HTTP Params/Headers/Cookies/Body 与响应 Tab 是 `.tool-tabs` 下划线，不是填充色块
- 侧栏折叠/搜索/分组为 `.icon-ghost` 30×30 透明按钮

## 本产品行为

- `ToastQueue` + `MooToastHost`：主窗与分离窗仅在获得焦点时显示；复制/保存/备份接到真实 toast
- HTTP 请求/响应 Tab 改 `MooToolTab`；集合条目 on/off 仍用 `TabChip`
- 侧栏动作改 `MooGhostButton`

## 理由

复制成功此前只写状态栏/按钮文案，缺少 Electron 的应用内通知层。

## 证据

`ToastQueueTest`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
