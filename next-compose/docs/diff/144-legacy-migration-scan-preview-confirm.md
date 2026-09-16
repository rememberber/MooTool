# DIFF-144：数据迁移扫描 / 预览 / 确认导入

对照 Electron `LegacyMigrationSettings`（`previewLegacyMigration` → 确认 → `runLegacyMigration`）。

## 范围

- 设置「数据迁移」：默认来源 `~/.MooTool`、来源路径 + 选目录、**扫描**预览（条数、数据库/配置是否找到、警告）。
- 已导入指纹显示「已导入」；有数据时 **确认弹层** 后再执行备份 + `CrossProductImporter.apply`。
- `ImportPreview` 增加 `databaseFound` / `configFound`；`totalItems()` 辅助。
- `AppContainer.defaultLegacyImportSource()`。

## 与 Electron 仍不同

- Compose 仍只迁移笔记/JSON/自定义分组（与既有 `CrossProductImporter` 一致），不含 Electron 全量 `LegacyMigrationCounts`（HTTP 历史、Host 等）。

## 文件

- `MigrationSettingsPanel.kt`、`SettingsScreen.kt`、`CrossProductImporter.kt`、`AppContainer.kt`、`Translator.kt`
- `VaultGitCheckpointSchedulerTest.kt`（`CrossProductImporterTest`）

## 未测

- 迁移确认弹层与扫描按钮的产品窗手工走查。
