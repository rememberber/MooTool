# DIFF-036：随手记颜色、行距与列表前缀

- 编号：DIFF-036
- 影响：F01 随手记工具栏与文件树
- 日期：2026-09-15

## 原行为（Electron）

工具栏有笔记颜色、行距倍数、无序/有序列表。颜色写入 frontmatter 并给文件树图标着色。行距按 `1.65 * lineSpacing` 作用到编辑器。列表前缀覆盖选区所在整行，编号从 1 起。

## 本产品行为

- 颜色选择写入当前笔记 `metadata.color`，保存进 YAML；文件树对非 default 显示色点并着色文件名；未保存时当前文件仍按会话色显示。
- 行距 1.0–2.0 写入 metadata；`EditorHost` 在 RSTA 计算行高之后按倍数抬高 `lineHeight`（1.0 保持默认）。
- 「无序列表 / 有序列表」按 Electron 规则给选区整行加 `- ` 或 `1. `，进入撤销栈。

## 理由

feature-parity 要求字体/语法之外还有列表动作与文档元数据（颜色、行距）。这些必须改变编辑器、树或正文，不能只做开关。

## 证据

`NoteListEngineTest`、`NoteColorsTest`、`EditorThemeTest` 行距、`NoteVaultTest` 列表带 color。`desktopTest` **196/196**，见 `docs/evidence/2026-09-15-note-toolbar/`。行距走 RSTA 私有 `lineHeight` 字段，升级 RSTA 需复测。IME 手工、窗口截图、三平台安装仍未测。

## 受影响范围

- JSON Vault 树无 color 字段，保持默认。
- 行距 1.0 不改 RSTA 默认行高，与 Electron 的 1.65em 基数不同，倍数关系一致。
