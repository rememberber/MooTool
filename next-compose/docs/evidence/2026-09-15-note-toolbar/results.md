# 证据：随手记颜色、行距、列表前缀（2026-09-15）

- 覆盖：`NoteListEngine` 与 Electron 选区整行前缀一致；`NoteColors` 七色与树着色条件；`EditorBuffer.applyTheme(lineSpacing=2)` 行高至少约加倍；Vault `list()` 带回 frontmatter color
- `desktopTest` **196/196**（0 skipped）
- 未测：IME/列编辑手工窗口、真实窗口截图、托盘权限对话框、六套 CSS 逐选择器皮肤、三平台安装/升级/卸载
