# DIFF-426：复制 / 单文件导入 / 冲突副本 `rebaseline`

## 背景

[DIFF-425](425-vault-crud-rebaseline.md) 覆盖删除、移动、对话框与树批量拖放。**复制片段**、**工具栏单文件导入**、**冲突「另存副本」** 仍会新增路径，仅用 `noteOwnWrite` 不足。

## 行为

- `duplicateJsonVaultSnippet` / `duplicateQuickNoteEntry`：成功后 `rebaselineAfterLocalCrud()`。
- JSON 编辑器单文件 `.json` 拖入入库、随手记 `runQuickNoteVaultImport`：成功后 rebaseline。
- Vault 外部冲突对话框「另存副本」：写盘后 rebaseline（JSON/随手记）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（533/533）
