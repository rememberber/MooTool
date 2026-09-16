# DIFF-385：随手记清空编辑器 metadata 使用 `Untitled`

## 背景

`NoteMetadata.defaults("")` 经 `normalizeTitle` 会抛 `Invalid title`。目录 `prepareQuickNoteVaultContext`、新建文件夹对话框、删除打开笔记等路径曾使用空标题，可能在选中目录清空编辑器时崩溃。

## 行为

- 上述路径统一 `NoteMetadata.defaults("Untitled")`（与 [DIFF-383](383-quicknote-vault-refresh-reload-open.md) `clearQuickNoteOpenSession` 一致）。

## 测试

- `QuickNoteVaultPrepareContextTest.prepareContextDirectorySelectClearsEditorWithUntitledMetadata`

## 验收

- F01；`desktopTest --offline`。
