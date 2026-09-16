# DIFF-118：导入 toast 与随手记 Vault 树拖放

## JSON（对照 Electron `JsonTool` toast）

- 工具栏导入、编辑器拖放、Vault 树拖放成功后 `toastSuccess(json.notice.imported)`。

## F01 随手记

- Vault 树区域支持拖入 `.md` / `.markdown` / `.txt`，导入到当前笔记父目录（无选中则在根目录）；单文件自动打开。
- 复用 `jsonVaultImportRelativePath` / `jsonVaultEntryRelativePath`。

## 文件

- `JsonScreen.kt`、`QuickNoteScreen.kt`
