# DIFF-122：Vault CRUD / Host / HTTP / 格式化 toast

对照 Electron `JsonVaultPanel.tsx`、`HostTool.tsx`、`ReformatTool.tsx` 与全局 Toast。

## JSON Vault（F04）

- 新建片段、删除、拖放移动、对话框新建文件夹/重命名/移动 → `json.vault.created|deleted|folderCreated|renamed|moved` toast。
- 新增 i18n：`json.vault.created`、`deleted`、`folderCreated`、`renamed`（中/英/日）。

## 其它工具

- **F03 格式化**：格式化成功 → `reformat.formatted` toast。
- **F10 Host**：保存、重命名保存、导出、应用 hosts → `common.save` / `host.exported` / `host.applied*` toast。
- **F09 HTTP**：另存响应成功/失败 → toast。
- **F01 随手记**：树拖放移动、复制 → `quickNote.move` / `quickNote.duplicated` toast。

## 文件

- `Translator.kt`、`JsonScreen.kt`、`ReformatScreen.kt`、`HostScreen.kt`、`HttpScreen.kt`、`QuickNoteScreen.kt`
