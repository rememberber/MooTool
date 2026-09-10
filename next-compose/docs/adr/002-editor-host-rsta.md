# ADR-002：编辑器宿主使用 RSyntaxTextArea

- 状态：已采用（P0 实验进行中）
- 日期：2026-09-09

## 背景

规格要求 EditorHost 支持语法高亮、undo、查找替换、分离/收回。首选实验是 RSyntaxTextArea + SwingPanel。

## 决定

- 文档模型为 `EditorBuffer`，持有 RSyntaxTextArea、Document 与 UndoManager。
- Compose `SwingPanel` 只托管已有 `RTextScrollPane`，不因重组重建文档。
- 格式化/替换通过 `beginAtomicEdit`/`endAtomicEdit` 记为一次 undo。
- 命令搜索与对话框使用独立 Compose Dialog/Window，避免被 Swing 编辑器遮挡。

## 尚未用真实桌面交互验证

中文预编辑、矩形列编辑、5 MiB 文本、DPI 与分离/收回后的 undo。这些保持待验收，不以编译通过代替 T05/T07。
