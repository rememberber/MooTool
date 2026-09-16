# DIFF-061：列编辑获焦、工具栏底边、Electron 式焦点外描边

- 编号：DIFF-061
- 影响：F01/F04 列编辑键盘写入；ui-spec §8 焦点环；工具栏 chrome
- 日期：2026-09-15

## 原行为（Electron）

`:focus-visible` 使用 `outline: 2px solid` 且 `outline-offset: 2px`，不吃掉控件自身边线。工具栏与工作区之间有细分隔。列选后键盘输入写入矩形选区。

## 本产品行为

- 列编辑 `mousePressed`/`mouseReleased` 在 `consume()` 之后调用 `requestFocusInWindow()`，避免 Compose 仍占键盘焦点导致按键进不了 Swing。
- `MooButton`/`MooTextField`/`MooSwitch`/侧栏项增加 `mooFocusOutline`：2dp 外描边 + 2dp 间距，对齐 Electron outline-offset；控件仍保留原 1/2dp 边。
- 各工具页工具栏由 `toolbarBrush()` 改为 `mooToolbarBackground()`，画出 `borderSoft` 底边。

## 理由

产品主窗 Alt 拖选已能画出列高亮，但按键落到 Vault 搜索框。内描边替换边线时 Tab 环几乎看不见。工具栏底边此前只铺了壳标题栏。

## 证据

`desktopTest` **244/244**。列选写入单测：`shownComponentColumnTypeWritesEveryLineAndRequestsFocus`。产品主窗列选写入见 `122-json-column-type.png`（ASCII `x`，不是系统 IME）。JSON「复制」外描边见 `123-json-copy-focus.png`。

## 未做

- 不是 Electron 六套 CSS 逐选择器移植。
- 系统 IME 预编辑窗口手势、托盘 TCC、三平台安装仍未测。
