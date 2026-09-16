# DIFF-157：`t_next_migration_run` 整包跳过

## 背景

Electron legacy 迁移在目标库写入 `t_next_migration_run(source_path, fingerprint, …)`，预览时若 `source_path` 已迁移则 `alreadyMigrated`。compose 仅有 `fingerprints.txt`；用户从**已跑过 Electron legacy 迁移的 SQLite**（或同路径 Java 数据目录）再导入时，应识别整包已迁移。

## 行为

- `inspect`：源库存在 `t_next_migration_run` 且任一 `source_path`（规范化绝对路径）等于当前 `sourceRoot` → `electronMigrationRunComplete = true`，警告 `electron-migration-run`。
- 设置迁移预览：`electronMigrationRunComplete` 时提示可跳过数据、仍合并设置；`alreadyMigrated` 仍仅指 compose `fingerprints.txt`。
- `apply`：`electronMigrationRunComplete` 时跳过 SQLite/Vault/收藏/历史等数据写入，确认后仍可 `updateSettings` 合并 Electron/Java 设置（对齐 Electron `alreadyMigrated` 仍返回 `settingsPatch`）。

## 证据

- `ElectronMigrationRowFilterTest.inspectMarksElectronMigrationRunForSameSourcePath`

## 仍未覆盖

- Electron `sourceFingerprint` 与 compose `fingerprint` 算法对齐比对未做（仅 `source_path`）。
- safeStorage 密文迁移未做。
