# DIFF-024：随手记列编辑按逻辑行与 Unicode 列宽

- 编号：DIFF-024
- 影响：F01 随手记、EditorHost
- 日期：2026-09-14

## 原行为（Electron）

Quick Note 始终启用 CodeMirror `rectangularSelection` + `crosshairCursor`。按住 Alt 拖动产生多光标/矩形选区，输入对每个 range 调用 `replaceSelection`。列位置由编辑器布局决定。其它工具默认单选区。

## 本产品行为

RSTA 没有对等的矩形选区。本产品用 `ColumnEditEngine` 在**逻辑行**上按可视列插入/删除/粘贴/抽取；Tab 按 `tabSize` 展开；汉字/emoji 等按 Unicode 宽字符计 2 列，组合标记计 0 列且不与基字拆开。随手记默认启用 Alt+拖动；工具栏「列编辑」锁定后可不按 Alt 拖选。一次列操作通过 `CompoundEdit` 记为一次撤销。JSON/代码运行本轮不打开列模式。

软换行时仍按逻辑行，不按折行后的视觉行。列宽用字符网格，不用 FontMetrics 逐像素。

## 理由

架构 §5.3 要求逻辑行列编辑、短行补齐、一次操作一次撤销、不截断 surrogate。RSTA 原生选区是线性的，必须自建引擎。宽字符用 Unicode 启发式才能在无窗口单测里固定结果。

## 证据

`ColumnEditEngineTest`：短行补齐、矩形粘贴/删除、Electron `xaa\nxbb`、Tab、emoji/组合标记、汉字整字删除。`EditorBufferColumnEditTest`：列替换一次撤销。`desktopTest` **136/136**。无运行截图；IME 与真实 Alt 拖动手势未在窗口验收。

## 受影响范围

- 与 CodeMirror 像素列、软换行视觉列可能不一致。
- 等宽字体下 CJK 若未占两格，高亮条可能与字形不完全重合。
- JSON/代码运行、拖入编辑器、frontmatter、外部冲突仍未做。pull/push 见 [DIFF-025](025-git-cli-local-checkpoint.md)。
