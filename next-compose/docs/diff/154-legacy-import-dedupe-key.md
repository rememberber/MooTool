# DIFF-154：迁移历史/工具草稿幂等键

## 背景

跨产品导入在整包 `fingerprint` 已导入时会跳过，但 SQLite 行级重复（改源目录后 fingerprint 变化、或仅重导历史）仍可能重复插入 `history` 或覆盖工具草稿。Electron legacy 使用 `t_next_migration_run` + 逐行记录；compose 需自有稳定键。

## 行为

- `LegacyImportOptions.withDedupeKey` 在 `history.options` 写入 `importDedupeKey`（保留 `migratedFrom` 等字段）。
- `HistoryRepository.mergeImport` 优先用 `json_extract(options, '$.importDedupeKey')` 跳过已导入行。
- 工具草稿：`data/imports/applied-tool-drafts.txt` 记录已应用的 `dedupeKey`，避免重复覆盖用户会话。

## 证据

- `HistoryMergeImportTest.skipsDuplicateLegacyRowsByImportDedupeKey`

## 仍未覆盖

- 逐行来源登记见 [DIFF-155](155-import-migration-row-table.md)；读取 Electron 目标库 `t_next_migration_run` 未做。
- safeStorage 密文迁移未做。
