# ADR 001：编辑器

- 状态：采纳（领域模型）；桌面 IME/列编辑 UI 待登录会话实机验收
- 日期：2026-09-08

## 决定

JSON、随手记及其他文本工具共用 `EditorDocument`：文本、选区、矩形列选区、revision、dirty、按文档隔离的 undo/redo。查找替换移植 Electron `findReplace.ts` 语义。

UI 使用 Flutter `TextField`/`EditableText` 作为输入表面，而不是把普通无行号 TextField 当作完整编辑器。行号、等宽字体、wrap、快捷键 undo、composition 期间跳过 ⌘Z/⌘Enter 由适配层处理。矩形选区先在文档模型中实现并测试；指针手势列选择仍待接到手势层。

未采用 `re_editor` 作为第一依赖：P0 需要可测试的文档事务和跨窗口转移快照，先掌握 undo/选区所有权，再评估第三方编辑器能否导出这些状态。

## 后果

- 格式化、清空、快速替换都走 `apply`，可撤销。
- 切换文档不会串用 undo。
- 中文/日文 IME 依赖 Flutter 文本输入；CORE-05 需在真实桌面会话用输入法验收，不能只靠 widget 测试。
