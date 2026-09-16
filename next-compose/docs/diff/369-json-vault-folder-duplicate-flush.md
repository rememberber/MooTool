# DIFF-369：JSON Vault 新建文件夹 flush + 复制片段 flush 失败 notice

## 背景

- 随手记新建文件夹在 [DIFF-347](347-quicknote-create-folder-save-guard.md) 于落盘前 `saveIfNeeded`，避免保存失败后仍改 Vault 状态。
- JSON `json-file` / 移动 / 重命名等已在对话框提交时 `jsonVaultRequireFlushDirty`；**新建文件夹**分支此前未 flush，脏片段仍打开时可能先建目录再遇后续操作丢改。
- `duplicateJsonVaultSnippet` 直接调用 `flushJsonVaultEditorIfDirty`，冲突时未写入 `session.notice`，与 DIFF-368 其它 Vault 路径不一致。

## 行为

- 对话框 `json-folder` 提交：在 `createDirectory` 前 `jsonVaultRequireFlushDirty`（干净文档跳过；失败 `notice` + 中止）。
- `duplicateJsonVaultSnippet`：改用 `jsonVaultFlushDirtyOrNotice`，失败返回带 `notice` 的 `Result.failure`。

## 测试

- `JsonVaultFlushNoticeTest.duplicateSnippetFailsWithNoticeOnConflict`
- `JsonVaultCreateFolderFlushTest.folderCreateAbortsWhenFlushConflict`

## 验收

- F04 Vault CRUD；`desktopTest --offline`。
