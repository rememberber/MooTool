# DIFF-054：导航/设置焦点环与真实聚焦像素

- 编号：DIFF-054
- 影响：A01 键盘焦点；ui-spec §8 焦点环不因鼠标样式消失
- 日期：2026-09-15

## 原行为（Electron）

侧栏工具行、分离按钮、语言切换、设置分类在 `:focus-visible` 时有描边。Compose 此前只有按钮/开关/分段/色板画环，导航行本身不可键盘露出焦点。

## 本产品行为

- 侧栏 `NavItem`、分离/收回图标、语言「中 / EN / 日」、设置左栏分类使用同一 `focusRing` 描边，并带 `Role`/`selected`。
- `MooIconButton` 补 `focusable`。
- `CompactShellCaptureTest` 对真实 `MooButton` 调用 `requestFocus()` 后采样 `#316DC0` 像素，不再在工作区画假边框冒充焦点环。

## 理由

ui-spec 要求交互节点可键盘到达且焦点环可见。导航行原先只有 `clickable`，键盘 Tab 走不到描边。

## 证据

`desktopTest` **236/236**。Compose 场景见 `docs/evidence/2026-09-15-tray-density/captures/compact-960-focus-ring.png`。列选择后的可见 `JFrame` 见 `docs/evidence/2026-09-15-inspector-screencapture/windows/70-column-edit-jframe.png`（测试窗标题 `next-compose-column-edit`，派发鼠标/IME 事件，**不是**产品主窗手工手势）。

## 受影响范围

- 产品主窗 Tab 走查仍未拍。
- 仍非 Electron 六套 CSS 逐选择器皮肤。
- 系统输入法预编辑窗口手势未手工验收。
