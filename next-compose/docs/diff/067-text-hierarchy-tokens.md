# DIFF-067：六套风格补齐 Electron 文本层级 token

- 编号：DIFF-067
- 影响：A01 外观；F00 首页与工具页标题
- 日期：2026-09-15

## 原行为（Electron）

各风格 CSS 除 `--text` / `--text-muted` 外还有 `--text-strong`、`--text-body`。`.tool-page h1` / `.section-title` 用 strong，正文用 body，分组标题用 muted。

## 本产品行为

`MooColors` 增加 `textStrong` / `textBody` / `textMuted`，数值对齐 Electron 六套风格变量。首页标题/分区用 strong，介绍用 body，副文案与侧栏分组用 muted；工具页 16sp 标题改 strong。

## 理由

此前只有 primary/secondary，标题和正文挤在同一色阶，和 Electron 层级不一致。

## 证据

`ThemeContrastTest.modernChromeTokensMatchElectronCss` 与 `heroAndMiuiTokensFollowElectronCssVariables`。仍不是逐选择器 CSS 移植。

## 未做

- 其余选择器皮肤、系统 IME、托盘 TCC、三平台安装仍未测
