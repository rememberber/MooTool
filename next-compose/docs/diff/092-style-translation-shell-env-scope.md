# DIFF-092：翻译 P5 壳、编辑列最小宽、环境作用域下拉

- 编号：DIFF-092
- 影响：F20 翻译；F08 环境变量
- 日期：2026-09-15

## 原行为（Electron）

- `.translation-workspace` 在 `local-tool-shell` 内，含 Tab + 主内容
- `.translation-editor-grid` 双列，分栏最小约 280
- `.environment-scope select`：26px 高、10sp 标签，非分段按钮

## 本产品行为

- 翻译 Tab + 三页内容套 `mooToolShell(p5=true)`（页标题仍在外）
- 源/目标编辑列 `widthIn(min = 280.dp)`
- 环境 Tab 下作用域改为 10sp 标签 + 下拉按钮（`MooMenu`），不再用 `primary` 分段按钮；进程编辑弹层内 `targetScope` 仍 `primary = expr`

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

环境作用域 26 dense 见 [DIFF-093](093-style-ua-shell-scope-dense.md)。六套 CSS 逐选择器、系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
