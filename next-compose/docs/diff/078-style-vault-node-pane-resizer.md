# DIFF-078：Vault 树行、分栏手柄与检查器卡片表面

- 编号：DIFF-078
- 影响：F01/F04 Vault 树；JSON/随手记/HTTP/Host/图片/运行台分栏；F04 检查器卡片
- 日期：2026-09-15

## 原行为（Electron）

- `.vault-node`：最小 36px、圆角 5、悬停/选中 `--control`、13px 正文省略；空树 11px 居中 `--text-faint`
- `.pane-resizer`：10px 命中、默认透明；悬停/焦点 1px `--accent-strong` 0.5，拖动 2px 0.9
- `.inspector-card` 风格块底为 `--surface`（不是 `--surface-card`），内边距 16px

## 本产品行为

- `VaultTreeRow` 对齐上述树行与空态
- `VerticalPaneHandle`/`HorizontalPaneHandle` 10dp 命中 + 强调色中线
- `MooCard` 底改 `workspace`，内边距 16dp；smartisan 仍走 raised

## 理由

树行此前用蓝色选中底与 12sp；分栏条常显灰色底，和 Electron 默认隐藏中线不一致。

## 证据

`PaneHandleTest.resizeLineMatchesElectronPaneResizer`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
