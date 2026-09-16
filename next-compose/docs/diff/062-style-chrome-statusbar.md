# DIFF-062：非 modern 风格 chrome token 对齐 Electron CSS，工具状态栏顶边

- 编号：DIFF-062
- 影响：A01 外观；ui-spec chrome token；各工具页状态栏
- 日期：2026-09-15

## 原行为（Electron）

`hero` / `smartisan` / `miui-v5` / `claude` 各自在 `global.css` 覆盖 `--border-soft` / `--border-control` / `--surface-card`。工具页状态栏顶边用细分隔。quiet 没有独立色板块，沿用 `:root`。

## 本产品行为

- 上述四套风格的 `borderSoft` / `borderControl` / `surfaceCard` 取 Electron 对应 CSS 变量，不再对 `border`/`workspace` lerp。
- modern / quiet 仍用 `:root` 的 `#ECECEF` / `#E1E1E3` / `#F2F2F3`（及暗色对应值），与 DIFF-060 一致。
- 工具页状态栏改为 `mooStatusBarBackground()`，画出 `borderSoft` 顶边，与主窗 `StatusBar` 一致。

## 理由

非 modern 卡片和按钮边此前是近似色，和设置里可选的六套风格不一致。工具状态栏缺顶边，工具栏已有底边后层级不对称。

## 证据

`ThemeContrastTest.heroAndMiuiTokensFollowElectronCssVariables` 增加 hero/miui/claude/smartisan chrome 断言。`desktopTest` **244/244**。仍不是六套 CSS 的逐选择器移植。

## 未做

- Electron 各风格下大量选择器（阴影、分段、对话框细节）未逐条移植。
- 系统 IME 预编辑、托盘 TCC、三平台安装仍未测。
