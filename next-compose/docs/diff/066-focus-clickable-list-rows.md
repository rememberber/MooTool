# DIFF-066：列表行/芯片/主题块可 Tab 焦点环

- 编号：DIFF-066
- 影响：A01 键盘焦点；F09/F10/F20/F08/F15/F16/F17/F22/F23/F19/F24/F04
- 日期：2026-09-15

## 原行为（Electron）

`button/select/input/[role=button]` 的 `:focus-visible` 使用 `outline` + `outline-offset: 2px`。集合项、方案、单词本、常用正则、主题色块等可键盘到达。

## 本产品行为

`Modifier.mooFocusClickable` 给列表行、HTTP TabChip、Host 方案、翻译语言/历史、环境变量行、正则/Cron 收藏、二维码历史、调色板、图片库、留言板主题、Git 提交、PDF 输出路径加上 2dp 外描边并可 Tab。

## 理由

这些节点此前只有鼠标 `clickable`，Tab 走查到不了，焦点环规格无法覆盖。

## 证据

实现与 `desktopTest`。产品窗逐页 Tab 帧仍待拍。

## 未做

- 首页以外产品窗 Tab 走查帧仍缺
- 系统 IME、托盘 TCC、六套 CSS 逐选择器、三平台安装仍未测
