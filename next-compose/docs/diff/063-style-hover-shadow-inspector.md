# DIFF-063：modern 风格 chrome 对齐风格块，补 hover/shadow 并用于卡片与检查器

- 编号：DIFF-063
- 影响：A01 外观；F00 首页卡片；F04 JSON 检查器卡片；弹层阴影
- 日期：2026-09-15

## 原行为（Electron）

`data-interface-style='modern'` 覆盖 `:root`：`--surface-card: #f4f4f5`、`--border-control: #dcdce0`、`--border-soft: #ececef`。各风格还有 `--surface-card-hover`、`--control-hover`、`--control-active`、`--border-control-hover`、`--shadow`、`--shadow-soft`。按钮悬停改边与底，卡片/设置分组/命令盘有阴影。JSON 检查器分区用 `.inspector-card`。quiet 没有独立色板块，沿用 `:root`。

## 本产品行为

- modern 的 chrome 改走风格块，不再误用 `:root` 的 `#F2F2F3` / `#E1E1E3`。quiet 仍用 `:root`。
- `MooColors` 增加 hover/pressed/shadow token；次级按钮悬停/按下用 `compositeOver` 合成半透明 `--control-hover`。
- 首页卡片、设置分组、命令盘、JSON 转换结果/输入/路径弹层使用对应阴影色；JSON 检查器格式/转换/JSONPath 分区改为 `MooCard`。

## 理由

DIFF-062 把 modern 对齐到未选风格时的 `:root`，和设置里默认的 modern 不一致。缺 hover/shadow 时，六套风格只换了静止边色。

## 证据

`ThemeContrastTest.modernChromeTokensMatchElectronCss`、`heroAndMiuiTokensFollowElectronCssVariables`、`hoverAndPressedFillsCompositeTranslucentControlTokens`。`desktopTest` **245/245**。

## 未做

- 仍不是六套 CSS 的逐选择器移植（分段 inset、桌面 vibrancy、大量工具页选择器未搬）。
- 系统 IME 预编辑、托盘 TCC、三平台安装仍未测。
