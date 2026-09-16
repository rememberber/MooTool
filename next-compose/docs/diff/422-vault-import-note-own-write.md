# DIFF-422：Vault 导入后 `noteOwnWrite` 抑制监视器误报

## 背景

[DIFF-416](416-vault-monitor-tree-snapshot-refresh.md) 在磁盘变更时刷新 Vault 树。本机拖放/工具栏导入会同步写入 Vault，但 `VaultRevisionMonitor` 若未登记 `expectedHashes`，下一轮轮询会把刚导入的文件当作「外部变更」，重复 `notify*VaultTreeChanged` 与 `handle*VaultChange`。单文件打开路径此前仅 JSON 编辑器拖入 `.json` 有 `noteOwnWrite`。

## 行为

- JSON Vault 树多文件拖放：每个成功 `importJsonVaultFile` 后 `noteOwnWrite`（磁盘正文哈希）。
- 随手记：树多文件拖放与工具栏 `runQuickNoteVaultImport` 在打开笔记后对 `vault.read(relative)` 登记（与保存路径一致）。

## 验证

- 复用 `VaultConflictEngineTest.snapshotDiffAndPollingSeeExternalWrites` 中 `noteOwnWrite` 过滤语义
- Vault 复制见 [DIFF-423](423-vault-duplicate-note-own-write.md)
- `./gradlew :composeApp:desktopTest --offline`
