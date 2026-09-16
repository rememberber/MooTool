# DIFF-155：`import_migration_row` 逐行迁移登记

## 背景

DIFF-154 用 `history.options.importDedupeKey` 与草稿文件避免重复；仍缺 Electron `t_next_migration_row` 等价的「来源路径 + 表 + 行 id」登记，以便 fingerprint 变化后仍能跳过已迁入行。

## 行为

- SQLite 表 `import_migration_row(source_path, source_table, source_id, migrated_at)`（schema v2）。
- `ImportPreview.sourceRoot` 为规范化绝对路径。
- `HistoryRepository.mergeImport` 在插入成功后 `record`；重复导入同来源同 `dedupeKey` 跳过。
- `CrossProductImporter.apply` 过滤 `legacyHistory` 并传入 `LegacyMigrationRowRepository`。
- 工具草稿应用后亦 `record` 对应 `dedupeKey`。

## 证据

- `HistoryMergeImportTest.recordsImportMigrationRowForSourcePath`

## 仍未覆盖

- 读取源 SQLite `t_next_migration_row` 见 [DIFF-156](156-electron-migration-row-read.md)；`t_next_migration_run` 整包对齐未做。
- safeStorage 密文迁移未做。
