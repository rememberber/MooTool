# DIFF-060：退出确认为应用内遮罩，chrome token 对齐 Electron

- 编号：DIFF-060
- 影响：A01 关闭行为；P1 主窗 overlay；A01 外观 chrome（按钮/输入/侧栏/卡片边）
- 日期：2026-09-15

## 原行为（Electron）

关闭主窗且设置为「每次询问」时，`dialog.showMessageBox` 文案为「关闭窗口后如何处理 MooTool？」，按钮顺序 hide / quit / cancel（默认 hide）。侧栏右边线用 `--border-soft`，按钮与输入用 `--border-control`，卡片用 `--surface-card`。

## 本产品行为

- 退出确认不再开 `DialogWindow`。遮罩画在主窗 `Window` 内，走 `MooOverlay` + `ModalOverlayState`，打开时卸下 `SwingPanel`。
- 文案与按钮顺序对齐 Electron：隐藏到后台（默认主按钮）/ 退出 MooTool / 取消；Esc 与点遮罩取消。
- `MooColors` 增加 `borderSoft` / `borderControl` / `surfaceCard`，modern 明暗值取自 Electron `global.css`；非 modern 由 `border`/`workspace` lerp 派生。
- 侧栏画 1dp 右边线；按钮、输入、分段、开关未选中边用 `borderControl`；首页卡片、设置分组、弹层表面用 `surfaceCard` + `borderSoft`。

## 理由

系统 `DialogWindow` 是独立半透明窗，截图透底、点击与 Swing 编辑器抢层。chrome 边色此前混用结构 `border`，侧栏 `border(0.dp)` 实际无分隔线。

## 证据

`desktopTest` **243/243**。关闭 overlay 产品窗截图若已拍见 `docs/evidence/2026-09-15-inspector-screencapture/windows/`。

## 未做

- 不是 Electron 六套 CSS 的逐选择器移植。
- 产品主窗 JSON Alt 拖选跨行矩形已拍（`115-json-column-edit-product.png`）；列选写入、系统 IME、托盘 TCC、三平台安装仍未测。
- 系统信息在切走页面时仍会取消采集；960 帧可能赶上空表，全尺寸数据见 `60-hardware.png`。
