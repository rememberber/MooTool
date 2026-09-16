# DIFF-423：Vault 复制片段后 `noteOwnWrite`

## 背景

[DIFF-422](422-vault-import-note-own-write.md) 为导入路径登记 `expectedHashes`。JSON/随手记 **复制** 会在磁盘新建文件并打开，此前未 `noteOwnWrite`，监视器可能把副本当作外部写入重复刷新。

## 行为

- `duplicateJsonVaultSnippet`：复制后对副本路径登记磁盘正文哈希。
- `duplicateQuickNoteEntry`：打开副本后对 `vault.read(relativePath)` 登记（与保存一致）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
