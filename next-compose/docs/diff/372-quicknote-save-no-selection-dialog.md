# DIFF-372：随手记无打开笔记时保存打开新建对话框

## 背景

Electron `saveCurrent` 在 `!state.note` 时直接返回，不写入磁盘。

Compose `quickNoteSaveCurrent` 在 `currentFile` 为空时曾生成 `note-<timestamp>.md` 静默落盘，与 [DIFF-371](371-json-vault-save-no-selection-dialog.md) 修复的 JSON `draft.json` 问题同类。

## 行为

- `openQuickNoteNewNoteDialog`：与「新建笔记」按钮共用 `dialogMode = note`。
- `quickNoteSaveFromUserAction`：无 `currentFile` 时打开新建对话框；否则 `quickNoteSaveCurrent`。
- 工具栏「保存」、编辑器 `Cmd/Ctrl+S` 改用上述逻辑。
- `quickNoteSaveCurrent`：`currentFile` 为空时失败（`quickNote.saveNoSelection`）；idle/Git 等仍仅在已打开笔记时写盘。

## 测试

- `QuickNoteSaveSelectionTest`

## 验收

- F01 Vault 保存；`desktopTest --offline`。
