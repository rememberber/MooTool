# DIFF-156：读取源 SQLite `t_next_migration_row`

## 背景

DIFF-155 在 compose 库内登记 `import_migration_row`；Electron 在**目标库**已有 `t_next_migration_row` 记录自 legacy 迁入的行。用户以含该表的 SQLite（如 Electron 数据目录中的库）作为导入源时，不应再把已登记行重复写入 compose 历史/草稿。

## 行为

- `inspect`：若源库存在 `t_next_migration_row`，按 `source_table:source_id` 与 `ImportedLegacyHistory.dedupeKey` 对齐，过滤 `legacyHistory`；写入警告 `electron-migration-row:<skipped>`。
- `apply` 开头：将 `source_path` 与当前 `sourceRoot` 一致的 Electron 行种子写入 `import_migration_row`（`electron-migration-seed:<count>` 仅内部计数，不展示给用户时可忽略）。

## 证据

- `ElectronMigrationRowFilterTest.inspectSkipsLegacyRowsRecordedInElectronMigrationTable`

## 仍未覆盖

- `t_next_migration_run` 按 `source_path` 整包跳过见 [DIFF-157](157-electron-migration-run-skip.md)。
- safeStorage 密文迁移未做。
