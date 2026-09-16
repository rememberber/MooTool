# DIFF-077：统一复制 toast 与首页/计算器卡片圆角

- 编号：DIFF-077
- 影响：F01–F25 复制反馈；F00 首页分区；F21 计算器面板
- 日期：2026-09-15

## 原行为（Electron）

`useToolActions.copy` 统一写入剪贴板并 `toast.success/error`。列编辑内部复制不弹通知。首页分区卡片与计算器 log 用 inspector-card 半径。

## 本产品行为

- `AppContainer.copyText` 统一文本复制与 toast；JSON/HTTP/随手记/Host/环境/翻译/正则/Cron/对比/编码以外其余文本复制入口均接入
- 图片/二维码剪贴板仍走 Transferable，成功/失败同样 toast
- 列编辑内部复制不 toast
- 首页分区与计算器面板改 `cardRadius`

## 理由

DIFF-076 只接到部分工具，其余复制仍只有状态栏/按钮文案。

## 证据

`ToastQueueTest` 仍覆盖队列。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
