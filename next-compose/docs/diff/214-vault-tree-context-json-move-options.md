# DIFF-214：Vault 移动目标过滤与随手记树右键前置保存

## 背景

- 随手记 Electron `openTreeAction`：目录操作前保存脏笔记并清空编辑区；文件操作前保存当前笔记，必要时加载目标文件。
- JSON Vault 移动下拉此前列出全部目录，未排除被移动目录及其子目录（随手记 DIFF-212 已修，JSON 未共用）。
- JSON/随手记 Vault 文本对话框字段标签与主按钮文案与 Electron 不一致。

## 行为

- 共享 `vaultMoveFolderOptions`（`features/vault/VaultMoveFolderOptions.kt`）；JSON 与随手记移动弹层共用。
- `prepareQuickNoteVaultContext`：重命名/移动/删除前对齐 Electron 保存与选中状态。
- 随手记 Action 对话框显示 `quickNote.dialog.name` 标签。
- JSON Vault 对话框：字段标签 `fileName`/`folderName`/`renameName`；新建/文件夹主按钮 `json.vault.create`；次按钮 `common.cancel`。

## 验证

- `VaultMoveFolderOptionsTest`、`QuickNoteMoveFolderOptionsTest`
- `./gradlew :composeApp:desktopTest --offline`
