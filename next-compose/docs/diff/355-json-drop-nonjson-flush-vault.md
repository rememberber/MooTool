# DIFF-355：JSON 拖入非 JSON 前 flush + 随手记树拖放目录辅助

## 问题

[DIFF-341](341-json-vault-import-flush-dirty.md) / [DIFF-354](354-json-editor-drop-import-selection-dir.md) 仅在拖入 `.json` 入库前 `flushJsonVaultEditorIfDirty`。拖入其它文本会直接覆盖编辑器，若仍绑定未保存的 Vault 片段，磁盘内容与编辑器脱节且可能丢编辑。

随手记 Vault 树拖放仍手写 `VaultSelectionPath.parentDirectory`，与工具栏导入 [DIFF-350](350-quicknote-vault-import-selection-path.md) 的 `quickNoteVaultImportTargetDirectory` 重复。

## 行为

- JSON 编辑器拖入非 `.json`：覆盖正文前先 flush（失败则中止并提示）。
- 随手记树多文件拖放：目标目录统一 `quickNoteVaultImportTargetDirectory`（语义不变）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
